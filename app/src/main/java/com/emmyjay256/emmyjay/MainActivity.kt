package com.emmyjay256.emmyjay

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.emmyjay256.emmyjay.data.EmmyJayDatabase
import com.emmyjay256.emmyjay.recievers.ReminderReceiver
import com.emmyjay256.emmyjay.repo.TaskRepository
import com.emmyjay256.emmyjay.ui.EmmyJayNav
import com.emmyjay256.emmyjay.ui.theme.EmmyJayTheme
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = Room.databaseBuilder(
            applicationContext,
            EmmyJayDatabase::class.java,
            "emmyjay.db"
        ).fallbackToDestructiveMigration()
            .build()

        val repo = TaskRepository(
            taskDao = db.taskDao(),
            dayRecordDao = db.dayRecordDao(),
            dayTaskRecordDao = db.dayTaskRecordDao()
        )

        // ✅ Auto-finalize missing days (yesterday / older) on app launch
        lifecycleScope.launch {
            repo.finalizeMissingDaysIfNeeded(applicationContext)
        }

        lifecycleScope.launch {
            // 1. Handle the database cleanup
            repo.finalizeMissingDaysIfNeeded(applicationContext)

            // 2. TRIGGER THE REPO ALARMS: This makes the Repo "live"
            repo.resyncAllAlarms(applicationContext)
        }

        setContent {
            EmmyJayTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    EmmyJayNav(repo = repo)
                }
            }
        }

        // Overlay Permission Check
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }

        // Schedule the 7:50 AM Assistant Wake-up
        schedule750Alarm()
    }

    private fun schedule750Alarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 1. Check if we are allowed to schedule exact alarms (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // If not, send the user to the settings page to toggle it on
                val intent = Intent().apply {
                    action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
                return // Stop here until permission is granted
            }
        }

        val intent = Intent(this, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 3)
            set(Calendar.SECOND, 0)

            if (before(Calendar.getInstance())) {
                add(Calendar.DATE, 1)
            }
        }

        // Now it's safe to call this without a crash
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }
}