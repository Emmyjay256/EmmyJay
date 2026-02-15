package com.emmyjay256.emmyjay.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [TaskEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class EmmyJayDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
}