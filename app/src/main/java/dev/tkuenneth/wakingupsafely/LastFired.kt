package dev.tkuenneth.wakingupsafely

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/** Remembers when an alarm last went off, and which path delivered it. */
object LastFired {
    const val VIA_BROADCAST = "PendingIntent broadcast"
    const val VIA_LISTENER = "listener (API 37)"

    private const val KEY_TIME = "time"
    private const val KEY_VIA = "via"

    fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences("last_fired", Context.MODE_PRIVATE)

    fun record(context: Context, via: String) {
        prefs(context).edit {
            putLong(KEY_TIME, System.currentTimeMillis())
                .putString(KEY_VIA, via)
        }
    }

    fun time(context: Context): Long = prefs(context).getLong(KEY_TIME, 0L)

    fun via(context: Context): String? = prefs(context).getString(KEY_VIA, null)
}
