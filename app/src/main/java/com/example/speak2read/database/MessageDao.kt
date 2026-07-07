package com.example.speak2read.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MessageDao {
    @Insert
    fun insert(message: ChatMessageEntity): Long

    @Query("SELECT * FROM messages WHERE userId = :userId ORDER BY id ASC")
    fun getAll(userId: String): List<ChatMessageEntity>

    // Obtener los nombres únicos de contactos para mostrar la lista de chats
    @Query("SELECT DISTINCT contactName FROM messages WHERE userId = :userId AND contactName IS NOT NULL")
    fun getUniqueContacts(userId: String): List<String>

    // Obtener mensajes de un contacto específico
    @Query("SELECT * FROM messages WHERE userId = :userId AND contactName = :contactName ORDER BY id ASC")
    fun getMessagesByContact(userId: String, contactName: String): List<ChatMessageEntity>

    @Query("SELECT * FROM messages WHERE userId = :userId AND isFavorite = 1")
    fun getFavorites(userId: String): List<ChatMessageEntity>

    @Query("UPDATE messages SET isFavorite = :isFavorite WHERE id = :messageId")
    fun updateFavorite(messageId: Int, isFavorite: Boolean)

    @Query("UPDATE messages SET isPinned = :pinned WHERE contactName = :contactName AND userId = :userId")
    fun updatePinnedStatus(userId: String, contactName: String, pinned: Boolean)

    @Query("DELETE FROM messages WHERE userId = :userId")
    fun clear(userId: String)
}
