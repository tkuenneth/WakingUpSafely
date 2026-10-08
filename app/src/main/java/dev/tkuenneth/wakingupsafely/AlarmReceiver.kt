package dev.tkuenneth.wakingupsafely

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import kotlinx.coroutines.launch

// Slide 8: "What the recipe makes us add in onReceive()"
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val tag = BroadcastDispatcher.tagOf(intent) ?: return
        val work = WorkRegistry[tag] ?: return
        val app = context.applicationContext as WakeApp
        val powerManager = context.getSystemService(PowerManager::class.java)

        val pending = goAsync()
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK, tag
        ).apply { acquire(30_000) }

        app.scope.launch {
            try { work() }
            finally {
                if (wakeLock.isHeld) wakeLock.release()
                pending.finish()
            }
        }
    }
}
