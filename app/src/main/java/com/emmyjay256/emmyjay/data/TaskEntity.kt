package com.emmyjay256.emmyjay.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalTime

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    // 1..7 (Mon=1 ... Sun=7)
    val dayOfWeek: Int,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val category: TaskCategory,

    // Completion is PER DATE, so the task repeats weekly.
    // ISO date string like "2026-02-15"
    val lastCompletedDate: String? = null,

    val completedAtEpochMs: Long? = null
) {
    fun durationMinutes(): Long {
        val start = startTime.toSecondOfDay()
        val end = endTime.toSecondOfDay()
        val diff = end - start
        return (diff.coerceAtLeast(0) / 60).toLong()
    }
}