package dev.tkuenneth.wakingupsafely

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class WakeApp : Application() {
    /** Application-wide scope on Dispatchers.IO: the work may do network I/O. */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Slide 14: the heartbeat runs on one background thread, kept for the life of the process. */
    val heartbeatExecutor: Executor = Executors.newSingleThreadExecutor()

    /** Slide 22: create the dispatcher once and share it. */
    val dispatcher: AlarmDispatcher by lazy { alarmDispatcher(this) }

    override fun onCreate() {
        super.onCreate()
        // Application.onCreate() runs before any receiver is created,
        // so AlarmReceiver finds the work after a cold start.
        WorkRegistry.register(KeepAlive.TAG, KeepAlive.work(this))
    }
}
