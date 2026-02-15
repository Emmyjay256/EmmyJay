package com.emmyjay256.emmyjay.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalTime

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    // 1..7 (Mon=1 ... Sun=7) matching java.time.DayOfWeek.value
    val dayOfWeek: Int,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val category: TaskCategory,
    val isCompleted: Boolean = false,
    val completedAtEpochMs: Long? = null
) {
    fun durationMinutes(): Long {
        val start = startTime.toSecondOfDay()
        val end = endTime.toSecondOfDay()
        val diff = end - start
        return (diff.coerceAtLeast(0) / 60).toLong()
    }
}