package com.emmyjay256.emmyjay.repo

import android.content.Context
import com.emmyjay256.emmyjay.data.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class TaskRepository(
    private val taskDao: TaskDao,
    private val dayRecordDao: DayRecordDao,
    private val dayTaskRecordDao: DayTaskRecordDao
) {
    fun observeTasksForDay(day: Int): Flow<List<TaskEntity>> = taskDao.observeTasksForDay(day)

    suspend fun upsert(task: TaskEntity): Long = taskDao.upsert(task)
    suspend fun delete(task: TaskEntity) = taskDao.delete(task)

    suspend fun markCompletedToday(task: TaskEntity, dateIso: String = LocalDate.now().toString()) {
        taskDao.update(
            task.copy(
                lastCompletedDate = dateIso,
                completedAtEpochMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun markIncomplete(task: TaskEntity) {
        taskDao.update(
            task.copy(
                lastCompletedDate = null,
                completedAtEpochMs = null
            )
        )
    }

    /**
     * Call this on app launch:
     * - If you missed days, it logs each missing day (completed vs missed).
     * - Uses SharedPreferences to remember last finalized date.
     */
    suspend fun finalizeMissingDaysIfNeeded(appContext: Context) {
        val prefs = appContext.getSharedPreferences("emmyjay_prefs", Context.MODE_PRIVATE)

        val today = LocalDate.now()
        val todayIso = today.toString()

        val last = prefs.getString("last_finalized_iso", null)

        // First ever run: set checkpoint and do nothing
        if (last == null) {
            prefs.edit().putString("last_finalized_iso", todayIso).apply()
            return
        }

        val lastDate = runCatching { LocalDate.parse(last) }.getOrNull()
            ?: run {
                prefs.edit().putString("last_finalized_iso", todayIso).apply()
                return
            }

        // If already up to date, nothing to do
        if (!lastDate.isBefore(today)) return

        // Log each missing date from (lastDate) up to (today - 1)
        var d = lastDate
        while (d.isBefore(today)) {
            val dateIso = d.toString()
            // Don’t log today-in-progress. Only log days strictly before today.
            if (d.isBefore(today)) {
                finalizeDay(dateIso, d.dayOfWeek.value)
            }
            d = d.plusDays(1)
        }

        // Move checkpoint to today
        prefs.edit().putString("last_finalized_iso", todayIso).apply()
    }

    /**
     * Writes the day record + per-task records for a specific date.
     * If already logged, it does nothing.
     */
    private suspend fun finalizeDay(dateIso: String, dayOfWeek: Int) {
        // Already logged? stop.
        if (dayRecordDao.getByDate(dateIso) != null) return

        // If details already exist (edge case), stop.
        if (dayTaskRecordDao.countForDate(dateIso) > 0) return

        val tasks = taskDao.getTasksForDay(dayOfWeek)

        val totalMinutes = tasks.sumOf { it.durationMinutes() }.coerceAtLeast(0L)

        val completed = tasks.filter { it.lastCompletedDate == dateIso }
        val missed = tasks.filter { it.lastCompletedDate != dateIso }

        val completedMinutes = completed.sumOf { it.durationMinutes() }.coerceAtLeast(0L)
        val missedMinutes = missed.sumOf { it.durationMinutes() }.coerceAtLeast(0L)

        val percent =
            if (totalMinutes <= 0L) 0f
            else (completedMinutes.toFloat() / totalMinutes.toFloat()).coerceIn(0f, 1f)

        // Insert summary row
        dayRecordDao.insert(
            DayRecordEntity(
                dateIso = dateIso,
                dayOfWeek = dayOfWeek,
                totalMinutesScheduled = totalMinutes,
                completedMinutes = completedMinutes,
                missedMinutes = missedMinutes,
                completionPercent = percent,
                createdAtEpochMs = System.currentTimeMillis()
            )
        )

        // Insert per-task snapshot rows
        val detailRows = tasks.map { t ->
            val isDone = (t.lastCompletedDate == dateIso)
            DayTaskRecordEntity(
                dateIso = dateIso,
                taskId = t.id,
                title = t.title,
                category = t.category,
                startTime = t.startTime,
                endTime = t.endTime,
                durationMinutes = t.durationMinutes(),
                status = if (isDone) DayTaskStatus.COMPLETED else DayTaskStatus.MISSED,
                completedAtEpochMs = if (isDone) t.completedAtEpochMs else null
            )
        }

        dayTaskRecordDao.insertAll(detailRows)
    }
}