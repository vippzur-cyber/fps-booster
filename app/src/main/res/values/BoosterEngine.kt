package com.asteroid.fpsbooster

import android.app.ActivityManager
import android.app.NotificationManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.provider.Settings

data class BoostResult(
    val ramFreedMb: Long,
    val appsKilled: Int,
    val dndOn: Boolean,
    val animSped: Boolean
)

object BoosterEngine {

    // Package yang jangan di-kill (system critical + app kita sendiri)
    private val WHITELIST = setOf(
        "com.asteroid.fpsbooster",
        "com.android.systemui",
        "android",
        "com.android.phone",
        "com.android.settings"
    )

    fun runFullBoost(context: Context): BoostResult {
        val amBefore = getAvailMem(context)
        val killed = killBackgroundApps(context)
        val amAfter = getAvailMem(context)
        val freed = ((amAfter - amBefore).coerceAtLeast(0)) / (1024 * 1024)

        val dnd = enableDnd(context)
        val anim = speedUpAnimations(context)

        return BoostResult(freed, killed, dnd, anim)
    }

    private fun getAvailMem(context: Context): Long {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return info.availMem
    }

    /** Clear RAM: kill background process semua app non-system yang lagi jalan */
    private fun killBackgroundApps(context: Context): Int {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val pm = context.packageManager
        var killedCount = 0

        val packages = try {
            pm.getInstalledApplications(0)
        } catch (e: Exception) {
            emptyList()
        }

        for (appInfo: ApplicationInfo in packages) {
            val pkg = appInfo.packageName
            if (pkg in WHITELIST) continue
            // skip system app biar gak bikin device error, cuma target app biasa
            val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            if (isSystemApp) continue
            try {
                am.killBackgroundProcesses(pkg)
                killedCount++
            } catch (e: SecurityException) {
                // permission ga cukup buat package ini, skip
            }
        }
        return killedCount
    }

    /** Aktifin Do Not Disturb, perlu Notification Policy Access permission */
    private fun enableDnd(context: Context): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return try {
            if (nm.isNotificationPolicyAccessGranted) {
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun disableDnd(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            if (nm.isNotificationPolicyAccessGranted) {
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            }
        } catch (e: Exception) { }
    }

    /**
     * Speed up window/transition/animator animation system-wide (0.5x = lebih cepat).
     * Butuh WRITE_SECURE_SETTINGS, cuma bisa di-grant via ADB:
     * adb shell pm grant com.asteroid.fpsbooster android.permission.WRITE_SECURE_SETTINGS
     */
    private fun speedUpAnimations(context: Context): Boolean {
        return try {
            Settings.Global.putFloat(context.contentResolver, "window_animation_scale", 0.5f)
            Settings.Global.putFloat(context.contentResolver, "transition_animation_scale", 0.5f)
            Settings.Global.putFloat(context.contentResolver, "animator_duration_scale", 0.5f)
            true
        } catch (e: SecurityException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    fun resetAnimations(context: Context) {
        try {
            Settings.Global.putFloat(context.contentResolver, "window_animation_scale", 1f)
            Settings.Global.putFloat(context.contentResolver, "transition_animation_scale", 1f)
            Settings.Global.putFloat(context.contentResolver, "animator_duration_scale", 1f)
        } catch (e: Exception) { }
    }

    fun hasWriteSecureSettings(context: Context): Boolean {
        return try {
            Settings.Global.putFloat(context.contentResolver, "window_animation_scale",
                Settings.Global.getFloat(context.contentResolver, "window_animation_scale", 1f))
            true
        } catch (e: Exception) {
            false
        }
    }
}
