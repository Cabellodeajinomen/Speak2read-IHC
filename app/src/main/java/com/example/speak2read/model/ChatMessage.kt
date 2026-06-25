package com.example.speak2read.model

enum class MessageType {
    RECEIVE,
    SEND
}

data class ChatMessage(
    val id: Int = 0,
    val text: String,
    val type: MessageType,
    val timestamp: String,
    var isFavorite: Boolean = false
)