package com.example.speak2read.model

enum class MessageType {
    RECEIVE,
    SEND
}

data class ChatMessage(
    val text: String,
    val type: MessageType
)

