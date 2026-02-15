package com.emmyjay256.emmyjay.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        TaskEntity::class,
        DayRecordEntity::class,
        DayTaskRecordEntity::class
    ],
    version = 3, // ✅ bump (you'll reinstall anyway)
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class EmmyJayDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun dayRecordDao(): DayRecordDao
    abstract fun dayTaskRecordDao(): DayTaskRecordDao
}