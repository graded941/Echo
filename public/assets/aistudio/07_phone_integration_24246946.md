# ECHO Implementation Spec — 07: Phone Software Integration (itel P55 / Transsion OS)

**Pillar:** 7 of 8 | **Feasibility:** 🟡→🟢 HIGH | **Effort:** ~11 dev-days | **Priority:** P1

---

## 1. Overview

The itel P55 runs Transsion OS (Android 14 base) which includes Transsion-specific IPC infrastructure already imported by iPulse. ECHO leverages this for deep system integration: Gemini triggers, call control, camera remote, find device, and system health widget data.

---

## 2. Transsion OS IPC Infrastructure (Inherited from iPulse)

| Component | Package / Version | ECHO Use |
|---|---|---|
| **OxygenBus** | `com.transsion.oxygenbus 1.5.136` | Subscribe to Transsion system events (screen on/off, battery, settings changes) |
| **HubSDK** | `com.transsion.hub.*` | Register ECHO as a trusted health app; persist as foreground service whitelisted from battery saver |
| **DataContentProvider** | Authority: `com.health.provider.ifit` | Expose ECHO health metrics to Transsion OS health widgets |
| **transsion.permission.health** | Transsion OS signature permission | Read-only health data share with system (not required for ECHO as source) |
| **systemWidgetUtils** | `com.transsion.systemWidgetUtils` | Push daily step/HR summary to quick settings / lock screen widget |

---

## 3. OxygenBus Integration

```kotlin
// echo-transsion/src/main/kotlin/com/echo/transsion/OxygenBusClient.kt

class OxygenBusClient @Inject constructor(context: Context) {

    // OxygenBus 1.5.136 — Transsion IPC event bus
    // Events are delivered as Intent broadcasts on the OxygenBus channel
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                "com.transsion.action.SCREEN_ON" -> EventBus.post(TranssionScreenOnEvent())
                "com.transsion.action.SCREEN_OFF" -> EventBus.post(TranssionScreenOffEvent())
                "com.transsion.action.BATTERY_CHANGED" -> {
                    val level = intent.getIntExtra("battery_level", -1)
                    EventBus.post(PhoneBatteryEvent(level))
                }
                "com.transsion.action.SETTINGS_CHANGED" -> {
                    // Forward relevant settings changes to watch
                    val key = intent.getStringExtra("setting_key")
                    handleSettingsChange(key)
                }
            }
        }
    }

    fun register() {
        val filter = IntentFilter().apply {
            addAction("com.transsion.action.SCREEN_ON")
            addAction("com.transsion.action.SCREEN_OFF")
            addAction("com.transsion.action.BATTERY_CHANGED")
            addAction("com.transsion.action.SETTINGS_CHANGED")
        }
        // OxygenBus uses standard Android LocalBroadcastManager channel
        LocalBroadcastManager.getInstance(EchoApp.context).registerReceiver(receiver, filter)
    }

    private fun handleSettingsChange(key: String?) {
        when (key) {
            "time_format" -> EchoBleManager.syncTimeFormat()
            "language" -> EchoBleManager.syncLanguage()
        }
    }
}
```

---

## 4. HubSDK Integration — Foreground Service Whitelisting

```kotlin
// echo-transsion/src/main/kotlin/com/echo/transsion/HubSdkBridge.kt

class HubSdkBridge @Inject constructor(context: Context) {

    // HubSDK allows ECHO to register as a system-trusted health app,
    // exempting it from Transsion OS aggressive battery saver kills.
    fun register() {
        try {
            val hubIntent = Intent("com.transsion.hub.action.REGISTER_HEALTH_APP").apply {
                putExtra("app_package", "com.echo.wearable")
                putExtra("app_name", "ECHO")
                putExtra("foreground_service", true)
            }
            context.sendBroadcast(hubIntent)
        } catch (e: Exception) {
            // HubSDK not available on this firmware; fall back to standard foreground service
            startStandardForegroundService()
        }
    }

    private fun startStandardForegroundService() {
        val intent = Intent(EchoApp.context, EchoCoreService::class.java)
        ContextCompat.startForegroundService(EchoApp.context, intent)
    }
}
```

---

## 5. DataContentProvider — System Health Widget

```kotlin
// echo-transsion/src/main/kotlin/com/echo/transsion/EchoDataContentProvider.kt

// Extends the existing iPulse DataContentProvider (authority: com.health.provider.ifit)
// to surface ECHO-specific columns to Transsion OS widgets.

class EchoDataContentProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "com.health.provider.ifit"

        // Existing iPulse columns (kept for compatibility):
        const val COL_STEPS = "steps"
        const val COL_CALORIES = "calories"
        const val COL_HR = "heart_rate"

        // New ECHO columns:
        const val COL_BODY_BATTERY = "body_battery"
        const val COL_SPO2 = "spo2"
        const val COL_SLEEP_QUALITY = "sleep_quality"
        const val COL_STRESS = "stress_level"
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?,
                       selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
        val cursor = MatrixCursor(projection ?: arrayOf(COL_STEPS, COL_HR, COL_BODY_BATTERY))
        val repo = EchoHealthRepository.get()
        cursor.addRow(arrayOf(
            repo.getTodaySteps(),
            repo.getLatestHr(),
            repo.getBodyBattery()
        ))
        return cursor
    }

    override fun getType(uri: Uri) = "vnd.android.cursor.item/echo.health"
    override fun insert(uri: Uri, values: ContentValues?) = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?) = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?) = 0
    override fun onCreate() = true
}
```

---

## 6. Gemini Assistant Integration

### 6.1 Watch → Gemini Launch Flow

```kotlin
// echo-integrations/src/main/kotlin/com/echo/integrations/gemini/EchoIntentBridge.kt

class EchoIntentBridge @Inject constructor(
    private val context: Context,
    private val bleBridge: EchoBleManager
) {
    fun launchGemini() {
        // Standard Android assistant launch — no API key needed
        val intent = Intent(Intent.ACTION_ASSIST).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Gemini not installed — fallback to Google Search
            val searchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, "")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(searchIntent)
        }
    }

    // Map watch GATT command → phone action
    // Watch sends FEATURE_CAMERA_CONTROL command class with custom subcommand for Gemini
    fun onWatchCommand(command: EchoWatchCommand) {
        when (command) {
            EchoWatchCommand.GEMINI_TRIGGER -> launchGemini()
            EchoWatchCommand.CAMERA_SHUTTER -> triggerCamera()
            EchoWatchCommand.FIND_PHONE -> triggerFindPhone()
        }
    }

    fun triggerCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun triggerFindPhone() {
        // Flash torch + max volume ringtone for 5 seconds
        val vibrator = context.getSystemService(Vibrator::class.java)
        vibrator.vibrate(VibrationEffect.createWaveform(
            longArrayOf(0, 500, 200, 500, 200, 500), -1))
        // Flash camera torch
        val cameraManager = context.getSystemService(CameraManager::class.java)
        val cameraId = cameraManager.cameraIdList.firstOrNull()
        cameraId?.let { cameraManager.setTorchMode(it, true) }
        Handler(Looper.getMainLooper()).postDelayed({
            cameraId?.let { cameraManager.setTorchMode(it, false) }
        }, 5000L)
    }
}
```

### 6.2 Watch Shortcut Configuration

The ISW-O23 supports `FEATURE_CAMERA_CONTROL` (bitmask 0x400000) which maps to a physical button press or swipe gesture on the watch. ECHO repurposes one gesture slot as a Gemini trigger:

```
ISW-O23 camera button press
    └─► GATT CameraChangeListener.onCameraOpen() callback
            └─► EchoIntentBridge.onWatchCommand(GEMINI_TRIGGER)
                    └─► Intent(ACTION_ASSIST) → Gemini opens on phone
```

---

## 7. Phone Call Control

```kotlin
// echo-integrations/src/main/kotlin/com/echo/integrations/call/EchoCallController.kt

class EchoCallController @Inject constructor(
    private val context: Context
) {
    // Accept incoming call from watch button press
    fun acceptCall() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val telecomManager = context.getSystemService(TelecomManager::class.java)
            if (ActivityCompat.checkSelfPermission(context,
                    Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED) {
                telecomManager.acceptRingingCall()
            }
        } else {
            // Legacy: inject headset button key event
            val audioManager = context.getSystemService(AudioManager::class.java)
            val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HEADSETHOOK)
            val upEvent = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HEADSETHOOK)
            audioManager.dispatchMediaKeyEvent(downEvent)
            audioManager.dispatchMediaKeyEvent(upEvent)
        }
    }

    // Reject/end call
    fun rejectCall() {
        val telecomManager = context.getSystemService(TelecomManager::class.java)
        if (ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED) {
            telecomManager.endCall()
        }
    }
}
```

---

## 8. Find My Watch

Reverse direction — phone triggers watch vibration + screen wake:

```kotlin
fun triggerFindWatch() {
    // Sends GATT "find device" command (FEATURE_FIND_DEVICE = 0x8)
    bleBridge.sendFindDevice()
    // ISW-O23 responds with 3 strong vibrations + screen on (built into MoYoung protocol)
}
```

---

## 9. Contacts Sync

```kotlin
// Reuse existing ContactsSender from MoYoung SDK
// ContactsSender.java pushes up to FEATURE_CONTACT_NUM (0x100000 = 1 contact max per GATT session)

class EchoContactsSync @Inject constructor(
    private val context: Context,
    private val bleBridge: EchoBleManager
) {
    suspend fun syncTopContacts(limit: Int = 20) {
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection, null, null,
            ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED + " DESC LIMIT $limit"
        ) ?: return

        val contacts = mutableListOf<Pair<String, String>>()
        cursor.use {
            while (it.moveToNext()) {
                val name = it.getString(0) ?: continue
                val phone = it.getString(1) ?: continue
                contacts.add(name to phone)
            }
        }
        bleBridge.syncContacts(contacts)
    }
}
```

---

## 10. Required Permissions

```xml
<uses-permission android:name="android.permission.ANSWER_PHONE_CALLS"/>
<uses-permission android:name="android.permission.READ_CONTACTS"/>
<uses-permission android:name="android.permission.CAMERA"/>
<uses-permission android:name="android.permission.FLASHLIGHT"/>
<uses-permission android:name="android.permission.VIBRATE"/>
<uses-permission android:name="android.permission.CALL_PHONE"/>
```

---

## 11. Evidence Log

| Claim | Source File | Evidence |
|---|---|---|
| OxygenBus 1.5.136 imported in iPulse | `base/AndroidManifest.xml` dependencies | Library version string |
| HubSDK present | Decompiled source tree | `com.transsion.hub.*` package |
| `DataContentProvider` authority `com.health.provider.ifit` | `base/AndroidManifest.xml` | `<provider android:authorities="com.health.provider.ifit"` |
| `FEATURE_CAMERA_CONTROL` = 0x400000 | `OperateFeature.java` | Constant; `ItelISWO23.java` bitmask includes it |
| `FEATURE_FIND_DEVICE` = 0x8 | `OperateFeature.itelO23TotalOperate` | Bitmask entry |
| `FEATURE_CONTACT_NUM` = 0x100000 | `OperateFeature.itelO23TotalOperate` | Bitmask entry |
| `ContactsSender.java` exists | `cmd/contacts/ContactsSender.java` | File present in MoYoung SDK |
