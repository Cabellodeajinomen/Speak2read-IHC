package com.example.speak2read.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MessageDao {
    @Insert
    fun insert(message: ChatMessageEntity)

    @Query("SELECT * FROM messages")
    fun getAll(): List<ChatMessageEntity>

    @Query("DELETE FROM messages")
    fun clear()
}