package dev.tkuenneth.wakingupsafely

import android.app.AlarmManager
import android.os.Build
import android.os.SystemClock
import androidx.annotation.RequiresApi
import java.util.concurrent.Executor

interface KeepAliveSocket {
    fun ping()
}

const val INTERVAL_MS = 60_000L

// Slide 14: "Our keep-alive on API 37"; onAlarm() is on slide 15
@RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
class Heartbeat(
    private val alarmManager: AlarmManager,
    private val executor: Executor,   // single thread
    private val socket: KeepAliveSocket,
) : AlarmManager.OnAlarmListener {
    private var running = false   // only touched on executor

    fun start() = executor.execute {
        running = true; scheduleNext()
    }
    fun stop() = executor.execute {
        running = false; alarmManager.cancel(this)
    }

    private fun scheduleNext() =
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + INTERVAL_MS,
            "net:keepalive", executor, this,
        )

    override fun onAlarm() {
        if (!running) return            // late delivery
        scheduleNext()                  // first: one-shot
        runCatching { socket.ping() }   // keep looping
    }
}
