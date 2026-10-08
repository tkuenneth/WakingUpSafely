package dev.tkuenneth.wakingupsafely

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

// Slide 22: "ListenerDispatcher is our heartbeat, keyed by tag"
@RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
class ListenerDispatcher(context: Context) : AlarmDispatcher {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val executor = Executors.newSingleThreadExecutor()   // work may do network I/O
    private val listeners = ConcurrentHashMap<String, AlarmManager.OnAlarmListener>()

    override fun schedule(tag: String, at: Long, policy: Delivery, work: () -> Unit) {
        cancel(tag)
        val listener = object : AlarmManager.OnAlarmListener {
            override fun onAlarm() {
                if (listeners.remove(tag, this)) work()   // skip a late delivery after cancel()
            }
        }
        listeners[tag] = listener
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP, at, tag, executor, listener,
        )
    }

    override fun cancel(tag: String) {
        listeners.remove(tag)?.let(alarmManager::cancel)
    }
}
