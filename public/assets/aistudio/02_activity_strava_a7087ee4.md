# ECHO Implementation Spec — 02: Activity History & Monitoring (Strava Parity)

**Pillar:** 2 of 8 | **Feasibility:** 🟢 HIGH | **Effort:** ~17 dev-days | **Priority:** P1

---

## 1. Overview

ECHO turns the itel ISW-O23 into a full activity tracker rivalling Strava by:
1. Capturing GPS route data via the phone during outdoor sport sessions (ISW-O23 has no onboard GPS)
2. Storing sessions in a local Room database (inherited `MotionRecordEntity`)
3. Exporting GPX files and uploading to Strava API v3
4. Rendering interactive maps, pace splits, and lap analysis in-app

---

## 2. iPulse Foundation — Inherited Classes

| Class | Location | ECHO Use |
|---|---|---|
| `MotionRecordEntity` | `decompiled/sources/.../db/entity/` | Sport session storage |
| `WatchSportListEntity` | Same | Per-session sport list |
| `SportLocationProvider.java` | `decompiled/sources/...` | GPS coordinate capture during outdoor sport |
| `GPSTrainingUtils.isGPSTraining(int)` | `cmd/gps/GPSTrainingUtils.java` | Identifies GPS sport IDs 30–34 |
| `TrainingProvider.java` | `cmd/training/TrainingProvider.java` | Retrieves history sessions from watch |
| `TrainingChangeListener.java` | `cmd/training/TrainingChangeListener.java` | Real-time sport start/stop callbacks |
| `TrainingTypeConverter.java` | `cmd/training/TrainingTypeConverter.java` | Maps MoYoung int → sport name |
| `EpoFileDownloader.java` | `cmd/gps/EpoFileDownloader.java` | Assisted GPS ephemeris update |
| `EpoFileUploader.java` | `cmd/gps/EpoFileUploader.java` | Push EPO data to watch |

**GPS Sport Type IDs** (from `GPSTrainingUtils.isGPSTraining()`):
- Sport IDs 30–34 trigger phone GPS capture
- Mapping (from `TrainingTypeConverter`): 30=Outdoor Run, 31=Outdoor Ride, 32=Hike, 33=Trail Run, 34=Walk

---

## 3. Strava OAuth 2.0 Flow

### 3.1 App Registration

Register ECHO at [strava.com/settings/api](https://www.strava.com/settings/api):
- Authorization Callback Domain: `echo://strava/callback`
- Scopes needed: `activity:write,activity:read_all,read`

### 3.2 StravaAuthManager

```kotlin
// echo-activity/src/main/kotlin/com/echo/activity/strava/StravaAuthManager.kt

object StravaConfig {
    const val CLIENT_ID = "YOUR_STRAVA_CLIENT_ID"
    const val AUTH_URL = "https://www.strava.com/oauth/authorize"
    const val TOKEN_URL = "https://www.strava.com/oauth/token"
    const val REDIRECT_URI = "echo://strava/callback"
    const val SCOPE = "activity:write,activity:read_all,read"
}

class StravaAuthManager @Inject constructor(
    private val tokenStore: StravaTokenStore,
    private val httpClient: OkHttpClient
) {
    fun buildAuthIntent(): Intent {
        val authUri = Uri.parse(StravaConfig.AUTH_URL).buildUpon()
            .appendQueryParameter("client_id", StravaConfig.CLIENT_ID)
            .appendQueryParameter("redirect_uri", StravaConfig.REDIRECT_URI)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("approval_prompt", "auto")
            .appendQueryParameter("scope", StravaConfig.SCOPE)
            .build()
        return Intent(Intent.ACTION_VIEW, authUri)
    }

    suspend fun exchangeCode(code: String): StravaToken {
        val body = FormBody.Builder()
            .add("client_id", StravaConfig.CLIENT_ID)
            .add("client_secret", BuildConfig.STRAVA_CLIENT_SECRET)
            .add("code", code)
            .add("grant_type", "authorization_code")
            .build()
        val request = Request.Builder().url(StravaConfig.TOKEN_URL).post(body).build()
        val response = httpClient.newCall(request).await()
        val token = Json.decodeFromString<StravaToken>(response.body!!.string())
        tokenStore.save(token)
        return token
    }

    suspend fun refreshToken(): StravaToken {
        val current = tokenStore.get() ?: throw IllegalStateException("No token stored")
        if (current.expiresAt > System.currentTimeMillis() / 1000 + 300) return current
        val body = FormBody.Builder()
            .add("client_id", StravaConfig.CLIENT_ID)
            .add("client_secret", BuildConfig.STRAVA_CLIENT_SECRET)
            .add("refresh_token", current.refreshToken)
            .add("grant_type", "refresh_token")
            .build()
        val request = Request.Builder().url(StravaConfig.TOKEN_URL).post(body).build()
        val response = httpClient.newCall(request).await()
        val fresh = Json.decodeFromString<StravaToken>(response.body!!.string())
        tokenStore.save(fresh)
        return fresh
    }
}
```

---

## 4. GPX Exporter

```kotlin
// echo-activity/src/main/kotlin/com/echo/activity/gps/GpxExporter.kt

object GpxExporter {
    fun export(session: MotionRecordEntity, path: List<GpsPoint>): File {
        val gpxDir = File(EchoApp.context.filesDir, "gpx")
        gpxDir.mkdirs()
        val file = File(gpxDir, "session_${session.id}_${session.startTime}.gpx")

        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val xml = buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("""<gpx version="1.1" creator="ECHO" xmlns="http://www.topografix.com/GPX/1/1">""")
            appendLine("""  <metadata><name>${session.sportType} ${isoFormat.format(session.startTime)}</name></metadata>""")
            appendLine("""  <trk><name>ECHO Activity</name><trkseg>""")
            path.forEach { pt ->
                appendLine("""    <trkpt lat="${pt.latitude}" lon="${pt.longitude}">""")
                pt.altitude?.let { appendLine("""      <ele>$it</ele>""") }
                appendLine("""      <time>${isoFormat.format(Date(pt.timestamp))}</time>""")
                appendLine("""    </trkpt>""")
            }
            appendLine("""  </trkseg></trk>""")
            appendLine("""</gpx>""")
        }
        file.writeText(xml)
        return file
    }
}
```

---

## 5. Strava Uploader

```kotlin
// echo-activity/src/main/kotlin/com/echo/activity/strava/StravaUploader.kt

class StravaUploader @Inject constructor(
    private val authManager: StravaAuthManager,
    private val httpClient: OkHttpClient
) {
    companion object {
        const val API_BASE = "https://www.strava.com/api/v3"
    }

    // Map MoYoung sport type → Strava sport_type string
    private fun mapSportType(moyoungType: Int): String = when (moyoungType) {
        30 -> "Run"
        31 -> "Ride"
        32 -> "Hike"
        33 -> "TrailRun"
        34 -> "Walk"
        else -> "Workout"
    }

    suspend fun uploadActivity(session: MotionRecordEntity, gpxFile: File): String {
        val token = authManager.refreshToken()
        val sportType = mapSportType(session.sportType)
        val isoStart = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(session.startTime)

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("name", "ECHO $sportType ${isoStart.substring(0, 10)}")
            .addFormDataPart("sport_type", sportType)
            .addFormDataPart("start_date_local", isoStart)
            .addFormDataPart("elapsed_time", session.duration.toString())
            .addFormDataPart("distance", session.distanceMeters.toString())
            .addFormDataPart("description", "Recorded by ECHO on itel ISW-O23 Storm")
            .addFormDataPart("data_type", "gpx")
            .addFormDataPart("file", gpxFile.name,
                gpxFile.asRequestBody("application/gpx+xml".toMediaType()))
            .build()

        val request = Request.Builder()
            .url("$API_BASE/uploads")
            .header("Authorization", "Bearer ${token.accessToken}")
            .post(body)
            .build()

        val response = httpClient.newCall(request).await()
        val uploadResult = Json.decodeFromString<StravaUploadResult>(response.body!!.string())
        return uploadResult.id.toString()
    }
}
```

---

## 6. WorkManager — Offline Upload Queue

```kotlin
// echo-activity/src/main/kotlin/com/echo/activity/strava/StravaUploadWorker.kt

class StravaUploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val sessionId = inputData.getLong("session_id", -1)
        if (sessionId == -1L) return Result.failure()
        return try {
            val repo = EchoActivityRepository.get()
            val session = repo.getSession(sessionId) ?: return Result.failure()
            val gpxFile = GpxExporter.export(session, repo.getPath(sessionId))
            val uploader = StravaUploader.get()
            val stravaId = uploader.uploadActivity(session, gpxFile)
            repo.markUploaded(sessionId, stravaId)
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        fun enqueue(context: Context, sessionId: Long) {
            val request = OneTimeWorkRequestBuilder<StravaUploadWorker>()
                .setInputData(workDataOf("session_id" to sessionId))
                .setConstraints(Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
```

---

## 7. Pace & Lap Analysis

```kotlin
object LapAnalyzer {
    data class Lap(val lapNumber: Int, val distanceKm: Double, val paceMinPerKm: Double, val avgHr: Int)

    fun analyze(path: List<GpsPoint>, hrData: List<Int>): List<Lap> {
        val laps = mutableListOf<Lap>()
        var lapStartIndex = 0
        var cumulativeDistance = 0.0

        path.zipWithNext().forEachIndexed { index, (a, b) ->
            cumulativeDistance += haversineKm(a.latitude, a.longitude, b.latitude, b.longitude)
            if (cumulativeDistance >= (laps.size + 1).toDouble()) {
                val lapPath = path.subList(lapStartIndex, index + 2)
                val lapDuration = (lapPath.last().timestamp - lapPath.first().timestamp) / 1000.0 / 60.0
                val lapHr = if (hrData.isNotEmpty()) hrData.subList(
                    (lapStartIndex * hrData.size / path.size).coerceIn(0, hrData.lastIndex),
                    ((index + 2) * hrData.size / path.size).coerceIn(0, hrData.lastIndex)
                ).average().toInt() else 0
                laps.add(Lap(laps.size + 1, 1.0, lapDuration, lapHr))
                lapStartIndex = index + 1
            }
        }
        return laps
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return R * 2 * asin(sqrt(a))
    }
}
```

---

## 8. Map Route Rendering

Use **MapLibre GL Android SDK** (open-source, no API key for base tiles):

```kotlin
// In ActivityMapFragment
private fun drawRoute(path: List<GpsPoint>) {
    val lineString = LineString.fromLngLats(
        path.map { Point.fromLngLat(it.longitude, it.latitude) }
    )
    val source = GeoJsonSource("echo-route", lineString)
    mapboxMap.style?.addSource(source)

    val layer = LineLayer("echo-route-layer", "echo-route").apply {
        setProperties(
            lineColor(Color.parseColor("#FF5733")),  // ECHO brand orange
            lineWidth(4f),
            lineCap(LINE_CAP_ROUND),
            lineJoin(LINE_JOIN_ROUND)
        )
    }
    mapboxMap.style?.addLayer(layer)
}
```

---

## 9. UI Screens

| Screen | Description |
|---|---|
| **ActivityFeedScreen** | Strava-style feed: today's activity cards, weekly distance total, monthly stats |
| **ActivityDetailScreen** | Map route, elevation profile, pace chart, lap splits table, HR zone breakdown |
| **ActivityCompareScreen** | Compare two runs: pace overlay, HR overlay |
| **StravaConnectScreen** | OAuth login, sync status, disconnect |
| **SportsCalendarScreen** | Month view: run days highlighted, weekly distance |

---

## 10. Evidence Log

| Claim | Source File | Evidence |
|---|---|---|
| GPS sport IDs 30–34 | `GPSTrainingUtils.java` | `return 30 <= i && i < 35` |
| EPO file management | `EpoFileDownloader.java`, `EpoFileUploader.java` | Full class implementations present |
| Sport session entity | `MotionRecordEntity` | Room `@Entity` class in db/entity/ |
| `TrainingChangeListener` real-time sport events | `cmd/training/TrainingChangeListener.java` | Interface callbacks onTrainingStart/End |
| `TrainingProvider` history retrieval | `cmd/training/TrainingProvider.java` | Full class |
