package com.emmyjay256.emmyjay.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.emmyjay256.emmyjay.MainActivity
import com.emmyjay256.emmyjay.R
import com.emmyjay256.emmyjay.ui.BigReminderCard
import com.emmyjay256.emmyjay.ui.theme.EmmyJayTheme

class OverlayService : LifecycleService(), SavedStateRegistryOwner, ViewModelStoreOwner {
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null

    private val currentTitle = mutableStateOf("Task")
    private val currentAiMessage = mutableStateOf("Processing engineering logic...")

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private lateinit var _viewModelStore: ViewModelStore
    override val viewModelStore: ViewModelStore
        get() = _viewModelStore

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        _viewModelStore = ViewModelStore()

        val channelId = "emmyjay_assistant"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Assistant Active",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("EmmyJay Assistant")
            .setContentText("Monitoring your long-game goals...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val title = intent?.getStringExtra("EXTRA_TITLE") ?: "Engineering Task"
        val aiText = intent?.getStringExtra("EXTRA_AI_TEXT") ?: "System online. Proceed."

        currentTitle.value = title
        currentAiMessage.value = aiText

        showBigCard()
        return START_STICKY
    }

    private fun showBigCard() {
        if (overlayView != null) return

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
            y = 150
        }

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
            setViewTreeViewModelStoreOwner(this@OverlayService)

            setContent {
                EmmyJayTheme {
                    BigReminderCard(
                        title = currentTitle.value,
                        aiMessage = currentAiMessage.value,
                        onDismiss = {
                            // 1. Logic to launch the app
                            val launchIntent = Intent(this@OverlayService, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            }
                            startActivity(launchIntent)

                            // 2. Kill the overlay service
                            stopSelf()
                        }
                    )
                }
            }
        }

        windowManager.addView(composeView, params)
        overlayView = composeView
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayView?.let {
            if (it.isAttachedToWindow) {
                windowManager.removeView(it)
            }
        }
        _viewModelStore.clear()
    }
}