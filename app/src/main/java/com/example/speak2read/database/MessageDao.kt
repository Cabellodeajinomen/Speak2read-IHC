package com.example.speak2read.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MessageDao {
    @Insert
    fun insert(message: ChatMessageEntity): Long

    @Query("SELECT * FROM messages WHERE userId = :userId")
    fun getAll(userId: String): List<ChatMessageEntity>

    @Query("SELECT * FROM messages WHERE userId = :userId AND isFavorite = 1")
    fun getFavorites(userId: String): List<ChatMessageEntity>

    @Query("UPDATE messages SET isFavorite = :isFavorite WHERE id = :messageId")
    fun updateFavorite(messageId: Int, isFavorite: Boolean)

    @Query("DELETE FROM messages WHERE userId = :userId")
    fun clear(userId: String)
}
