package com.example.speak2read.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ChatMessageEntity::class],
    version = 5
)
abstract class Speak2ReadDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
}