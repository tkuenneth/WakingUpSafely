package dev.tkuenneth.wakingupsafely

import android.content.Context
import android.os.Build

// Slide 20: "One call site, two mechanisms"
enum class Delivery { WHILE_RUNNING, ALWAYS }

/** [at] is in [android.os.SystemClock.elapsedRealtime] time. */
interface AlarmDispatcher {
    fun schedule(tag: String, at: Long, policy: Delivery, work: () -> Unit)
    fun cancel(tag: String)
}

// Slide 21: "The fork, in one place"
fun alarmDispatcher(context: Context): AlarmDispatcher =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN)
        HybridDispatcher(
            ListenerDispatcher(context),
            BroadcastDispatcher(context),
        )
    else
        BroadcastDispatcher(context)
