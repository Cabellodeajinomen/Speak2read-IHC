package com.example.speak2read.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ChatMessageEntity::class],
    version = 7
)
abstract class Speak2ReadDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
}