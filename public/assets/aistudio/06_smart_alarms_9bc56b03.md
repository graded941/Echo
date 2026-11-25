# ECHO Implementation Spec — 06: Smart Alarms & Do Not Disturb

**Pillar:** 6 of 8 | **Feasibility:** 🟡 MODERATE | **Effort:** ~9 dev-days | **Priority:** P2

---

## 1. Overview

ECHO implements smart wake alarms that analyse sleep stages to trigger vibration at the optimal light-sleep window, plus a bidirectional DND sync between the watch and itel P55.

---

## 2. iPulse Foundation — Inherited Classes

| Class | Capability |
|---|---|
| `AlarmBuilder.build(WatchAlarmBean)` | Converts `WatchAlarmBean` → `CRPAlarmInfo` and sends to watch via GATT |
| `AlarmListener.java` | Receives alarm trigger events from ISW-O23 |
| `WatchSleepListEntity` | Nightly sleep session with stage array |
| `WatchNapListEntity` | Daytime nap staging |
| `cmd/alarm/AlarmBuilder.java` | Full GATT alarm construction (ID, enable, hour, minute, repeatMode, date) |

**From `AlarmBuilder.java`:**
```java
cRPAlarmInfo.setId(watchAlarmBean.getId());         // alarm slot ID
cRPAlarmInfo.setEnable(watchAlarmBean.getEnable()); // on/off
cRPAlarmInfo.setRepeatMode(repeatMode);             // 0=once, bitmask=days of week
cRPAlarmInfo.setHour(watchAlarmBean.getHour());
cRPAlarmInfo.setMinute(watchAlarmBean.getMinute());
cRPAlarmInfo.setDate(date);                         // null for repeating
```

**Sleep stage codes** (confirmed from `WatchSleepListEntity`):
| Code | Meaning |
|---|---|
| 0 | Awake |
| 1 | Light sleep |
| 2 | Deep sleep |
| 3 | REM |

---

## 3. Smart Wake Engine

```kotlin
// echo-alarms/src/main/kotlin/com/echo/alarms/SmartWakeEngine.kt

data class SleepSegment(
    val startEpochMs: Long,
    val endEpochMs: Long,
    val stage: Int  // 0=Awake, 1=Light, 2=Deep, 3=REM
)

class SmartWakeEngine @Inject constructor(
    private val sleepDao: SleepDao
) {
    companion object {
        const val WINDOW_BEFORE_ALARM_MS = 30 * 60 * 1000L  // 30 minutes before
        const val WINDOW_AFTER_ALARM_MS  =  5 * 60 * 1000L  // 5 minutes after (grace)
        const val LIGHT_SLEEP_STAGE = 1
    }

    /**
     * Calculates optimal alarm trigger time within [targetTimeMs - 30min, targetTimeMs + 5min].
     * Returns targetTimeMs as fallback if no light sleep window found.
     */
    suspend fun calculateOptimalWakeTime(targetTimeMs: Long): Long {
        val windowStart = targetTimeMs - WINDOW_BEFORE_ALARM_MS
        val windowEnd = targetTimeMs + WINDOW_AFTER_ALARM_MS

        val segments = sleepDao.getSegmentsBetween(windowStart, windowEnd)
        val lightSegments = segments.filter { it.stage == LIGHT_SLEEP_STAGE }

        if (lightSegments.isEmpty()) {
            return targetTimeMs  // fallback: standard alarm
        }

        // Find the segment closest to the original alarm time
        // Prefer waking at the END of a light sleep segment (natural wake point)
        val bestWakeTime = lightSegments
            .minByOrNull { Math.abs(it.endEpochMs - targetTimeMs) }
            ?.endEpochMs
            ?: targetTimeMs

        return bestWakeTime.coerceIn(windowStart, windowEnd)
    }

    suspend fun scheduleSmartAlarm(
        targetHour: Int,
        targetMinute: Int,
        repeatMode: Int
    ): WatchAlarmBean {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
        }

        val optimalMs = calculateOptimalWakeTime(calendar.timeInMillis)
        val optimalCalendar = Calendar.getInstance().apply { timeInMillis = optimalMs }

        return WatchAlarmBean().apply {
            id = generateAlarmId()
            enable = true
            hour = optimalCalendar.get(Calendar.HOUR_OF_DAY)
            minute = optimalCalendar.get(Calendar.MINUTE)
            this.repeatMode = repeatMode
            date = if (repeatMode == 0) optimalCalendar.time else null
        }
    }

    private fun generateAlarmId() = (System.currentTimeMillis() % 100).toInt()
}
```

---

## 4. EchoAlarmManager

```kotlin
// echo-alarms/src/main/kotlin/com/echo/alarms/EchoAlarmManager.kt

@Singleton
class EchoAlarmManager @Inject constructor(
    private val smartWakeEngine: SmartWakeEngine,
    private val alarmDao: AlarmDao,
    private val bleBridge: EchoBleManager
) {
    suspend fun setAlarm(
        hour: Int,
        minute: Int,
        repeatMode: Int,
        smartWake: Boolean = true
    ): EchoAlarm {
        val watchBean = if (smartWake) {
            smartWakeEngine.scheduleSmartAlarm(hour, minute, repeatMode)
        } else {
            WatchAlarmBean().apply {
                id = generateAlarmId()
                enable = true
                this.hour = hour
                this.minute = minute
                this.repeatMode = repeatMode
            }
        }

        // Push to watch via MoYoung GATT
        val cRPAlarmInfo = AlarmBuilder.build(watchBean)
        bleBridge.sendAlarm(cRPAlarmInfo)

        // Store locally
        val alarm = EchoAlarm(watchBean.id, hour, minute, repeatMode, smartWake)
        alarmDao.insert(alarm)
        return alarm
    }

    suspend fun deleteAlarm(id: Int) {
        val watchBean = WatchAlarmBean().apply { this.id = id; enable = false }
        bleBridge.sendAlarm(AlarmBuilder.build(watchBean))
        alarmDao.delete(id)
    }

    fun getAlarms(): Flow<List<EchoAlarm>> = alarmDao.getAll()
}
```

---

## 5. DND Scheduler

### 5.1 DndScheduler

```kotlin
// echo-alarms/src/main/kotlin/com/echo/alarms/DndScheduler.kt

@Singleton
class DndScheduler @Inject constructor(
    private val notifManager: NotificationManager,
    private val prefs: EchoPreferences,
    private val bleBridge: EchoBleManager
) {
    data class DndRule(
        val id: String,
        val startHour: Int,
        val startMinute: Int,
        val endHour: Int,
        val endMinute: Int,
        val daysOfWeek: Set<Int>,  // Calendar.MONDAY..SUNDAY
        val allowCalls: Boolean = true,
        val allowAlarms: Boolean = true
    )

    fun isActive(): Boolean {
        val now = Calendar.getInstance()
        val nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val day = now.get(Calendar.DAY_OF_WEEK)

        return prefs.dndRules.any { rule ->
            if (day !in rule.daysOfWeek) return@any false
            val startMin = rule.startHour * 60 + rule.startMinute
            val endMin = rule.endHour * 60 + rule.endMinute
            if (startMin <= endMin) {
                nowMin in startMin..endMin
            } else {
                // Overnight rule (e.g. 22:00–07:00)
                nowMin >= startMin || nowMin <= endMin
            }
        }
    }

    suspend fun activateDnd() {
        // Android phone DND
        if (notifManager.isNotificationPolicyAccessGranted) {
            notifManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
        }
        // Watch DND (via GATT `FEATURE_NO_DISTURB_MODEL`)
        bleBridge.sendDndMode(enabled = true)
    }

    suspend fun deactivateDnd() {
        if (notifManager.isNotificationPolicyAccessGranted) {
            notifManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        }
        bleBridge.sendDndMode(enabled = false)
    }

    // Called by WorkManager every 15 minutes
    suspend fun tick() {
        if (isActive()) activateDnd() else deactivateDnd()
    }
}
```

### 5.2 WorkManager Schedule

```kotlin
// Schedule DND check every 15 minutes
val dndWork = PeriodicWorkRequestBuilder<DndTickWorker>(15, TimeUnit.MINUTES)
    .setConstraints(Constraints.Builder()
        .setRequiresBatteryNotLow(false)
        .build())
    .build()
WorkManager.getInstance(context)
    .enqueueUniquePeriodicWork("echo_dnd_tick", ExistingPeriodicWorkPolicy.KEEP, dndWork)
```

---

## 6. Sunrise Alarm (Brightness Ramp)

```kotlin
// echo-alarms/src/main/kotlin/com/echo/alarms/SunriseAlarmService.kt

class SunriseAlarmService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var currentBrightness = 0

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val targetBrightness = Settings.System.getInt(contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)

        // Ramp brightness from 0 to max over 30 minutes (1800 seconds)
        // Update every 30 seconds: 60 steps
        rampBrightness(steps = 60, intervalMs = 30_000L)
        return START_NOT_STICKY
    }

    private fun rampBrightness(steps: Int, intervalMs: Long) {
        var step = 0
        handler.post(object : Runnable {
            override fun run() {
                if (step >= steps) return
                val brightness = (step * 255 / steps).coerceIn(1, 255)
                Settings.System.putInt(contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS, brightness)
                step++
                handler.postDelayed(this, intervalMs)
            }
        })
    }
}
```

Requires: `android.permission.WRITE_SETTINGS` (user must grant via system settings dialog).

---

## 7. Sleep Analytics

```kotlin
class SleepAnalyticsEngine @Inject constructor(private val sleepDao: SleepDao) {

    data class SleepReport(
        val date: LocalDate,
        val totalHours: Double,
        val deepHours: Double,
        val remHours: Double,
        val lightHours: Double,
        val wakeCount: Int,
        val qualityScore: Int  // 0–100
    )

    suspend fun getReport(date: LocalDate): SleepReport {
        val session = sleepDao.getForDate(date) ?: return SleepReport(date, 0.0, 0.0, 0.0, 0.0, 0, 0)
        val segments = session.segments
        val totalMs = session.endTime - session.startTime
        val deepMs = segments.filter { it.stage == 2 }.sumOf { it.endEpochMs - it.startEpochMs }
        val remMs = segments.filter { it.stage == 3 }.sumOf { it.endEpochMs - it.startEpochMs }
        val lightMs = segments.filter { it.stage == 1 }.sumOf { it.endEpochMs - it.startEpochMs }
        val wakeCount = segments.count { it.stage == 0 }

        val quality = calculateQuality(totalMs, deepMs, remMs, wakeCount)
        return SleepReport(date,
            totalMs.toHours(), deepMs.toHours(), remMs.toHours(), lightMs.toHours(),
            wakeCount, quality)
    }

    private fun calculateQuality(totalMs: Long, deepMs: Long, remMs: Long, wakeCount: Int): Int {
        val totalH = totalMs.toHours()
        val durationScore = (totalH / 8.0 * 40).coerceIn(0.0, 40.0)
        val deepScore = (deepMs.toHours() / 2.0 * 30).coerceIn(0.0, 30.0)
        val remScore = (remMs.toHours() / 1.5 * 20).coerceIn(0.0, 20.0)
        val wakeScore = ((5 - wakeCount).coerceAtLeast(0) * 2.0)
        return (durationScore + deepScore + remScore + wakeScore).toInt()
    }

    private fun Long.toHours() = this / 3_600_000.0

    suspend fun getSleepDebt(days: Int = 7): Double {
        val goal = 8.0  // hours
        return (0 until days).sumOf { daysAgo ->
            val date = LocalDate.now().minusDays(daysAgo.toLong())
            val report = getReport(date)
            (goal - report.totalHours).coerceAtLeast(0.0)
        }
    }
}
```

---

## 8. UI Screens

| Screen | Description |
|---|---|
| **AlarmsScreen** | List of alarms; each shows target time, smart wake badge (±30 min window), repeat days |
| **AlarmEditorScreen** | Time picker, smart wake toggle, label, repeat day selector |
| **DndScheduleScreen** | List of DND rules; add/edit time windows; call exception toggle |
| **SleepReportScreen** | Nightly sleep stages waterfall chart, quality score, sleep debt graph |
| **SunriseAlarmCard** | Per-alarm toggle for 30-min brightness ramp |

---

## 9. Required Permissions

```xml
<uses-permission android:name="android.permission.WRITE_SETTINGS"/>
<uses-permission android:name="android.permission.ACCESS_NOTIFICATION_POLICY"/>
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM"/>
<uses-permission android:name="android.permission.USE_EXACT_ALARM"/>
```

---

## 10. Evidence Log

| Claim | Source File | Evidence |
|---|---|---|
| `AlarmBuilder.build()` sends `CRPAlarmInfo` via GATT | `AlarmBuilder.java` | Full class implementation |
| `repeatMode=0` = once-only alarm | `AlarmBuilder.java` | `if (date != null && repeatMode == 0 && DateUtils.isToday(...))` |
| Sleep stage codes 0–3 | `WatchSleepListEntity` | Room entity column `type` |
| `FEATURE_NO_DISTURB_MODEL` exists | `OperateFeature.java` | Constant in feature bitmask |
| `FEATURE_INTELLIGENT_CLOCK` exists | `OperateFeature.java` | Constant in feature bitmask |
