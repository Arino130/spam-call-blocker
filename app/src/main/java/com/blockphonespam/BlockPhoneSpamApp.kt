package com.blockphonespam

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.blockphonespam.data.local.AppDatabase
import com.blockphonespam.data.repository.SpamBlockRepository
import com.blockphonespam.worker.KeepAliveWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BlockPhoneSpamApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val repository: SpamBlockRepository by lazy { SpamBlockRepository(database) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        appScope.launch {
            repository.ensureDefaultPrefixesSeeded()
        }
        KeepAliveWorker.schedule(this)
    }

    private fun createNotificationChannels() {
        val notificationManager = getSystemService(NotificationManager::class.java) ?: return

        val protectionChannel = NotificationChannel(
            CHANNEL_PROTECTION,
            getString(R.string.notification_channel_protection_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_protection_desc)
            setShowBadge(false)
        }

        val blockedCallsChannel = NotificationChannel(
            CHANNEL_BLOCKED_CALLS,
            getString(R.string.notification_channel_blocked_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = getString(R.string.notification_channel_blocked_desc)
        }

        notificationManager.createNotificationChannel(protectionChannel)
        notificationManager.createNotificationChannel(blockedCallsChannel)
    }

    companion object {
        const val CHANNEL_PROTECTION = "spam_blocker_protection_channel"
        const val CHANNEL_BLOCKED_CALLS = "spam_blocker_blocked_calls_channel"
    }
}
