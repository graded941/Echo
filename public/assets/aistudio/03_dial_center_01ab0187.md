# ECHO Implementation Spec — 03: Enhanced Watch Dial & Centre Customisation

**Pillar:** 3 of 8 | **Feasibility:** 🟢 HIGH | **Effort:** ~17 dev-days | **Priority:** P1

---

## 1. Overview

The itel ISW-O23 Storm supports 8 watch face slots and natively exposes `FEATURE_DIAL_CENTER` for centre complication customisation. ECHO extends this into a full **Dial Studio** — a drag-and-drop in-app editor with AI generation, complication binding, and a dynamic theme engine.

---

## 2. ISW-O23 Dial Hardware Facts

Confirmed from `WatchFaceUtils.java` and `ItelISWO23.java`:

| Property | Value | Source |
|---|---|---|
| Total dial slots | 8 | `WatchFaceUtils.O23_WATCH_FACE_COUNT = 8` |
| Screen shape | Rectangular | `ItelISWO23.isCircleDial() = false` |
| Corner radius | 26 px | `WatchFaceUtils.getScreenCornerRadius()` → O23 → 26 |
| Custom slot index | 7 | `ItelISWO23.dial_type_custom` |
| Binary slot index | 8 | `ItelISWO23.dial_type_bin` |
| In-watch slots | 1–6 | `ItelISWO23.dial_type_in_watch` |
| AI dial support | true | `ItelISWO23.isSupportAiDial() = true` |
| Dial switch support | true | `ItelISWO23.isSupportSwitchDial() = true` |
| Dial centre feature | Supported | `FEATURE_DIAL_CENTER` (0x1) in `itelO23TotalOperate` |

**Screen dimensions** (derived from MoYoung SDK photo face sizing): **390 × 450 px** rectangular AMOLED.

---

## 3. MoYoung Watch Face Protocol

### 3.1 SDK Classes Inherited

| Class | Role |
|---|---|
| `CustomWatchFaceFactory.create()` | Factory: returns `PhotoCustomWatchFaceProxy` or `DefaultCustomWatchFaceProxy` |
| `PhotoCustomWatchFaceProxy` | Sends photo-based custom face to slot 7 |
| `DefaultCustomWatchFaceProxy` | Sends default complication layout changes |
| `PhotoElementHelper` | Bitmap layer compositor for photo face |
| `WatchFaceLayoutInfo` | Layout descriptor with complication positions |
| `WatchFaceInfoProvider` | Queries current active face info from watch |
| `WatchFaceTransListener` / `CustomWatchFaceTransListener` | Transfer progress callbacks |
| `DisplayWatchFaceListener` | Receives watch-triggered face change events |

### 3.2 Custom Watch Face Push Flow

```kotlin
// Push a custom bitmap to ISW-O23 slot 7
val proxy: CustomWatchFaceProxy = CustomWatchFaceFactory.create()

// For photo-based face:
val photoProxy = proxy as PhotoCustomWatchFaceProxy
photoProxy.setBackground(backgroundBitmap)          // 390×450 user photo
photoProxy.setElements(layoutInfo)                   // complication positions
photoProxy.setTransListener(object : CustomWatchFaceTransListener {
    override fun onProgress(percent: Int) { /* update progress UI */ }
    override fun onSuccess() { /* face applied */ }
    override fun onFailed(code: Int) { /* handle error */ }
})
photoProxy.send()   // triggers GATT multi-packet transfer to ISW-O23
```

---

## 4. ECHO Dial Studio

### 4.1 Complication Types Supported

| Complication | Data Source | Update Interval |
|---|---|---|
| **Digital Time** | System clock | 1 second |
| **Analog Time hands** | System clock | 1 second |
| **Date** | System calendar | Daily |
| **Heart Rate** | `HeartRateChangeListener` | Real-time push |
| **Steps** | `StepsChangeListener` | Real-time push |
| **Calories** | `ThreeCircleEntity` | Hourly |
| **Battery (Watch)** | `BatteryChangeListener` | On change |
| **Weather** | `EchoWeatherService` | Hourly |
| **Music Now Playing** | `EchoMediaController` | On track change |
| **SpO₂** | `BloodOxygenChangeListener` | On measurement |
| **Stress** | `StressChangeListener` | On measurement |

### 4.2 DialStudioViewModel

```kotlin
// echo-dial/src/main/kotlin/com/echo/dial/studio/DialStudioViewModel.kt

data class DialComplication(
    val id: String,
    val type: ComplicationType,
    val x: Float,  // 0.0–1.0 relative position on 390px wide canvas
    val y: Float,  // 0.0–1.0 relative position on 450px tall canvas
    val width: Float,
    val height: Float,
    val fontFamily: String = "echo_sans",
    val fontSizeSp: Float = 14f,
    val colorHex: String = "#FFFFFF"
)

class DialStudioViewModel @HiltViewModel constructor(
    private val dialRepo: EchoDialRepository,
    private val hcReader: HealthConnectReader
) : ViewModel() {

    private val _complications = MutableStateFlow<List<DialComplication>>(defaultLayout())
    val complications: StateFlow<List<DialComplication>> = _complications

    private val _backgroundBitmap = MutableStateFlow<Bitmap?>(null)
    val backgroundBitmap: StateFlow<Bitmap?> = _backgroundBitmap

    fun setBackground(bitmap: Bitmap) {
        // Scale to 390×450
        val scaled = Bitmap.createScaledBitmap(bitmap, 390, 450, true)
        _backgroundBitmap.value = scaled
    }

    fun moveComplication(id: String, x: Float, y: Float) {
        _complications.update { list ->
            list.map { if (it.id == id) it.copy(x = x, y = y) else it }
        }
    }

    fun applyToWatch() {
        viewModelScope.launch {
            val bg = _backgroundBitmap.value ?: return@launch
            val layout = buildWatchFaceLayoutInfo(_complications.value)
            dialRepo.pushCustomFace(bg, layout)
        }
    }

    private fun defaultLayout(): List<DialComplication> = listOf(
        DialComplication("time", ComplicationType.DIGITAL_TIME, 0.5f, 0.35f, 0.6f, 0.2f),
        DialComplication("date", ComplicationType.DATE, 0.5f, 0.55f, 0.4f, 0.1f),
        DialComplication("hr", ComplicationType.HEART_RATE, 0.25f, 0.75f, 0.3f, 0.1f),
        DialComplication("steps", ComplicationType.STEPS, 0.75f, 0.75f, 0.3f, 0.1f)
    )
}
```

---

## 5. AI Dial Generator (Gemini Vision API)

Leverages `ItelISWO23.isSupportAiDial() = true` to push AI-styled faces to slot 7.

```kotlin
// echo-dial/src/main/kotlin/com/echo/dial/ai/GeminiDialGenerator.kt

class GeminiDialGenerator @Inject constructor(
    private val generativeModel: GenerativeModel  // com.google.ai.client.generativeai
) {
    suspend fun generateDialFace(
        userPhoto: Bitmap,
        style: DialStyle  // MINIMALIST, SPORTS, DARK_AMOLED, NATURE, RETRO
    ): Bitmap {
        val prompt = buildPrompt(style)
        val imagePart = content {
            image(userPhoto)
            text(prompt)
        }
        val response = generativeModel.generateContent(imagePart)
        // Parse base64 image from response
        val base64 = response.candidates.first().content.parts
            .filterIsInstance<ImagePart>().first().image
        return BitmapFactory.decodeByteArray(base64, 0, base64.size)
    }

    private fun buildPrompt(style: DialStyle): String = when (style) {
        DialStyle.MINIMALIST -> "Transform this into a minimalist smartwatch face background. " +
                "Rectangular 390x450px. Dark background, subtle textures only. No text, no UI."
        DialStyle.SPORTS -> "Transform this into an energetic sports smartwatch face. " +
                "Bold gradients, athletic feel. Rectangular 390x450px. No text."
        DialStyle.DARK_AMOLED -> "Create a pure black AMOLED smartwatch face from this image. " +
                "Deep blacks, glowing accent colours. 390x450px rectangular."
        DialStyle.NATURE -> "Artistic nature-themed smartwatch background from this image. " +
                "Painterly style. 390x450px rectangular. No UI elements."
        DialStyle.RETRO -> "Retro/vintage styled smartwatch face from this image. " +
                "Film grain, muted palette. 390x450px rectangular."
    }
}
```

---

## 6. Dynamic Theme Engine

Auto-switches watch face based on context:

```kotlin
// echo-dial/src/main/kotlin/com/echo/dial/theme/DynamicThemeEngine.kt

class DynamicThemeEngine @Inject constructor(
    private val dialRepo: EchoDialRepository,
    private val prefs: EchoPreferences
) {

    data class ThemeRule(
        val condition: ThemeCondition,
        val dialSlot: Int  // 1–8
    )

    enum class ThemeCondition {
        MORNING,         // 06:00–09:00
        WORK_HOURS,      // 09:00–18:00
        EVENING,         // 18:00–22:00
        SLEEP_MODE,      // 22:00–06:00
        ACTIVE_SPORT,    // sport session in progress
        HIGH_HR_ZONE,    // HR > 80% max
        DND_MODE         // DND active
    }

    fun evaluate(context: ThemeContext): Int {
        val rules = prefs.themeRules
        return rules.firstOrNull { it.condition.matches(context) }?.dialSlot
            ?: prefs.defaultDialSlot
    }

    suspend fun applyIfChanged(context: ThemeContext) {
        val targetSlot = evaluate(context)
        if (dialRepo.getCurrentSlot() != targetSlot) {
            dialRepo.switchToSlot(targetSlot)
        }
    }
}
```

---

## 7. Watch Face Marketplace (Slot 8 — Binary Faces)

Binary `.bin` watch faces are written to slot 8 (`dial_type_bin`):

```kotlin
class WatchFaceMarketplace @Inject constructor(
    private val apiClient: EchoMarketplaceApi,
    private val dialRepo: EchoDialRepository
) {
    suspend fun downloadAndInstall(faceId: String) {
        val binFile = apiClient.downloadFace(faceId)  // returns .bin file
        dialRepo.pushBinFace(binFile, slot = 8)
    }
}
```

Marketplace API (ECHO Cloud — Phase 2):
- `GET /v1/watchfaces?model=O23&page=1` → list of face entries with preview thumbnails
- `GET /v1/watchfaces/{id}/download` → `.bin` file stream

---

## 8. UI Screens

| Screen | Description |
|---|---|
| **DialGalleryScreen** | 8-slot grid showing current face previews; tap to preview full-screen |
| **DialStudioScreen** | Drag-and-drop canvas editor; layer panel; complication picker |
| **AIDialScreen** | Photo picker → style selector → AI preview → push to watch |
| **MarketplaceScreen** | Browse/search community faces; preview; install to slot 8 |
| **ThemeRulesScreen** | Configure auto-switch rules per time/activity/HR zone |

---

## 9. Evidence Log

| Claim | Source File | Evidence |
|---|---|---|
| O23 has 8 dial slots | `WatchFaceUtils.java` | `O23_WATCH_FACE_COUNT = 8` |
| O23 corner radius 26px | `WatchFaceUtils.java` | `return currentFirmwareVersion.startsWith(O23) ? 26 : 0` |
| `FEATURE_DIAL_CENTER` supported | `OperateFeature.itelO23TotalOperate` | Bitmask 0x1 |
| `isSupportAiDial()` = true | `ItelISWO23.java` | Method returning true |
| Custom slot = 7 | `ItelISWO23.java` | `dial_type_custom` field |
| `CustomWatchFaceFactory` / `PhotoCustomWatchFaceProxy` | `cmd/watchface/custom/*.java` | All 6 custom watchface classes present |
| `WatchFaceLayoutInfo` | `cmd/watchface/custom/WatchFaceLayoutInfo.java` | Complication layout descriptor |
