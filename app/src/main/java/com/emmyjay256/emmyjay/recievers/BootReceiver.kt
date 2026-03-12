package com.emmyjay256.emmyjay.recievers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.emmyjay256.emmyjay.repo.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    // If you aren't using Dagger/Hilt, you'll need to get your repo instance here
    // For a manual setup, you might use your Database class or an AppContainer

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync() // Tells Android to keep the receiver alive a bit longer

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Get your repository (Example: from your Application class)
                    // val repo = (context.applicationContext as MyApplication).repository
                    // repo.resyncAllAlarms(context)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}