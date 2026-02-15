package com.emmyjay256.emmyjay.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalTime

@Entity(
    tableName = "day_task_records",
    indices = [
        Index(value = ["dateIso"]),
        Index(value = ["taskId"]),
        Index(value = ["dateIso", "taskId"])
    ]
)
data class DayTaskRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,

    val dateIso: String,          // "YYYY-MM-DD"
    val taskId: Long,             // template task id

    // Snapshot fields (so history survives even if template changes/deletes)
    val title: String,
    val category: TaskCategory,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val durationMinutes: Long,

    val status: DayTaskStatus,
    val completedAtEpochMs: Long? = null
)