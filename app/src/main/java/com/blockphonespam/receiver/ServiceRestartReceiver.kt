package com.blockphonespam.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.blockphonespam.service.KeepAliveForegroundService

class ServiceRestartReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        KeepAliveForegroundService.start(context)
    }
}
