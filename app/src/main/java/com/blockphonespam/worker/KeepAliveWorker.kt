package com.blockphonespam.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.blockphonespam.service.KeepAliveForegroundService
import java.util.concurrent.TimeUnit

class KeepAliveWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (!KeepAliveForegroundService.isRunning.get()) {
            KeepAliveForegroundService.start(applicationContext)
        }
        return Result.success()
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "block_phone_spam_keep_alive_watchdog"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<KeepAliveWorker>(
                15, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
