package com.blockphonespam.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import java.lang.reflect.Method

object CallEndHelper {

    private const val TAG = "CallEndHelper"

    @SuppressLint("MissingPermission")
    fun endCall(context: Context): Boolean {
        // Cách 1: Trên Android 9+ (API 28+), dùng TelecomManager.endCall() nếu có quyền ANSWER_PHONE_CALLS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val hasAnswerCalls = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ANSWER_PHONE_CALLS
            ) == PackageManager.PERMISSION_GRANTED

            if (hasAnswerCalls) {
                try {
                    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                    if (telecomManager != null) {
                        @Suppress("DEPRECATION")
                        val success = telecomManager.endCall()
                        if (success) {
                            Log.d(TAG, "Call ended via TelecomManager.endCall()")
                            return true
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to end call via TelecomManager", e)
                }
            }
        }

        // Cách 2: Reflection qua ITelephony (hỗ trợ các phiên bản và fallback)
        try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (telephonyManager != null) {
                val getITelephonyMethod: Method = telephonyManager.javaClass.getDeclaredMethod("getITelephony")
                getITelephonyMethod.isAccessible = true
                val iTelephony = getITelephonyMethod.invoke(telephonyManager)
                if (iTelephony != null) {
                    val endCallMethod: Method = iTelephony.javaClass.getDeclaredMethod("endCall")
                    endCallMethod.invoke(iTelephony)
                    Log.d(TAG, "Call ended via ITelephony reflection")
                    return true
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to end call via ITelephony reflection", e)
        }

        return false
    }
}
