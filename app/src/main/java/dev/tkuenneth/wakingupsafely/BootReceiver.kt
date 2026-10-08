package dev.tkuenneth.wakingupsafely

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Alarms are cancelled when the device shuts down, so we re-arm after boot.
 * The same applies when the user grants the exact alarm permission.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
            -> KeepAlive.schedule(context)
        }
    }
}
