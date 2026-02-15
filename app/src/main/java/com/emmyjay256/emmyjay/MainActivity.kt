package com.emmyjay256.emmyjay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.room.Room
import com.emmyjay256.emmyjay.data.EmmyJayDatabase
import com.emmyjay256.emmyjay.repo.TaskRepository
import com.emmyjay256.emmyjay.ui.EmmyJayNav
import com.emmyjay256.emmyjay.ui.theme.EmmyJayTheme

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

        val repo = TaskRepository(db.taskDao())

        setContent {
            EmmyJayTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    EmmyJayNav(repo = repo)
                }
            }
        }
    }
}