package com.blockphonespam.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.blockphonespam.BlockPhoneSpamApp
import com.blockphonespam.domain.PhoneNumberMatcher
import com.blockphonespam.util.CallEndHelper
import kotlinx.coroutines.launch

class PhoneStateReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "PhoneStateReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        Log.d(TAG, "Phone state changed: $stateStr, incomingNumber: $incomingNumber")

        if (stateStr == TelephonyManager.EXTRA_STATE_RINGING) {
            val app = context.applicationContext as BlockPhoneSpamApp
            val pendingResult = goAsync()

            app.appScope.launch {
                try {
                    val enabledPrefixes = app.repository.getEnabledPrefixes()

                    if (!incomingNumber.isNullOrBlank()) {
                        val matchedPrefix = PhoneNumberMatcher.findMatchingPrefix(incomingNumber, enabledPrefixes)
                        if (matchedPrefix != null) {
                            Log.i(TAG, "Fallback Receiver: MATCHED PREFIX $matchedPrefix for $incomingNumber. Ending call!")
                            val ended = CallEndHelper.endCall(context)
                            Log.i(TAG, "Fallback endCall result: $ended")

                            val normalizedNumber = PhoneNumberMatcher.normalizePhoneNumber(incomingNumber)
                            val displayNumber = normalizedNumber.ifEmpty { incomingNumber }
                            app.repository.recordBlockedCall(displayNumber, matchedPrefix)
                            Log.i(TAG, "Fallback Receiver successfully recorded blocked call: $displayNumber")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing incoming call in PhoneStateReceiver", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
