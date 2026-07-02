package com.example.speak2read.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: String, // ID único del usuario de Firebase
    val text: String,
    val type: String,
    val timestamp: String,
    val isFavorite: Boolean = false
)
