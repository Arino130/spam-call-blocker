package com.blockphonespam.service

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.blockphonespam.BlockPhoneSpamApp
import com.blockphonespam.MainActivity
import com.blockphonespam.R
import com.blockphonespam.domain.PhoneNumberMatcher
import com.blockphonespam.util.CallEndHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SpamCallScreeningService : CallScreeningService() {

    companion object {
        private const val TAG = "SpamScreeningService"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onScreenCall(callDetails: Call.Details) {
        Log.d(TAG, "onScreenCall received. Direction: ${callDetails.callDirection}, Handle: ${callDetails.handle}")

        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            allowCall(callDetails)
            return
        }

        val rawHandle = callDetails.handle?.schemeSpecificPart
        if (rawHandle.isNullOrBlank()) {
            Log.w(TAG, "rawHandle is null or blank. Allowing call.")
            allowCall(callDetails)
            return
        }
        val rawNumber = Uri.decode(rawHandle)
        if (rawNumber.isNullOrBlank()) {
            Log.w(TAG, "rawNumber is blank after decode. Allowing call.")
            allowCall(callDetails)
            return
        }

        val app = application as BlockPhoneSpamApp
        val repository = app.repository

        serviceScope.launch {
            try {
                val enabledPrefixes = repository.getEnabledPrefixes()
                Log.d(TAG, "Checking incoming call: $rawNumber with enabled prefixes: $enabledPrefixes")

                val matchedPrefix = PhoneNumberMatcher.findMatchingPrefix(rawNumber, enabledPrefixes)

                if (matchedPrefix != null) {
                    Log.i(TAG, "MATCHED SPAM PREFIX: $matchedPrefix for number: $rawNumber. Blocking call!")
                    val normalizedNumber = PhoneNumberMatcher.normalizePhoneNumber(rawNumber)
                    val displayNumber = normalizedNumber.ifEmpty { rawNumber }

                    // Ghi vào CSDL bằng appScope của Application để không bị hủy coroutine khi service bị unbind
                    app.appScope.launch {
                        try {
                            repository.recordBlockedCall(
                                phoneNumber = displayNumber,
                                matchedPrefix = matchedPrefix
                            )
                            Log.i(TAG, "Successfully recorded blocked call into DB: $displayNumber")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to record blocked call into DB", e)
                        }
                    }

                    // Hiển thị thông báo cuộc gọi bị chặn
                    showBlockedNotification(displayNumber, matchedPrefix)

                    // Phản hồi cho hệ điều hành từ chối cuộc gọi
                    val responseBuilder = CallResponse.Builder()
                        .setDisallowCall(true)
                        .setRejectCall(true)
                        .setSkipCallLog(false)
                        .setSkipNotification(true)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        responseBuilder.setSilenceCall(true)
                    }

                    val response = responseBuilder.build()
                    respondToCall(callDetails, response)

                    // Chủ động ngắt cuộc gọi ngay bằng TelecomManager/ITelephony đề phòng ROM hoặc SIM ảo bỏ qua phản hồi
                    CallEndHelper.endCall(applicationContext)
                } else {
                    Log.d(TAG, "Number $rawNumber did not match any spam prefix. Allowing call.")
                    allowCall(callDetails)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error screening call for $rawNumber", e)
                allowCall(callDetails)
            }
        }
    }

    private fun allowCall(callDetails: Call.Details) {
        val response = CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .build()
        respondToCall(callDetails, response)
    }

    private fun showBlockedNotification(phoneNumber: String, matchedPrefix: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            phoneNumber.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, BlockPhoneSpamApp.CHANNEL_BLOCKED_CALLS)
            .setSmallIcon(R.drawable.ic_shield_notification)
            .setContentTitle("Đã chặn cuộc gọi rác")
            .setContentText("Số $phoneNumber (đầu số $matchedPrefix)")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(this).notify(
            (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
            notification
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
