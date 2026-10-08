package dev.tkuenneth.wakingupsafely

import android.content.Context
import android.os.SystemClock

/**
 * The keep-alive from the story: it must survive our process and reboots,
 * so it runs with [Delivery.ALWAYS] through the dispatcher.
 */
object KeepAlive {
    const val TAG = "net:keepalive"

    /** The work for [TAG]. Registered at app start, so cold starts find it. */
    fun work(context: Context): () -> Unit {
        val app = context.applicationContext
        return {
            LastFired.record(app, LastFired.VIA_BROADCAST)
            schedule(app)   // exact alarms are one-shot
        }
    }

    fun schedule(context: Context) {
        val app = context.applicationContext as WakeApp
        app.dispatcher.schedule(
            TAG,
            SystemClock.elapsedRealtime() + INTERVAL_MS,
            Delivery.ALWAYS,
            work(app),
        )
    }
}
