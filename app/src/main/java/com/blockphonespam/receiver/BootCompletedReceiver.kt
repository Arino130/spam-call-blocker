package com.blockphonespam.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.blockphonespam.service.KeepAliveForegroundService
import com.blockphonespam.worker.KeepAliveWorker

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val shouldStart = action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"

        if (shouldStart) {
            KeepAliveForegroundService.start(context)
            KeepAliveWorker.schedule(context)
        }
    }
}
