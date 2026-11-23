package com.blockphonespam.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

/**
 * Hỗ trợ mở nhanh màn hình Tự khởi chạy (Auto-Start) / Không giới hạn chạy nền
 * trên các hãng Android hay tự động kill app chạy ngầm (Xiaomi, Oppo, Vivo, Huawei, Samsung...).
 */
object OemAutoStartHelper {

    private val oemAutoStartIntents = listOf(
        // Xiaomi / Redmi / Poco (MIUI / HyperOS)
        Intent().setComponent(
            ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            )
        ),
        // Oppo / Realme (ColorOS)
        Intent().setComponent(
            ComponentName(
                "com.coloros.safecenter",
                "com.coloros.safecenter.permission.startup.StartupAppListActivity"
            )
        ),
        Intent().setComponent(
            ComponentName(
                "com.oppo.safe",
                "com.oppo.safe.permission.startup.StartupAppListActivity"
            )
        ),
        // Vivo (FuntouchOS / OriginOS)
        Intent().setComponent(
            ComponentName(
                "com.vivo.permissionmanager",
                "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
            )
        ),
        Intent().setComponent(
            ComponentName(
                "com.iqoo.secure",
                "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
            )
        ),
        // Huawei / Honor (EMUI / HarmonyOS)
        Intent().setComponent(
            ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
            )
        ),
        Intent().setComponent(
            ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.optimize.process.ProtectActivity"
            )
        ),
        // Samsung (OneUI Device Care)
        Intent().setComponent(
            ComponentName(
                "com.samsung.android.lool",
                "com.samsung.android.sm.battery.ui.BatteryActivity"
            )
        ),
        // OnePlus
        Intent().setComponent(
            ComponentName(
                "com.oneplus.security",
                "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
            )
        ),
        // Asus
        Intent().setComponent(
            ComponentName(
                "com.asus.mobilemanager",
                "com.asus.mobilemanager.autostart.AutoStartActivity"
            )
        )
    )

    fun deviceBrandLabel(): String {
        val manufacturer = Build.MANUFACTURER.orEmpty()
        return manufacturer.replaceFirstChar { it.uppercase() }.ifBlank { "Thiết bị" }
    }

    fun openAutoStartSettings(context: Context) {
        val packageManager = context.packageManager
        for (intent in oemAutoStartIntents) {
            val resolved = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            if (resolved != null) {
                try {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return
                } catch (_: Exception) {
                    // Thử intent tiếp theo nếu không mở được
                }
            }
        }
        // Fallback: mở chi tiết ứng dụng để người dùng chọn Pin -> Không hạn chế
        context.startActivity(PermissionHelper.createAppDetailsSettingsIntent(context))
    }
}
