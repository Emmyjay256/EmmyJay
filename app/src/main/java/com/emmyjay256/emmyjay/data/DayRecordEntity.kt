package com.emmyjay256.emmyjay.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "day_records")
data class DayRecordEntity(
    @PrimaryKey val dateIso: String,      // "YYYY-MM-DD"
    val dayOfWeek: Int,                   // 1..7
    val totalMinutesScheduled: Long,
    val completedMinutes: Long,
    val missedMinutes: Long,
    val completionPercent: Float,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)