package com.blockphonespam.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

data class ProtectionPermissionState(
    val hasCallScreeningRole: Boolean = false,
    val isIgnoringBatteryOptimizations: Boolean = false,
    val hasNotificationPermission: Boolean = false,
    val hasPhonePermissions: Boolean = false
) {
    val allCoreGranted: Boolean
        get() = hasCallScreeningRole &&
            isIgnoringBatteryOptimizations &&
            hasNotificationPermission &&
            hasPhonePermissions
}

object PermissionHelper {

    fun checkAll(context: Context): ProtectionPermissionState {
        return ProtectionPermissionState(
            hasCallScreeningRole = isCallScreeningRoleHeld(context),
            isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations(context),
            hasNotificationPermission = hasNotificationPermission(context),
            hasPhonePermissions = hasPhonePermissions(context)
        )
    }

    fun isCallScreeningRoleHeld(context: Context): Boolean {
        val roleManager = context.getSystemService(RoleManager::class.java) ?: return false
        return roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    }

    fun createCallScreeningRoleIntent(context: Context): Intent? {
        val roleManager = context.getSystemService(RoleManager::class.java) ?: return null
        if (!roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) return null
        return roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(PowerManager::class.java) ?: return false
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    @SuppressLint("BatteryLife")
    fun createBatteryOptimizationIntent(context: Context): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasPhonePermissions(context: Context): Boolean {
        val phoneState = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
        val callLog = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
        val answerCalls = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ANSWER_PHONE_CALLS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        return phoneState && callLog && answerCalls
    }

    fun requiredRuntimePermissions(): Array<String> {
        val list = mutableListOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            list.add(Manifest.permission.ANSWER_PHONE_CALLS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return list.toTypedArray()
    }

    fun createAppDetailsSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
