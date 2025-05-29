package com.example.notesapp.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0 ,
    val title: String,
    val content: String,
    val folder: String,
    val colorHex: String,
    val isHighlighted: Boolean = false,
    @ColumnInfo(name = "pinned") val pinned: Boolean = false
)

