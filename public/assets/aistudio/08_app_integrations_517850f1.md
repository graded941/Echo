# ECHO Implementation Spec — 08: Seamless App Integrations

**Pillar:** 8 of 8 | **Feasibility:** 🟢 HIGH | **Effort:** ~13 dev-days | **Priority:** P1

---

## 1. Overview

ECHO bridges the itel ISW-O23 with the full ecosystem of daily apps: YouTube Music (now-playing + control), Weather (OpenWeatherMap), Phone/SMS (TelecomManager + SmsManager — already inherited), and WhatsApp (NotificationListenerService — covered in Pillar 5). This spec covers the net-new integration code for each.

---

## 2. YouTube Music Integration

### 2.1 MediaSession Discovery

```kotlin
// echo-integrations/src/main/kotlin/com/echo/integrations/media/EchoMediaController.kt

@Singleton
class EchoMediaController @Inject constructor(
    context: Context,
    private val bleBridge: EchoBleManager,
    private val musicControlManager: MusicControlManager  // existing iPulse class
) {
    private val mediaSessionManager = context.getSystemService(MediaSessionManager::class.java)
    private var activeController: MediaController? = null

    fun attach() {
        mediaSessionManager.addOnActiveSessionsChangedListener(
            { controllers ->
                // Find YouTube Music session
                activeController = controllers?.firstOrNull { controller ->
                    controller.packageName in setOf(
                        "com.google.android.apps.youtube.music",
                        "com.google.android.youtube"
                    )
                } ?: controllers?.firstOrNull()  // fallback: any active media session

                activeController?.registerCallback(mediaCallback)
                // Send current track to watch immediately on connect
                activeController?.metadata?.let { onMetadataChanged(it) }
            },
            ComponentName(context, EchoNotificationService::class.java)
        )
    }

    private val mediaCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            metadata ?: return
            val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: return
            val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
            val albumArt = metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)

            // Send track info to watch
            val thumbBitmap = albumArt?.let {
                Bitmap.createScaledBitmap(it, 48, 48, true)  // small thumb for watch display
            }
            bleBridge.sendMusicInfo(title, artist, thumbBitmap)
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            val playing = state?.state == PlaybackState.STATE_PLAYING
            bleBridge.sendMusicState(playing)
        }
    }

    // --- Controls triggered from watch ---
    fun play() = activeController?.transportControls?.play()
    fun pause() = activeController?.transportControls?.pause()
    fun next() = activeController?.transportControls?.skipToNext()
    fun previous() = activeController?.transportControls?.skipToPrevious()
    fun volumeUp() = musicControlManager.volumeUp()    // existing MoYoung implementation
    fun volumeDown() = musicControlManager.volumeDown() // existing MoYoung implementation
}
```

### 2.2 Watch Music Screen Data Flow

```
User presses Play on ISW-O23
    └─► GATT music command → MoYoungBleManager callback
            └─► EchoMediaController.play()
                    └─► MediaController.TransportControls.play()
                              └─► YouTube Music resumes

Track changes in YouTube Music
    └─► MediaController.Callback.onMetadataChanged(metadata)
            └─► EchoMediaController.bleBridge.sendMusicInfo(title, artist)
                    └─► MessageBuilder.build("Music", "Song - Artist")
                              └─► GATT → ISW-O23 music now-playing screen
```

### 2.3 Manifest (NotificationListenerService binding needed)

The `MediaSessionManager.addOnActiveSessionsChangedListener()` requires the `NOTIFICATION_LISTENER_SERVICE` permission to be granted (same as `EchoNotificationService`). No additional permission needed if `EchoNotificationService` is already bound.

---

## 3. Weather Integration

### 3.1 OpenWeatherMap API Client

```kotlin
// echo-integrations/src/main/kotlin/com/echo/integrations/weather/EchoWeatherService.kt

class EchoWeatherService @Inject constructor(
    private val locationManager: LocationManager,
    private val httpClient: OkHttpClient,
    private val prefs: EchoPreferences,
    private val bleBridge: EchoBleManager
) {
    companion object {
        // OpenWeatherMap One Call API 3.0
        const val OWM_BASE = "https://api.openweathermap.org/data/3.0/onecall"
        const val API_KEY = BuildConfig.OWM_API_KEY  // stored in local.properties
    }

    suspend fun fetchAndSync() {
        val location = getLastLocation() ?: return
        val url = "$OWM_BASE?lat=${location.latitude}&lon=${location.longitude}" +
                "&appid=$API_KEY&units=metric&exclude=minutely,hourly,alerts"

        val request = Request.Builder().url(url).build()
        val response = httpClient.newCall(request).await()
        val owmData = Json.decodeFromString<OwmOneCallResponse>(response.body!!.string())

        // Build today's weather for watch
        val today = owmData.current
        val todayBean = WatchTodayWeatherBean().apply {
            city = getCity(location)
            temp = today.temp.roundToInt()
            weatherId = mapOwmCode(today.weather.first().id)
            highTemperature = owmData.daily.first().temp.max.roundToInt()
            lowTemperature = owmData.daily.first().temp.min.roundToInt()
        }

        // Build 7-day forecast
        val futureList = owmData.daily.drop(1).take(6).map { day ->
            WatchFutureWeatherBean().apply {
                weatherId = mapOwmCode(day.weather.first().id)
                lowTemperature = day.temp.min.roundToInt()
                highTemperature = day.temp.max.roundToInt()
            }
        }

        // Use existing WeatherBuilder to push to watch
        val tempFormat = if (prefs.useFahrenheit)
            TemperatureFormat.TEMPERATURE_FAHRENHEIT
        else TemperatureFormat.TEMPERATURE_CELSIUS

        val todayInfo = WeatherBuilder.build(todayBean, tempFormat)
        val futureInfo = WeatherBuilder.build(futureList, tempFormat)

        bleBridge.sendTodayWeather(todayInfo)
        bleBridge.sendFutureWeather(futureInfo)
    }

    // OpenWeatherMap 800-code range → iPulse 1–18 weather ID mapping
    private fun mapOwmCode(owmCode: Int): Int = when (owmCode) {
        in 200..299 -> 8   // Thunderstorm → Heavy rain
        in 300..399 -> 6   // Drizzle → Light rain
        in 500..504 -> 7   // Rain
        511 -> 10          // Freezing rain → Sleet
        in 520..531 -> 7   // Shower rain
        in 600..622 -> 11  // Snow
        in 700..799 -> 13  // Atmosphere (fog, mist, haze)
        800 -> 1           // Clear sky → Sunny
        801 -> 2           // Few clouds
        802 -> 3           // Scattered clouds
        in 803..804 -> 4   // Overcast
        else -> 4
    }

    private suspend fun getLastLocation(): Location? {
        return suspendCancellableCoroutine { cont ->
            if (ActivityCompat.checkSelfPermission(EchoApp.context,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                cont.resume(null)
                return@suspendCancellableCoroutine
            }
            locationManager.getCurrentLocation(
                LocationManager.FUSED_PROVIDER,
                null, Executors.newSingleThreadExecutor()
            ) { loc -> cont.resume(loc) }
        }
    }

    private fun getCity(location: Location): String {
        val geocoder = Geocoder(EchoApp.context, Locale.getDefault())
        return try {
            geocoder.getFromLocation(location.latitude, location.longitude, 1)
                ?.firstOrNull()?.locality ?: ""
        } catch (e: Exception) { "" }
    }
}
```

### 3.2 Weather Sync Schedule

```kotlin
// Sync weather every hour via WorkManager
val weatherWork = PeriodicWorkRequestBuilder<WeatherSyncWorker>(1, TimeUnit.HOURS)
    .setConstraints(Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build())
    .build()
WorkManager.getInstance(context)
    .enqueueUniquePeriodicWork("echo_weather_sync", ExistingPeriodicWorkPolicy.KEEP, weatherWork)
```

### 3.3 iPulse WeatherBuilder Reuse

The existing `WeatherBuilder.java` already handles:
- Today weather → `CRPTodayWeatherInfo` GATT packet
- Future 7-day → `CRPFutureWeatherInfo` GATT packet
- Celsius/Fahrenheit conversion via `TemperatureFormat` enum
- 18-type weather ID mapping → 7 CRP weather types

ECHO only adds the HTTP layer and the OWM→iPulse weather ID mapping function above.

---

## 4. Phone & Messages Integration

### 4.1 Outgoing Call from Watch

```kotlin
fun dialFromWatch(phoneNumber: String) {
    val intent = Intent(Intent.ACTION_CALL).apply {
        data = Uri.parse("tel:$phoneNumber")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    EchoApp.context.startActivity(intent)
}
```

### 4.2 SMS Read on Watch (Inbox Preview)

```kotlin
class EchoSmsReader @Inject constructor(context: Context) {
    fun getLatestSms(limit: Int = 5): List<SmsMessage> {
        val cursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
            "${Telephony.Sms.TYPE} = ${Telephony.Sms.MESSAGE_TYPE_INBOX}",
            null,
            "${Telephony.Sms.DATE} DESC LIMIT $limit"
        ) ?: return emptyList()

        return buildList {
            cursor.use {
                while (it.moveToNext()) {
                    add(SmsMessage(
                        address = it.getString(0),
                        body = it.getString(1).take(100),
                        date = it.getLong(2)
                    ))
                }
            }
        }
    }
}
```

### 4.3 Quick SMS Reply (Reuse iPulse)

`SMSUtils.sendSMS(address, text)` is fully implemented in iPulse's `SMSUtils.java`. ECHO calls it directly when the user selects a quick reply on the watch.

---

## 5. WhatsApp Integration Summary

WhatsApp is covered in depth in `impl/05_notifications.md`. Summary of integration points:

| Feature | Mechanism |
|---|---|
| Receive WhatsApp messages on watch | `EchoNotificationService` → `WhatsAppNotificationParser` → `MessageBuilder` → GATT |
| Quick reply to WhatsApp from watch | `QuickResponsesChangeListener` → `NotificationReply.sendReply()` → `RemoteInput` |
| WhatsApp call notification | Standard incoming call notification detected by `PhoneStateReceiver` |
| Group chat support | `WhatsAppNotificationParser` extracts group name + sender |

---

## 6. App Integration Settings UI

| Screen | Description |
|---|---|
| **MusicControlScreen** | Now playing widget; volume slider; previous/play/pause/next controls; Buds Neo 3 connection indicator |
| **WeatherSettingsScreen** | Temperature unit toggle (C/F); auto-refresh interval; city override; weather ID debug |
| **ConnectedAppsScreen** | Toggle per integration: YouTube Music, Strava, Weather, Gemini |
| **QuickActionsScreen** | Configure watch shortcut buttons: Gemini / Camera / Find Watch / Music |

---

## 7. Required Permissions

```xml
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
<uses-permission android:name="android.permission.READ_SMS"/>
<uses-permission android:name="android.permission.CALL_PHONE"/>
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.media_content_control"/>
```

> [!NOTE]
> `android.permission.media_content_control` is a system-level permission on Android 13+. The `MediaSessionManager.addOnActiveSessionsChangedListener()` alternative requires only `BIND_NOTIFICATION_LISTENER_SERVICE` (already granted for `EchoNotificationService`).

---

## 8. Evidence Log

| Claim | Source File | Evidence |
|---|---|---|
| `MusicControlManager` controls `AudioManager.STREAM_MUSIC` stream 3 | `MusicControlManager.java` | `audioManager.setStreamVolume(3, ...)` |
| `MusicControlManager.volumeUp()` and `volumeDown()` | `MusicControlManager.java` | Both methods present |
| `WeatherBuilder.build()` constructs `CRPTodayWeatherInfo` | `WeatherBuilder.java` | Full implementation |
| `WeatherBuilder` has 18 weatherId entries | `WeatherBuilder.java` | `getWeatherId()` switch cases 1–18 |
| `WeatherBuilder` Celsius/Fahrenheit conversion | `WeatherBuilder.java` | `ek1.d(i, 9, 5, 32)` = `i*9/5+32` |
| `SMSUtils.sendSMS()` exists | `SMSUtils.java` | Full implementation |
| `PhoneStateReceiver` captures incoming number | `PhoneStateReceiver.java` | `intent.getExtras().getString("incoming_number")` |
