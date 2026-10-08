package dev.tkuenneth.wakingupsafely

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri

// Slide 23: "BroadcastDispatcher is our recipe from the start, now behind the interface"
class BroadcastDispatcher(context: Context) : AlarmDispatcher {
    private val context = context.applicationContext
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun schedule(tag: String, at: Long, policy: Delivery, work: () -> Unit) {
        WorkRegistry.register(tag, work)   // the lambda only covers the running process
        val operation = pendingIntent(tag)
        // Slide 23: check canScheduleExactAlarms() on API 31+, and have a plan for when it's revoked
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, operation)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, operation)
        }
    }

    override fun cancel(tag: String) {
        alarmManager.cancel(pendingIntent(tag))
    }

    // One PendingIntent per tag: the tag goes into the data URI, because extras
    // don't make two PendingIntents different.
    private fun pendingIntent(tag: String): PendingIntent =
        PendingIntent.getBroadcast(
            context, 0,
            Intent(context, AlarmReceiver::class.java).setData(tagUri(tag)),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    companion object {
        fun tagUri(tag: String): Uri = Uri.fromParts("alarm", tag, null)
        fun tagOf(intent: Intent): String? = intent.data?.schemeSpecificPart
    }
}
