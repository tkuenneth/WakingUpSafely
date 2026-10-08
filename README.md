# Waking Up Safely – sample app

Companion app for the droidCon Berlin 2026 lightning talk
"Waking Up Safely: Reducing Background Wake Locks with Android 17".
It uses the code from the slides.

## What it does

One screen shows when the alarm last went off, in a short friendly format
("Today, 14:32"), and which path delivered it. The screen updates while it is open.

- **The keep-alive** (`KeepAlive`, `Delivery.ALWAYS`) must survive our process
  and reboots, so it goes through `BroadcastDispatcher`: a `PendingIntent`
  alarm and `AlarmReceiver`, the recipe from slide 8. It fires every minute
  (`INTERVAL_MS`) and reschedules itself, because exact alarms are one-shot.
- **The heartbeat** (`Heartbeat`, slides 14 and 15) runs only while the screen is
  visible, and only on Android 17 (API 37): a listener alarm, started in `onStart()`
  and stopped in `onStop()`.
- **`BootReceiver`** re-arms the keep-alive after a reboot, because alarms are
  cancelled when the device shuts down. It also re-arms when the user grants the
  exact alarm permission.

## Where the slide code lives

| Slide | File |
|---|---|
| 8 – What the recipe makes us add in `onReceive()` | `AlarmReceiver.kt` |
| 14, 15 – Our keep-alive on API 37 / When the heartbeat fires | `Heartbeat.kt` |
| 20 – One call site, two mechanisms | `AlarmDispatcher.kt` |
| 21 – The fork, in one place | `AlarmDispatcher.kt` (`alarmDispatcher()`) |
| 22 – Routing by policy | `HybridDispatcher.kt`, `ListenerDispatcher.kt` |
| 23 – The honest part: the legacy side | `BroadcastDispatcher.kt`, `WorkRegistry.kt` |

`ListenerDispatcher` is part of the fork, but this app schedules its keep-alive
with `Delivery.ALWAYS`. The listener path is shown by `Heartbeat`.

### Things to know

- **Exact alarms:** on Android 14 and higher, `SCHEDULE_EXACT_ALARM` is denied by
  default for new installs. Until you tap **Allow exact alarms**, the keep-alive
  falls back to `setAndAllowWhileIdle()`, which is inexact.
- **Boot:** `BOOT_COMPLETED` only reaches the app after it has been launched once.
- **Idle:** Google documents rationing for allow-while-idle alarms. Expect the
  one-minute interval to stretch in Doze and in lower standby buckets.
- **Battery:** firing every minute is for the demo. Real keep-alives should use
  the longest interval the server allows.

### Testing idle (commands from Google's docs)

```
adb shell dumpsys deviceidle force-idle
adb shell dumpsys deviceidle unforce
adb shell dumpsys battery reset
adb shell dumpsys battery unplug
adb shell am set-inactive dev.tkuenneth.wakingupsafely true
adb shell am set-inactive dev.tkuenneth.wakingupsafely false
adb shell am get-standby-bucket dev.tkuenneth.wakingupsafely
```