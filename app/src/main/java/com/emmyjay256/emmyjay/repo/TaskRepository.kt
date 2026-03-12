package com.emmyjay256.emmyjay.repo

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.emmyjay256.emmyjay.data.*
import com.emmyjay256.emmyjay.recievers.ReminderReceiver
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.Calendar

class TaskRepository(
    private val taskDao: TaskDao,
    private val dayRecordDao: DayRecordDao,
    private val dayTaskRecordDao: DayTaskRecordDao
) {
    fun observeTasksForDay(day: Int): Flow<List<TaskEntity>> = taskDao.observeTasksForDay(day)
    suspend fun upsert(task: TaskEntity): Long = taskDao.upsert(task)
    suspend fun delete(task: TaskEntity) = taskDao.delete(task)

    suspend fun markCompletedToday(task: TaskEntity, dateIso: String = LocalDate.now().toString()) {
        taskDao.update(task.copy(lastCompletedDate = dateIso, completedAtEpochMs = System.currentTimeMillis()))
    }

    suspend fun markIncomplete(task: TaskEntity) {
        taskDao.update(task.copy(lastCompletedDate = null, completedAtEpochMs = null))
    }

    // Task alarms using unique requestCodes to prevent overlap
    fun scheduleTaskAlarms(context: Context, task: TaskEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (task.dayOfWeek != LocalDate.now().dayOfWeek.value) return

        val triggers = listOf(
            Pair(task.startTime.minusMinutes(10), "PRIME"),
            Pair(task.startTime, "START"),
            Pair(task.startTime.plusMinutes(10), "NUDGE")
        )

        triggers.forEachIndexed { index, trigger ->
            val time = trigger.first
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, time.hour)
                set(Calendar.MINUTE, time.minute)
                set(Calendar.SECOND, 0)
            }

            if (calendar.after(Calendar.getInstance())) {
                val intent = Intent(context, ReminderReceiver::class.java).apply {
                    putExtra("TASK_TITLE", task.title)
                    putExtra("REMINDER_STAGE", trigger.second)
                }

                // Mirroring MainActivity's PendingIntent structure
                val requestCode = (task.id.toInt() * 10) + index
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        }
    }

    // MIRRORED FROM MainActivity: Using the exact same pattern
    fun scheduleDailyFinalizeAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val times = listOf(
            Pair(22, 20) to "FINALIZE_PRIME", // Set to 11:20 PM
            Pair(22, 30) to "FINALIZE_LOCK",
            Pair(22, 40) to "FINALIZE_LAST_CALL",
            Pair(22, 50) to "FINALIZE_LAST_CALL"
        )

        times.forEachIndexed { index, pair ->
            val (time, stage) = pair
            val (hour, min) = time

            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("TASK_TITLE", "Day Finalization")
                putExtra("REMINDER_STAGE", stage)
            }

            // Using distinct requestCodes starting at 100 to avoid requestCode 0 conflict
            val requestCode = 100 + index
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, min)
                set(Calendar.SECOND, 0)

                // EXACT MATH FROM MAINACTIVITY
                if (before(Calendar.getInstance())) {
                    add(Calendar.DATE, 1)
                }
            }

            // EXACT CALL FROM MAINACTIVITY
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    suspend fun resyncAllAlarms(context: Context) {
        val today = LocalDate.now().dayOfWeek.value
        val tasks = taskDao.getTasksForDay(today)
        tasks.forEach { scheduleTaskAlarms(context, it) }
        scheduleDailyFinalizeAlarm(context)
    }

    suspend fun finalizeMissingDaysIfNeeded(appContext: Context) {
        val prefs = appContext.getSharedPreferences("emmyjay_prefs", Context.MODE_PRIVATE)
        val today = LocalDate.now()
        val last = prefs.getString("last_finalized_iso", null) ?: today.toString()
        val lastDate = runCatching { LocalDate.parse(last) }.getOrNull() ?: today
        if (!lastDate.isBefore(today)) return
        var d = lastDate
        while (d.isBefore(today)) {
            finalizeDay(d.toString(), d.dayOfWeek.value)
            d = d.plusDays(1)
        }
        prefs.edit().putString("last_finalized_iso", today.toString()).apply()
    }

    private suspend fun finalizeDay(dateIso: String, dayOfWeek: Int) {
        if (dayRecordDao.getByDate(dateIso) != null) return
        val tasks = taskDao.getTasksForDay(dayOfWeek)
        val totalMinutes = tasks.sumOf { it.durationMinutes() }.coerceAtLeast(0L)
        val completed = tasks.filter { it.lastCompletedDate == dateIso }
        val completedMinutes = completed.sumOf { it.durationMinutes() }.coerceAtLeast(0L)
        val missedMinutes = (totalMinutes - completedMinutes).coerceAtLeast(0L)
        val percent = if (totalMinutes <= 0L) 0f else (completedMinutes.toFloat() / totalMinutes.toFloat())
        dayRecordDao.insert(DayRecordEntity(dateIso = dateIso, dayOfWeek = dayOfWeek, totalMinutesScheduled = totalMinutes, completedMinutes = completedMinutes, missedMinutes = missedMinutes, completionPercent = percent, createdAtEpochMs = System.currentTimeMillis()))
    }
}