package com.example.speak2read.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: String,
    val text: String,
    val type: String,
    val timestamp: String,
    val isFavorite: Boolean = false,
    val category: String = "GENERAL",
    val contactName: String? = null,
    val isPinned: Boolean = false // Nueva columna para fijar chats
)
