package com.emmyjay256.emmyjay.recievers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.emmyjay256.emmyjay.BuildConfig
import com.emmyjay256.emmyjay.services.OverlayService
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("TASK_TITLE") ?: "Engineering Task"
        val stage = intent.getStringExtra("REMINDER_STAGE") ?: "START"
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            val aiMessage = fetchAiMessage(title, stage)

            val serviceIntent = Intent(context, OverlayService::class.java).apply {
                putExtra("EXTRA_TITLE", title)
                putExtra("EXTRA_STAGE", stage)
                putExtra("EXTRA_AI_TEXT", aiMessage)
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            pendingResult.finish()
        }
    }

    private suspend fun fetchAiMessage(title: String, stage: String): String {
        val model = GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )

        // Updated prompt: Aggressive, direct, and zero-bullshit.
        val prompt = """
            You are a hardcore engineering mentor. The user is a high-performing Electrical Engineer & IMO winner.
            Task: "$title"
            Stage: "$stage" (PRIME: 10m before, START: now, NUDGE: late, FINALIZE: end of day)
            Action: Generate a sharp, high-pressure directive. 
            Style: Use engineering metaphors (circuits, latency, precision). Be very tough. 
            Instruction: Remind them that failure to execute "$title" is a logic error in their long-game strategy. 
            Constraint: Long-form, direct, absolute. No disclaimers. No "good luck".
        """.trimIndent()

        return try {
            withTimeoutOrNull(5000) { // Increased to 5s for longer messages
                model.generateContent(prompt).text
            } ?: "Latency is fatal. Execute $title immediately."
        } catch (e: Exception) {
            "System integrity check failed. Start $title now or fall behind the curve."
        }
    }
}