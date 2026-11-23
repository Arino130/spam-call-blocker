package com.blockphonespam.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.blockphonespam.BlockPhoneSpamApp
import com.blockphonespam.MainActivity
import com.blockphonespam.R
import com.blockphonespam.receiver.ServiceRestartReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class KeepAliveForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var observerJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        isRunning.set(true)
        startForegroundNotification(activePrefixCount = null)
        observeActivePrefixes()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning.set(true)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        scheduleRestart(applicationContext)
    }

    override fun onDestroy() {
        isRunning.set(false)
        observerJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun observeActivePrefixes() {
        observerJob?.cancel()
        val repository = (application as BlockPhoneSpamApp).repository
        observerJob = serviceScope.launch {
            repository.ensureDefaultPrefixesSeeded()
            repository.observePrefixes().collectLatest { rules ->
                val activeCount = rules.count { it.isEnabled }
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.notify(
                    NOTIFICATION_ID,
                    buildNotification(activePrefixCount = activeCount)
                )
            }
        }
    }

    private fun startForegroundNotification(activePrefixCount: Int?) {
        val notification = buildNotification(activePrefixCount)
        val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, serviceType)
    }

    private fun buildNotification(activePrefixCount: Int?): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = when (activePrefixCount) {
            null -> "Đang bảo vệ cuộc gọi chạy ngầm"
            0 -> "Đang chạy ngầm • Chưa bật đầu số chặn nào"
            else -> "Đang chạy ngầm • Tự động chặn $activePrefixCount đầu số"
        }

        return NotificationCompat.Builder(this, BlockPhoneSpamApp.CHANNEL_PROTECTION)
            .setSmallIcon(R.drawable.ic_shield_notification)
            .setContentTitle("Chặn Cuộc Gọi Rác đang hoạt động")
            .setContentText(contentText)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(pendingIntent)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val RESTART_REQUEST_CODE = 2001

        val isRunning = AtomicBoolean(false)

        fun start(context: Context) {
            val intent = Intent(context.applicationContext, KeepAliveForegroundService::class.java)
            try {
                ContextCompat.startForegroundService(context.applicationContext, intent)
            } catch (_: Exception) {
                // Trên Android 12+, nếu hệ thống chặn khởi chạy FGS từ nền, WorkManager sẽ thử lại.
            }
        }

        fun scheduleRestart(context: Context) {
            val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
            val restartIntent = Intent(context.applicationContext, ServiceRestartReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context.applicationContext,
                RESTART_REQUEST_CODE,
                restartIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val triggerAtMillis = SystemClock.elapsedRealtime() + 2_000L
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }
}
