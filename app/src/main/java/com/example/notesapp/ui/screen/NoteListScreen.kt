package com.example.notesapp.ui.screen

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.notesapp.data.Note
import com.example.notesapp.data.NoteDatabase
import kotlinx.coroutines.launch
import androidx.compose.foundation.combinedClickable

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(navController: NavController) {
    val context = LocalContext.current
    val db = NoteDatabase.getDatabase(context)
    val noteDao = db.noteDao()
    val coroutineScope = rememberCoroutineScope()

    var notes by remember { mutableStateOf(listOf<Note>()) }
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var isSearchActive by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<Note?>(null) }
    var showFolderDeleteConfirm by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        noteDao.getAllNotes().collect { notes = it }
    }

    val filtered = notes.filter {
        if (searchQuery.text.isBlank()) true
        else {
            it.title.contains(searchQuery.text, true) ||
                    it.content.contains(searchQuery.text, true) ||
                    it.folder.contains(searchQuery.text, true)
        }
    }

    val pinnedNotes = filtered.filter { it.pinned }
    val unpinnedNotes = filtered.filter { !it.pinned }
    val grouped = filtered
        .groupBy { it.folder.ifBlank { "📁 Uncategorized" } }
        .mapValues { entry ->
            entry.value.sortedWith(
                compareByDescending<Note> { it.pinned }.thenBy { it.title }
            )
        }
        .toSortedMap()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search notes...") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { /* dismiss keyboard */ })
                        )
                    } else {
                        Text("Your Notes")
                    }
                },
                actions = {
                    IconButton(onClick = { isSearchActive = !isSearchActive }) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = null
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                navController.navigate("noteEditor?noteId=-1&folder=")
            }) {
                Icon(Icons.Default.Add, contentDescription = "New Note")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (pinnedNotes.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pinnedNotes.forEach { note ->
                        FilterChip(
                            selected = true,
                            onClick = {
                                navController.navigate("noteEditor?noteId=${note.id}&folder=${note.folder}")
                            },
                            label = { Text(note.title) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                grouped.forEach { (folder, notesInFolder) ->
                    item {
                        Text(
                            text = folder,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(notesInFolder) { note ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .combinedClickable(
                                    onClick = {
                                        navController.navigate("noteEditor?noteId=${note.id}&folder=${note.folder}")
                                    },
                                    onLongClick = {
                                        coroutineScope.launch {
                                            noteDao.insertNote(note.copy(pinned = !note.pinned))
                                        }
                                    }
                                ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(
                                containerColor = runCatching {
                                    Color(android.graphics.Color.parseColor(note.colorHex))
                                }.getOrElse { Color.White }
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = note.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = {
                                        coroutineScope.launch {
                                            noteDao.insertNote(note.copy(pinned = !note.pinned))
                                        }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.PushPin,
                                            contentDescription = null,
                                            tint = if (note.pinned) MaterialTheme.colorScheme.primary else Color.Gray
                                        )
                                    }
                                    IconButton(onClick = { showDeleteConfirm = note }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Note")
                                    }
                                }
                                if (note.pinned) {
                                    Text(
                                        text = "📌 Pinned",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }

            showDeleteConfirm?.let { note ->
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = null },
                    title = { Text("Delete Note") },
                    text = { Text("Delete '${note.title}'?") },
                    confirmButton = {
                        TextButton(onClick = {
                            coroutineScope.launch {
                                noteDao.deleteNote(note)
                                showDeleteConfirm = null
                            }
                        }) { Text("Yes") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirm = null }) { Text("Cancel") }
                    }
                )
            }

            showFolderDeleteConfirm?.let { folder ->
                AlertDialog(
                    onDismissRequest = { showFolderDeleteConfirm = null },
                    title = { Text("Delete Folder") },
                    text = { Text("Delete all notes in '$folder'?") },
                    confirmButton = {
                        TextButton(onClick = {
                            coroutineScope.launch {
                                notes.filter { it.folder == folder }
                                    .forEach { noteDao.deleteNote(it) }
                                showFolderDeleteConfirm = null
                            }
                        }) { Text("Yes") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showFolderDeleteConfirm = null }) { Text("Cancel") }
                    }
                )
            }
        }
    }
}
