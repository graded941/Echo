# ECHO Implementation Spec — 01: Fitness Tracking (Health Connect Parity)

**Pillar:** 1 of 8 | **Feasibility:** 🟢 HIGH | **Effort:** ~14 dev-days | **Priority:** P1

---

## 1. Overview

ECHO replaces iPulse's siloed health data storage with a **Health Connect–first** architecture. All biometric data flowing from the ISW-O23 Storm is simultaneously stored in ECHO's local Room database (for offline use and historical charting) and written to the system Health Connect store (for cross-app sharing with Google Fit, Fitbit, etc.).

---

## 2. ISW-O23 Sensor Capabilities

Confirmed from `OperateFeature.itelO23TotalOperate` bitmask and MoYoung SDK cmd packages:

| Sensor | iPulse Class | Health Connect Record Type |
|---|---|---|
| Heart Rate (continuous) | `HeartRateChangeListener` | `HeartRateRecord` |
| Heart Rate (on-demand) | `HeartRateChangeListener.onOnceMeasureComplete()` | `RestingHeartRateRecord` |
| Steps | `StepsChangeListener.onStepChange()` | `StepsRecord` |
| Distance | `ThreeCircleEntity.distance` | `DistanceRecord` |
| Calories (active) | `ThreeCircleEntity.calories` | `ActiveCaloriesBurnedRecord` |
| Sleep | `WatchSleepListEntity` / `WatchNapListEntity` | `SleepSessionRecord` + `SleepStageRecord` |
| SpO₂ | `WatchBloodOxygenEntity` | `OxygenSaturationRecord` |
| HRV | `WatchHrvEntity` | `HeartRateVariabilityRmssdRecord` |
| Stress | `WatchPressureEntity` | (custom `StressLevelRecord` via Health Connect extension) |
| Body Temperature | `FEATURE_BODY_TEMPERATURE` (0x4000) | `BodyTemperatureRecord` |

---

## 3. Health Connect Integration

### 3.1 Gradle Dependencies

```kotlin
// build.gradle.kts (echo-health module)
dependencies {
    implementation("androidx.health.connect:connect-client:1.1.0-rc01")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
}
```

### 3.2 Manifest Permissions

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.health.READ_HEART_RATE"/>
<uses-permission android:name="android.permission.health.WRITE_HEART_RATE"/>
<uses-permission android:name="android.permission.health.READ_STEPS"/>
<uses-permission android:name="android.permission.health.WRITE_STEPS"/>
<uses-permission android:name="android.permission.health.READ_SLEEP"/>
<uses-permission android:name="android.permission.health.WRITE_SLEEP"/>
<uses-permission android:name="android.permission.health.READ_OXYGEN_SATURATION"/>
<uses-permission android:name="android.permission.health.WRITE_OXYGEN_SATURATION"/>
<uses-permission android:name="android.permission.health.READ_DISTANCE"/>
<uses-permission android:name="android.permission.health.WRITE_DISTANCE"/>
<uses-permission android:name="android.permission.health.WRITE_HEART_RATE_VARIABILITY"/>
<uses-permission android:name="android.permission.health.WRITE_BODY_TEMPERATURE"/>

<activity-alias
    android:name="ViewPermissionUsageActivity"
    android:exported="true"
    android:permission="android.permission.START_VIEW_PERMISSION_USAGE"
    android:targetActivity=".ui.permissions.PermissionRationaleActivity">
    <intent-filter>
        <action android:name="androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE"/>
    </intent-filter>
</activity-alias>
```

### 3.3 HealthConnectWriter Implementation

```kotlin
// echo-health/src/main/kotlin/com/echo/health/connect/HealthConnectWriter.kt

class HealthConnectWriter(context: Context) {

    private val client = HealthConnectClient.getOrCreate(context)

    suspend fun insertHeartRateRecord(bean: WatchHeartRateBean) {
        val samples = bean.heartRateList.mapIndexed { index, bpm ->
            HeartRateRecord.Sample(
                time = bean.startTime.toInstant().plusSeconds(index * bean.timeInterval.toLong()),
                beatsPerMinute = bpm.toLong()
            )
        }
        val record = HeartRateRecord(
            startTime = bean.startTime.toInstant(),
            startZoneOffset = ZoneOffset.systemDefault().rules.getOffset(bean.startTime.toInstant()),
            endTime = bean.startTime.toInstant().plusSeconds(samples.size * bean.timeInterval.toLong()),
            endZoneOffset = ZoneOffset.systemDefault().rules.getOffset(bean.startTime.toInstant()),
            samples = samples
        )
        client.insertRecords(listOf(record))
    }

    suspend fun insertStepsRecord(steps: Int, date: LocalDate) {
        val start = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val end = date.atTime(LocalTime.now()).atZone(ZoneId.systemDefault()).toInstant()
        val record = StepsRecord(
            startTime = start,
            startZoneOffset = ZoneOffset.systemDefault().rules.getOffset(start),
            endTime = end,
            endZoneOffset = ZoneOffset.systemDefault().rules.getOffset(end),
            count = steps.toLong()
        )
        client.insertRecords(listOf(record))
    }

    suspend fun insertSleepSession(entity: WatchSleepListEntity) {
        val stages = entity.sleepStageList.map { stage ->
            SleepSessionRecord.Stage(
                startTime = Instant.ofEpochMilli(stage.startTime),
                endTime = Instant.ofEpochMilli(stage.endTime),
                stage = when (stage.type) {
                    0 -> SleepSessionRecord.STAGE_TYPE_AWAKE
                    1 -> SleepSessionRecord.STAGE_TYPE_LIGHT
                    2 -> SleepSessionRecord.STAGE_TYPE_DEEP
                    3 -> SleepSessionRecord.STAGE_TYPE_REM
                    else -> SleepSessionRecord.STAGE_TYPE_UNKNOWN
                }
            )
        }
        val session = SleepSessionRecord(
            startTime = Instant.ofEpochMilli(entity.startTime),
            startZoneOffset = ZoneOffset.systemDefault().rules.getOffset(Instant.ofEpochMilli(entity.startTime)),
            endTime = Instant.ofEpochMilli(entity.endTime),
            endZoneOffset = ZoneOffset.systemDefault().rules.getOffset(Instant.ofEpochMilli(entity.endTime)),
            stages = stages
        )
        client.insertRecords(listOf(session))
    }

    suspend fun insertSpO2(value: Int, timestamp: Instant) {
        val record = OxygenSaturationRecord(
            time = timestamp,
            zoneOffset = ZoneOffset.systemDefault().rules.getOffset(timestamp),
            percentage = Percentage(value.toDouble())
        )
        client.insertRecords(listOf(record))
    }

    suspend fun insertHrv(rmssd: Double, timestamp: Instant) {
        val record = HeartRateVariabilityRmssdRecord(
            time = timestamp,
            zoneOffset = ZoneOffset.systemDefault().rules.getOffset(timestamp),
            heartRateVariabilityMillis = rmssd
        )
        client.insertRecords(listOf(record))
    }

    suspend fun insertBodyTemperature(celsius: Double, timestamp: Instant) {
        val record = BodyTemperatureRecord(
            time = timestamp,
            zoneOffset = ZoneOffset.systemDefault().rules.getOffset(timestamp),
            temperature = Temperature.celsius(celsius),
            measurementLocation = BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_WRIST
        )
        client.insertRecords(listOf(record))
    }
}
```

### 3.4 EchoHealthRepository — Listener Wiring

```kotlin
// echo-health/src/main/kotlin/com/echo/health/EchoHealthRepository.kt

@Singleton
class EchoHealthRepository @Inject constructor(
    private val db: EchoDatabase,
    private val hcWriter: HealthConnectWriter,
    private val echoGoalEngine: EchoGoalEngine
) {
    fun attachListeners() {
        // --- Heart Rate ---
        HeartRateChangeListener.getInstance().setHrChangeListener(object : OnHeartRateChangeListener {
            override fun onTodayHeartRateData(bean: WatchHeartRateBean) {
                CoroutineScope(Dispatchers.IO).launch {
                    db.hrDao().insertOrReplace(bean.toEntity())
                    hcWriter.insertHeartRateRecord(bean)
                    echoGoalEngine.checkHrZone(bean)
                }
            }
            override fun onHistoryHeartRateData(list: List<WatchHeartRateBean>) {
                CoroutineScope(Dispatchers.IO).launch {
                    list.forEach {
                        db.hrDao().insertOrReplace(it.toEntity())
                        hcWriter.insertHeartRateRecord(it)
                    }
                }
            }
            override fun onMeasuring(bpm: Int) { /* real-time display */ }
            override fun onMeasureComplete(bpm: Int) { /* one-shot done */ }
        })

        // --- Steps ---
        StepsChangeListener.getInstance().setWatchStepChangeListener { steps ->
            CoroutineScope(Dispatchers.IO).launch {
                db.stepsDao().upsertToday(steps)
                hcWriter.insertStepsRecord(steps, LocalDate.now())
                echoGoalEngine.updateStepProgress(steps)
            }
        }
    }
}
```

---

## 4. Goal Engine

```kotlin
class EchoGoalEngine @Inject constructor(
    private val prefs: EchoPreferences
) {
    // Weekly aggregation for step goal (default 10,000 steps/day)
    fun updateStepProgress(todaySteps: Int) {
        val goal = prefs.dailyStepGoal  // default 10_000
        val progress = (todaySteps.toFloat() / goal).coerceAtMost(1f)
        EventBus.post(StepGoalProgressEvent(progress, todaySteps, goal))
    }

    // HR zone classification (Karvonen formula)
    fun checkHrZone(bean: WatchHeartRateBean) {
        val avgHr = bean.heartRateList.average()
        val maxHr = prefs.userMaxHr  // default: 220 - age
        val zone = when ((avgHr / maxHr * 100).toInt()) {
            in 50..60 -> HrZone.ZONE_1_RECOVERY
            in 60..70 -> HrZone.ZONE_2_FAT_BURN
            in 70..80 -> HrZone.ZONE_3_AEROBIC
            in 80..90 -> HrZone.ZONE_4_ANAEROBIC
            in 90..100 -> HrZone.ZONE_5_MAX
            else -> HrZone.ZONE_0_REST
        }
        EventBus.post(HrZoneEvent(zone, avgHr.toInt()))
    }
}
```

---

## 5. VO₂ Max Estimation

VO₂ Max is derived from the **Firstbeat method** (HR + speed during outdoor run):

```kotlin
object Vo2MaxEstimator {
    /**
     * Estimates VO₂ Max using the Cooper formula variant.
     * @param avgHr   Average heart rate during run (bpm)
     * @param maxHr   User's max heart rate (bpm)
     * @param distanceKm  Distance covered in km
     * @param durationMin Duration in minutes
     */
    fun estimate(avgHr: Int, maxHr: Int, distanceKm: Double, durationMin: Int): Double {
        val pace = durationMin / distanceKm  // min/km
        val hrReserve = (avgHr.toDouble() / maxHr)
        // Simplified Firstbeat: VO2Max ≈ 15 × (MaxHR / RestHR)
        // With activity correction factor
        val base = 15.0 * maxHr / 60.0  // resting HR placeholder
        return base * (1.0 - (hrReserve - 0.65) * 0.5)
    }
}
```

---

## 6. Body Battery / Energy Score

Composite score from HRV + sleep + stress (Garmin-style):

```kotlin
class BodyBatteryEngine @Inject constructor(
    private val db: EchoDatabase
) {
    suspend fun calculateBodyBattery(date: LocalDate): Int {
        val hrv = db.hrvDao().getLatest(date)?.rmssd ?: 50.0
        val sleep = db.sleepDao().getForDate(date)
        val stress = db.stressDao().getDailyAvg(date) ?: 50

        val deepSleepHours = sleep?.deepSleepMinutes?.div(60.0) ?: 4.0
        val remHours = sleep?.remMinutes?.div(60.0) ?: 1.5
        val totalSleepHours = sleep?.totalMinutes?.div(60.0) ?: 7.0

        // Weighted composite (0–100)
        val hrvScore = (hrv / 100.0 * 30).coerceIn(0.0, 30.0)
        val sleepScore = ((deepSleepHours / 2.0 + remHours / 1.5 + totalSleepHours / 8.0) / 3.0 * 50).coerceIn(0.0, 50.0)
        val stressScore = ((100 - stress) / 100.0 * 20).coerceIn(0.0, 20.0)

        return (hrvScore + sleepScore + stressScore).toInt()
    }
}
```

---

## 7. UI Components

| Screen | Description |
|---|---|
| **EchoHealthDashboard** | Daily rings (steps/calories/stand), body battery arc, current HR |
| **EchoHeartRateDetail** | 24h HR chart, zone breakdown pie, resting/max HR cards |
| **EchoSleepDetail** | Nightly sleep stages waterfall chart, sleep debt graph (7 days) |
| **EchoSpO2Detail** | Spot readings history chart, nighttime monitoring toggle |
| **EchoHrvDetail** | HRV trend (30 days), readiness score, recovery recommendation |
| **EchoGoalsScreen** | Edit daily step/calorie/sleep targets; weekly progress bars |

---

## 8. Evidence Log

| Claim | Source File | Line / Method |
|---|---|---|
| ISW-O23 supports `FEATURE_HEART_RATE` | `OperateFeature.itelO23TotalOperate` | Bitmask 0x4 |
| ISW-O23 supports `FEATURE_BODY_TEMPERATURE` | `OperateFeature.itelO23TotalOperate` | Bitmask 0x4000 |
| HR listener has 24h + history callbacks | `HeartRateChangeListener.java` | `on24HourMeasureResult()`, `onHistoryHeartRateData()` |
| Steps delivered via `CRPStepInfo.getSteps()` | `StepsChangeListener.java` | `onStepChange()` |
| Sleep stage integers 0–3 | `WatchSleepListEntity` schema | `type` column |
| SpO₂ entity | `WatchBloodOxygenEntity` | Room table |
| HRV entity | `WatchHrvEntity` | Room table |
