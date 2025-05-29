package com.example.notesapp.ui.screen

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.navigation.NavController
import com.example.notesapp.data.Note
import com.example.notesapp.data.NoteDatabase
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteScreen(navController: NavController, noteId: Int, presetFolder: String) {
    val context = LocalContext.current
    val db = NoteDatabase.getDatabase(context)
    val noteDao = db.noteDao()
    val coroutineScope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var folder by remember { mutableStateOf(presetFolder) }
    var colorHex by remember { mutableStateOf("#FFFFFF") }
    var pinned by remember { mutableStateOf(false) }
    var isHighlighted by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!loaded && noteId != -1) {
            noteDao.getNoteById(noteId)?.also { note ->
                title = note.title
                content = note.content
                folder = note.folder
                colorHex = note.colorHex
                pinned = note.pinned
                isHighlighted = note.isHighlighted
            }
            loaded = true
        }
    }

    val colorOptions = listOf(
        "#FFFFFF", "#FFCDD2", "#FFF9C4", "#C8E6C9", "#BBDEFB", "#D1C4E9"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(folder.ifBlank { "New Note" }) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: undo logic */ }) {
                        Icon(Icons.Filled.Undo, contentDescription = "Undo")
                    }
                    IconButton(onClick = { /* TODO: redo logic */ }) {
                        Icon(Icons.Filled.Redo, contentDescription = "Redo")
                    }
                    IconButton(onClick = { isHighlighted = !isHighlighted }) {
                        Icon(
                            Icons.Filled.Highlight,
                            contentDescription = "Toggle Highlight",
                            tint = if (isHighlighted) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                coroutineScope.launch {
                    if (title.isNotBlank() || content.isNotBlank()) {
                        val note = if (noteId != -1) {
                            Note(
                                id = noteId,
                                title = title,
                                content = content,
                                folder = folder,
                                colorHex = colorHex,
                                pinned = pinned,
                                isHighlighted = isHighlighted
                            )
                        } else {
                            Note(
                                id = 0,
                                title = title,
                                content = content,
                                folder = folder,
                                colorHex = colorHex,
                                pinned = false,
                                isHighlighted = isHighlighted
                            )
                        }
                        noteDao.insertNote(note)
                        Toast.makeText(context, "Note saved", Toast.LENGTH_SHORT).show()
                        navController.navigate("notes") {
                            popUpTo("notes") { inclusive = true }
                        }
                    }
                }
            }) {
                Icon(
                    imageVector = if (noteId != -1) Icons.Filled.Delete else Icons.Filled.Check,
                    contentDescription = if (noteId != -1) "Delete Note" else "Save Note"
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Select Color:", style = MaterialTheme.typography.labelLarge)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .fillMaxWidth()
            ) {
                colorOptions.forEach { hex ->
                    val isSelected = hex == colorHex
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(hex.toColorInt()), CircleShape)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                shape = CircleShape
                            )
                            .clickable { colorHex = hex }
                    )
                }
            }

            OutlinedTextField(
                value = folder,
                onValueChange = { folder = it },
                label = { Text("Folder (e.g. 📁 Work)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Content") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .height(150.dp)
            )
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete Note") },
                text = { Text("Are you sure you want to delete this note?") },
                confirmButton = {
                    TextButton(onClick = {
                        coroutineScope.launch {
                            noteDao.deleteNote(
                                Note(
                                    id = noteId,
                                    title = title,
                                    content = content,
                                    folder = folder,
                                    colorHex = colorHex,
                                    pinned = pinned,
                                    isHighlighted = isHighlighted
                                )
                            )
                            Toast.makeText(context, "Note deleted", Toast.LENGTH_SHORT).show()
                            navController.navigate("notes") {
                                popUpTo("notes") { inclusive = true }
                            }
                        }
                    }) {
                        Text("Yes")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
