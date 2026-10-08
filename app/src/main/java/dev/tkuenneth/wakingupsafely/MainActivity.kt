package dev.tkuenneth.wakingupsafely

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

class MainActivity : ComponentActivity() {
    private lateinit var alarmManager: AlarmManager
    private var heartbeat: Heartbeat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        alarmManager = getSystemService(AlarmManager::class.java)

        // Arm the keep-alive, for example after the first install.
        KeepAlive.schedule(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
            val app = application as WakeApp
            heartbeat = Heartbeat(
                alarmManager,
                app.heartbeatExecutor,
                object : KeepAliveSocket {
                    override fun ping() = LastFired.record(app, LastFired.VIA_LISTENER)
                },
            )
        }

        setContent {
            MaterialTheme {
                MainScreen(alarmManager = alarmManager)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
            heartbeat?.start()
        }
    }

    override fun onStop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
            heartbeat?.stop()
        }
        super.onStop()
    }
}

@Composable
fun MainScreen(alarmManager: AlarmManager) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var time by remember { mutableLongStateOf(LastFired.time(context)) }
    var via by remember { mutableStateOf(LastFired.via(context)) }
    var canScheduleExactAlarms by remember { mutableStateOf(alarmManager.canScheduleExactAlarms()) }

    val prefsListener = remember(context) {
        SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            time = LastFired.time(context)
            via = LastFired.via(context)
        }
    }

    DisposableEffect(context, prefsListener) {
        val prefs = LastFired.prefs(context)
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
        time = LastFired.time(context)
        via = LastFired.via(context)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
        }
    }

    DisposableEffect(lifecycleOwner, alarmManager) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canScheduleExactAlarms = alarmManager.canScheduleExactAlarms()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.last_went_off_label),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = if (time == 0L) stringResource(R.string.not_yet) else friendly(context, time),
                style = MaterialTheme.typography.displayMedium
            )
            Text(
                text = if (time == 0L) {
                    stringResource(R.string.waiting_for_alarm)
                } else {
                    stringResource(R.string.via_prefix, via.orEmpty())
                },
                style = MaterialTheme.typography.bodyLarge
            )
            if (!canScheduleExactAlarms) {
                Button(
                    onClick = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                "package:${context.packageName}".toUri()
                            )
                        )
                    }
                ) {
                    Text(stringResource(R.string.allow_exact_alarms))
                }
            }
        }
    }
}

private fun friendly(context: Context, time: Long): String {
    val day = if (DateUtils.isToday(time)) {
        context.getString(R.string.today)
    } else {
        DateUtils.formatDateTime(context, time, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH)
    }
    val clock = DateUtils.formatDateTime(context, time, DateUtils.FORMAT_SHOW_TIME)
    return "$day, $clock"
}
