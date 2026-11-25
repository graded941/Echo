# ECHO — Multi-Component Plan (MCP) Feasibility & Execution Analysis

> **Target Hardware Stack**
> - Smartwatch: **itel ISW-O23 Storm** (MoYoung/Sifli SF32LBxx, BLE 5.0, rectangular AMOLED)
> - Phone: **itel P55** (Transsion OS, Android 14-based)
> - Earbuds: **itel Buds Neo 3** (TWS, Bluetooth 5.3)
> - Foundation App: `com.transsion.healthifit` v5.9.1.17 (iPulse)

---

## 0. Executive Summary

ECHO is a full-stack personal health and device-integration companion app built on the decompiled and reconstructed iPulse codebase. It targets **8 capability pillars** organised across three execution tiers:

| Tier | Pillars | Overall Feasibility |
|------|---------|-------------------|
| **Green — Inherit & Enhance** | Fitness Tracking, Activity History, Dial Customisation, Notification Handling | HIGH — existing SDK coverage, days of adaptation |
| **Amber — Extend with New APIs** | Smart Alarms/DND, Phone Software Integration, App Integrations | MODERATE — standard Android APIs, 2–4 sprint effort |
| **Red — Net-New Engineering** | itel Buds Neo 3 Pairing | MODERATE-COMPLEX — BT profiles feasible; proprietary EQ unknown |

**Total estimated duration:** 16–20 engineering weeks (solo) / 7–9 weeks (2-person team)

---

## 1. Pillar 1 — Fitness Tracking (Google Fit parity)

### 1.1 Feasibility Rating: 🟢 HIGH

### 1.2 iPulse Foundation (What We Inherit)

| Source Class | Data Provided |
|---|---|
| `WatchHeartRateListEntity` | Timestamped HR list, interval, day classification |
| `WatchSleepListEntity` / `WatchNapListEntity` | Sleep stages (deep/light/REM), nap periods |
| `WatchBloodOxygenEntity` | SpO₂ percentages with timestamp |
| `WatchHrvEntity` | Heart Rate Variability (HRV) readings |
| `WatchPressureEntity` | Stress score (0–100 Transsion scale) |
| `ThreeCircleEntity` | Daily steps / calories / distance ring data |
| `StepEntity` | Per-hour step breakdown |
| `HeartRateChangeListener` | Real-time HR stream: `on24HourMeasureResult()`, `onMeasuring()`, `onOnceMeasureComplete()` |
| `StepsChangeListener` | Real-time step stream via `CRPStepInfo.getSteps()` |
| `BloodOxygenChangeListener` | Real-time SpO₂ callback |
| `HrvChangeListener` | Real-time HRV callback |
| `StressChangeListener` | Real-time stress level |

The ISW-O23 natively supports (from `OperateFeature.itelO23TotalOperate` bitmask):
- `FEATURE_HEART_RATE` (0x4)
- `FEATURE_BODY_TEMPERATURE` (0x4000)
- Steps, sleep, SpO₂, HRV, Stress (inferred from MoYoung SDK cmd packages)

### 1.3 Net-New ECHO Additions

| Addition | API / Method |
|---|---|
| **Health Connect write adapter** | `androidx.health.connect.client` — write `StepsRecord`, `HeartRateRecord`, `SleepSessionRecord`, `OxygenSaturationRecord`, `RestingHeartRateRecord`, `StressLevel` |
| **Google Fit legacy sync (optional)** | `com.google.android.gms.fitness.Fitness.getHistoryClient()` — `insertData()` |
| **Goal progress widgets** | Extend `ThreeCircleEntity` → `EchoGoalEntity` with weekly/monthly aggregates |
| **VO₂ Max estimation** | Derivation from sport HR + pace data (Cooper formula) |
| **Body battery / energy score** | Composite from HRV + sleep + stress (Garmin-style algorithm) |

### 1.4 Technical Approach

```
MoYoung BLE
    └─► HeartRateChangeListener.on24HourMeasureResult()
            └─► EchoHealthRepository.saveHr(WatchHeartRateBean)
                    ├─► Room DB: WatchHeartRateListEntity (existing)
                    └─► HealthConnectWriter.insertHeartRateRecord()
                              └─► Health Connect API (Android 14+)
```

Permissions needed: `android.permission.health.READ_HEART_RATE`, `WRITE_HEART_RATE`, `READ_STEPS`, `WRITE_STEPS`, `READ_SLEEP`, `WRITE_SLEEP`, `READ_OXYGEN_SATURATION`, `WRITE_OXYGEN_SATURATION`.

### 1.5 Effort Estimate

| Task | Days |
|---|---|
| Health Connect SDK integration & permission UI | 3 |
| Adapter: `WatchHeartRateBean` → `HeartRateRecord` | 1 |
| Adapter: `ThreeCircleEntity` → `StepsRecord` + `DistanceRecord` | 1 |
| Sleep/SpO₂/HRV/Stress adapters | 2 |
| Goal engine & weekly aggregation | 3 |
| VO₂ Max estimation module | 2 |
| Body battery composite score | 2 |
| **Total** | **~14 days** |

### 1.6 Risks

- Health Connect requires Android 9+ and Play Services — itel P55 (Transsion OS) confirmed to have Play Services.
- Google Fit is being deprecated in favour of Health Connect; target Health Connect as primary.
- Body temperature sensor accuracy varies; apply calibration offset.

---

## 2. Pillar 2 — Activity History & Monitoring (Strava parity)

### 2.1 Feasibility Rating: 🟢 HIGH

### 2.2 iPulse Foundation

| Source Class | Data Provided |
|---|---|
| `MotionRecordEntity` | Sport session record (type, start/end, calories, distance) |
| `WatchSportListEntity` | Per-session sport list with sport type enum |
| `SportLocationProvider.java` | Phone GPS path capture during outdoor sport |
| `GPSTrainingUtils.isGPSTraining(int)` | Identifies GPS sport type IDs 30–34 (outdoor run, ride, hike…) |
| `TrainingProvider.java` | Retrieves history training data from watch via BLE |
| `TrainingChangeListener` | Real-time sport event callbacks |
| `TrainingTypeConverter` | Maps MoYoung sport type ints ↔ display names |
| `EpoFileDownloader` / `EpoFileUploader` | Assisted GPS ephemeris data for faster fix |

### 2.3 Net-New ECHO Additions

| Addition | API / Method |
|---|---|
| **Strava OAuth 2.0 login** | `GET https://www.strava.com/oauth/authorize` → `POST /oauth/token` |
| **Strava activity upload** | `POST https://www.strava.com/api/v3/activities` with `name`, `sport_type`, `start_date_local`, `elapsed_time`, `distance` |
| **GPX file export** | Convert `sportPath` GPS coordinates → GPX 1.1 XML |
| **Strava route upload (GPX)** | `POST /api/v3/uploads` multipart with `data_type=gpx` |
| **Local activity map rendering** | Google Maps SDK or MapLibre GL (open-source) for route playback |
| **Pace / lap analysis** | Per-kilometre split calculation from GPS timestamps |
| **Social feed** | Strava Leaderboard and Kudos via API |

### 2.4 Technical Approach

```
GPSTrainingUtils.isGPSTraining(sportType) == true
    └─► SportLocationProvider captures lat/lng path
            └─► EchoActivityRepository.saveSession(MotionRecordEntity + path)
                    ├─► Local Room DB (existing MotionRecordEntity)
                    ├─► GPXExporter.export(path) → gpx file
                    └─► StravaUploader.upload(activity, gpx)
                              └─► Strava API v3
```

### 2.5 Effort Estimate

| Task | Days |
|---|---|
| Strava OAuth flow + token storage | 2 |
| Activity mapping (MotionRecordEntity → Strava fields) | 1 |
| GPX exporter | 2 |
| Strava activity + GPX upload | 2 |
| Map route visualisation (MapLibre) | 4 |
| Lap / pace analysis engine | 3 |
| Social feed & kudos UI | 3 |
| **Total** | **~17 days** |

### 2.6 Risks

- Strava API rate limits: 100 requests/15 min, 1000/day — cache aggressively.
- GPX accuracy depends on phone GPS during sport; ISW-O23 does not have onboard GPS.
- Transsion OS may have aggressive background process killing; use `WorkManager` for uploads.

---

## 3. Pillar 3 — Enhanced Dial Customisation

### 3.1 Feasibility Rating: 🟢 HIGH

### 3.2 iPulse Foundation

| Source Class | Data Provided |
|---|---|
| `WatchFaceUtils.getWatchFaceCount()` | O23 → returns 8 (8 total dial slots) |
| `WatchFaceUtils.getScreenCornerRadius()` | O23 → returns 26px corner radius |
| `ItelISWO23.isCircleDial()` | Returns `false` (rectangular face) |
| `ItelISWO23.isSupportAiDial()` | Returns `true` — AI dial supported |
| `ItelISWO23.isSupportSwitchDial()` | Returns `true` — runtime dial switching |
| `CustomWatchFaceFactory.create()` | Returns `PhotoCustomWatchFaceProxy` or `DefaultCustomWatchFaceProxy` |
| `PhotoElementHelper.java` | Photo composition for custom watch face |
| `WatchFaceLayoutInfo.java` | Layout descriptor for watchface complication slots |
| `WatchDialBean` / `WatchDialEntity` | Dial descriptor: slot index, type, preview bitmap |

**Dial slot map (ISW-O23 specific):**

| Slot | Constant | Purpose |
|---|---|---|
| 1–6 | `dial_type_in_watch` (1-indexed) | Pre-installed OEM watch faces |
| 7 | `dial_type_custom` | User-uploaded photo/custom face |
| 8 | `dial_type_bin` | Binary `.bin` firmware-bundled face |

`FEATURE_DIAL_CENTER` (flag `0x1`) in `itelO23TotalOperate` = **centre complication customisation is natively supported by the protocol**.

### 3.3 Net-New ECHO Additions

| Addition | Description |
|---|---|
| **ECHO Dial Studio** | Full in-app watch face builder: drag-and-drop complications (HR, steps, weather, music, battery) onto the rectangular canvas |
| **AI Dial Generator** | Use Gemini Vision API to style a photo into a watch face background (leverages `isSupportAiDial=true`) |
| **Complication editor** | Edit centre cluster: time widget font, HR widget, steps ring, date format |
| **Watch face marketplace** | Download community-created `.bin` faces (slot 8 path) |
| **Dynamic themes** | Auto-switch face based on time-of-day, activity, heart rate zone |

### 3.4 Technical Approach

```
ECHO Dial Studio (Android UI)
    ├─► WatchFaceLayoutInfo (complication layout descriptor)
    ├─► PhotoElementHelper (photo layer composition)
    └─► CustomWatchFaceFactory.create()
            └─► PhotoCustomWatchFaceProxy.sendToWatch(slot=7, bitmap)
                    └─► MoYoung GATT watchface transfer protocol
                              └─► ISW-O23 slot 7 written
```

For AI dial generation:
```
User photo → Gemini Vision API (style transfer prompt)
    → styled bitmap (rectangular, 390×450px)
    → PhotoCustomWatchFaceProxy.sendToWatch(slot=7, styledBitmap)
```

### 3.5 Effort Estimate

| Task | Days |
|---|---|
| Dial studio canvas (drag-drop complications) | 5 |
| Complication data binding (HR, steps, weather, battery) | 3 |
| AI dial generator (Gemini API integration) | 3 |
| Dynamic theme engine | 2 |
| Watch face marketplace (download + slot 8 write) | 4 |
| **Total** | **~17 days** |

---

## 4. Pillar 4 — itel Buds Neo 3 Pairing

### 4.1 Feasibility Rating: 🟡 MODERATE

### 4.2 iPulse Foundation

The existing iPulse / MoYoung SDK has **zero TWS earbuds management**. The only relevant code is:

- `MusicControlManager.java` — controls phone `AudioManager.STREAM_MUSIC` volume (stream type 3) from watch button presses. This is phone-side audio routing, not earbuds management.
- `BluetoothManager.java` (MoYoung) — manages watch BLE connection only.

### 4.3 Net-New ECHO Additions

| Feature | Android API |
|---|---|
| **Device discovery & pairing** | `CompanionDeviceManager.associate()` (API 26+) |
| **A2DP streaming detection** | `BluetoothProfile.A2DP` → `BluetoothA2dp.getConnectedDevices()` |
| **HFP call audio routing** | `BluetoothProfile.HEADSET` → `BluetoothHeadset` |
| **Battery level monitoring** | `BluetoothDevice.ACTION_BATTERY_LEVEL_CHANGED` (API 33+) or GATT `Battery Service (0x180F)` |
| **Media controls overlay** | Extend existing `MusicControlManager` with explicit `BluetoothA2dp` routing check |
| **Touch gesture config** (if supported) | Proprietary GATT service (requires Buds Neo 3 service UUID enumeration via BLE scan) |
| **EQ profile** (if supported) | Proprietary — requires reverse engineering Buds Neo 3 GATT profile OR checking itel SDK |

### 4.4 Feasibility Breakdown

| Feature | Confidence | Blocker |
|---|---|---|
| Discover & pair Buds Neo 3 | ✅ High | None — standard `CompanionDeviceManager` |
| A2DP connection status | ✅ High | None |
| Battery % display | ✅ High | GATT `0x180F` standard service |
| Call routing (HFP) | ✅ High | None |
| EQ profiles | ⚠️ Unknown | itel Buds Neo 3 may use proprietary GATT service; needs BLE sniffing |
| Touch gesture config | ⚠️ Unknown | Proprietary; requires APK reverse of itel earbuds companion app |

### 4.5 Technical Approach

```
EchoEarbudsManager
    ├─► CompanionDeviceManager.associate(request) → Buds Neo 3
    ├─► BluetoothProfileListener (A2DP, HFP state changes)
    ├─► BatteryGattClient.connect(device) → read 0x180F/0x2A19
    └─► UI: EarbudsCard (battery left/right, connection status)
```

For EQ/gestures (Phase 2):
```
BleGattScanner.scan(BudsNeo3MacAddress)
    └─► Enumerate all GATT services/characteristics
            └─► Map proprietary services → EchoEarbudsProtocol
```

### 4.6 Effort Estimate

| Task | Days |
|---|---|
| Companion device pairing flow | 2 |
| A2DP/HFP status monitoring | 2 |
| Battery GATT client | 2 |
| Earbuds settings UI card | 2 |
| EQ / gestures (Phase 2 — if feasible) | 5 |
| **Total Phase 1** | **~8 days** |

---

## 5. Pillar 5 — Notification Handling

### 5.1 Feasibility Rating: 🟢 HIGH

### 5.2 iPulse Foundation

| Source Class | Capability |
|---|---|
| `PhoneStateReceiver.java` | Listens for `android.intent.action.PHONE_STATE` broadcast; captures incoming call number |
| `SMSUtils.sendSMS()` | Send quick-reply SMS from watch via `SmsManager` |
| `cmd/message/MessageBuilder.java` | Builds MoYoung GATT notification packet |
| `cmd/quickresponses/QuickResponsesChangeListener.java` | Watch-side quick reply selection callback |
| `FEATURE_APP_REMINDER` (0x80) | Watch hardware supports app notification display |
| `FEATURE_CALL_REMINDER` (0x40) | Incoming call forwarding to watch |
| `FEATURE_MES_QUICK_REPLY` (0x100) | Watch quick-reply capability |
| `FEATURE_CONTACT_NUM` (0x100000) | Contacts sync for caller ID |

### 5.3 Net-New ECHO Additions

| Addition | Description |
|---|---|
| **EchoNotificationService** | Full `NotificationListenerService` with per-app filter, priority queue, and deduplication |
| **WhatsApp rich notifications** | Parse `com.whatsapp` / `com.whatsapp.w4b` notification extras; extract sender, message, chat group |
| **WhatsApp quick reply** | Use `Notification.WearableExtender` / `RemoteInput` to send reply without unlocking phone |
| **Notification filter UI** | Per-app toggle, keyword mute, Do Not Disturb schedule |
| **Priority escalation** | Missed call + WhatsApp message → watch haptic + escalated vibration pattern |

### 5.4 Technical Approach

```
EchoNotificationService extends NotificationListenerService
    ├─► onNotificationPosted(sbn)
    │       ├─► EchoNotificationFilter.shouldForward(packageName, priority)
    │       ├─► MessageBuilder.build(title, body, appIcon)
    │       └─► MoYoungBleManager → GATT notification packet → ISW-O23
    └─► onNotificationRemoved(sbn) → dismiss on watch
```

WhatsApp quick reply:
```
Watch sends quick reply selection (index N)
    └─► QuickResponsesChangeListener.onQuickResponseSelected(N)
            └─► NotificationReply.sendReply(remoteInput, text[N])
                    └─► Notification.WearableExtender reply action
```

### 5.5 Effort Estimate

| Task | Days |
|---|---|
| `EchoNotificationService` base | 2 |
| Per-app filter + priority UI | 3 |
| WhatsApp rich notification parser | 2 |
| Quick reply integration (`RemoteInput`) | 2 |
| Deduplication + rate limiting | 1 |
| **Total** | **~10 days** |

---

## 6. Pillar 6 — Smart Alarms & Do Not Disturb

### 6.1 Feasibility Rating: 🟡 MODERATE

### 6.2 iPulse Foundation

| Source Class | Capability |
|---|---|
| `AlarmBuilder.build(WatchAlarmBean)` | Converts alarm config to `CRPAlarmInfo` and sends to watch via GATT |
| `cmd/alarm/AlarmListener.java` | Receives alarm trigger events from watch |
| `WatchSleepListEntity` | Sleep stage data: deep / light / REM, timestamps |
| `WatchNapListEntity` | Nap session staging |
| `FEATURE_NO_DISTURB_MODEL` | Watch hardware DND mode flag (in feature bitmask) |
| `FEATURE_INTELLIGENT_CLOCK` | Smart wake alarm flag |

### 6.3 Net-New ECHO Additions

| Addition | Description |
|---|---|
| **Smart wake algorithm** | Analyse `WatchSleepListEntity` for light-sleep window within ±30 min of alarm time; trigger alarm at optimal moment |
| **DND scheduler** | Time-window based DND: sync to watch `FEATURE_NO_DISTURB_MODEL` GATT command + Android `NotificationManager.setInterruptionFilter()` |
| **Phone DND sync** | Mirror watch DND state ↔ phone Android DND (`INTERRUPTION_FILTER_NONE` / `_ALL`) |
| **Sunrise alarm** | Gradually increase screen brightness 30 min before alarm (Transsion OS `WindowManager.LayoutParams.screenBrightness`) |
| **Alarm analytics** | Track wake-up quality score over time; display sleep debt graph |

### 6.4 Smart Wake Algorithm

```
Input: target alarm time T, sleep data for night
Algorithm:
  1. Load WatchSleepListEntity for current night
  2. Find all segments where stage == LIGHT_SLEEP
  3. Filter segments ending in window [T-30min, T+5min]
  4. If found: set CRPAlarmInfo at earliest light-sleep exit point
  5. If not found: use T (fallback to standard alarm)
  6. Send AlarmBuilder.build(watchAlarmBean) → ISW-O23
```

Sleep stage values (from `WatchSleepListEntity` inspection):
- `0` = Awake
- `1` = Light sleep
- `2` = Deep sleep
- `3` = REM

### 6.5 Effort Estimate

| Task | Days |
|---|---|
| DND scheduler + phone sync | 2 |
| Smart wake algorithm | 3 |
| Alarm analytics & sleep debt graph | 3 |
| Sunrise alarm (brightness ramp) | 1 |
| **Total** | **~9 days** |

---

## 7. Pillar 7 — Phone Software Integration (itel P55)

### 7.1 Feasibility Rating: 🟡 MODERATE → 🟢 HIGH (component dependent)

### 7.2 iPulse Foundation (Transsion OS IPC)

| Source / Component | Capability |
|---|---|
| `OxygenBus 1.5.136` | Transsion inter-app IPC event bus (already imported in iPulse) |
| `HubSDK` | Bridges to Transsion OS system settings panel |
| `DataContentProvider` (`com.health.provider.ifit`) | Exposes iPulse health data to Transsion OS system widgets |
| `transsion.permission.health` | Transsion OS signature-level health data permission |
| `com.transsion.systemWidgetUtils` | System widget data feed |

### 7.3 Net-New ECHO Additions

| Feature | API | Feasibility |
|---|---|---|
| **Gemini Assistant trigger** | `Intent("android.intent.action.ASSIST")` + `ActivityResultContracts` | ✅ High |
| **Gemini from watch** | Watch button → GATT → `EchoIntentBridge` → Gemini intent | ✅ High |
| **Contacts sync** (call ID) | `ContactsContract.Contacts` + `ContactsSender.java` (existing) | ✅ High |
| **Phone call control** | `TelecomManager.acceptRingingCall()` / `endCall()` | ✅ High |
| **Camera remote shutter** | `FEATURE_CAMERA_CONTROL` (0x400000) + `MediaStore.ACTION_IMAGE_CAPTURE` | ✅ High |
| **Find phone** | `FEATURE_FIND_DEVICE` (0x8) → vibrate + flash phone | ✅ High |
| **System DND bridge** | `NotificationManager.setInterruptionFilter()` | ✅ High |
| **Transsion Health widget** | Extend `DataContentProvider` with ECHO-specific columns | 🟡 Moderate |

### 7.4 Gemini Integration Architecture

```
User raises wrist → Watch display "Gemini" shortcut
    └─► GATT custom command (using FEATURE_CAMERA_CONTROL slot or custom)
            └─► EchoIntentBridge.onWatchCommand(CMD_GEMINI)
                    └─► Intent("android.intent.action.ASSIST")
                              .addFlags(FLAG_ACTIVITY_NEW_TASK)
                            → Gemini app launches on phone
```

For voice response returned to watch (optional, Phase 2):
```
Google Assistant API → text response
    └─► MessageBuilder.build("Gemini", responseText)
            └─► GATT → ISW-O23 display
```

### 7.5 Effort Estimate

| Task | Days |
|---|---|
| Gemini intent bridge + watch shortcut | 2 |
| Call control (accept/reject/end) | 2 |
| Camera remote shutter | 1 |
| Find phone + find watch | 1 |
| Transsion Health widget extension | 3 |
| OxygenBus event subscriptions | 2 |
| **Total** | **~11 days** |

---

## 8. Pillar 8 — Seamless App Integrations

### 8.1 Feasibility Rating: 🟢 HIGH (all components)

### 8.2 YouTube Music

| Source | Capability |
|---|---|
| `MusicControlManager.java` (existing) | Volume up/down/set via `AudioManager.STREAM_MUSIC` stream 3 |
| Net-new: `MediaSessionManager` | Discover active `MediaSession` from YouTube Music |
| Net-new: `MediaControllerCompat` | Send play/pause/next/prev to YouTube Music media session |

```
Watch music button press
    └─► GATT command → MusicControlManager.volumeUp() (existing)
                     → EchoMediaController.sendCommand(PLAY/PAUSE/NEXT)
                              └─► MediaControllerCompat.getTransportControls()
                                        └─► YouTube Music media session
```

Track info display on watch:
```
MediaController.Callback.onMetadataChanged(metadata)
    └─► MessageBuilder.build(trackName, artist, albumArt thumbnail)
            └─► GATT → ISW-O23 music now-playing screen
```

### 8.3 Weather

| Source | Capability |
|---|---|
| `WeatherBuilder.java` (existing) | Builds `CRPTodayWeatherInfo` + `CRPFutureWeatherInfo` for watch |
| Net-new: OpenWeatherMap API v3 | `GET /data/3.0/onecall?lat=&lon=&appid=` for 7-day forecast |
| Net-new: `LocationManager` | Get current GPS coordinates for weather lookup |

The existing `WeatherBuilder` maps 18 weather IDs (iPulse enum) → 7 CRP weather types (sunny/cloudy/rain/snow/fog/thunder/windy). ECHO extends this to full OpenWeatherMap 800–code mapping.

### 8.4 Messages (SMS & Calls)

| Source | Capability |
|---|---|
| `PhoneStateReceiver.java` (existing) | Incoming call number detection via `PHONE_STATE` broadcast |
| `SMSUtils.sendSMS()` (existing) | Quick-reply SMS send |
| `FEATURE_CALL_REMINDER` / `FEATURE_MES_QUICK_REPLY` | Watch hardware supports call + SMS display |
| Net-new: `TelecomManager` | Accept/reject incoming call from watch |

### 8.5 WhatsApp Integration

WhatsApp notifications carry standard Android notification extras. ECHO parses:
- `android.title` → sender name
- `android.text` → message body
- `android.people` → contact URI (for avatar lookup)
- `android.remoteInputHistory` → conversation history
- `RemoteInput` extras → reply action key

Package filter: `com.whatsapp`, `com.whatsapp.w4b` (WhatsApp Business)

### 8.6 Effort Estimate

| Task | Days |
|---|---|
| `MediaSessionManager` integration + YouTube Music | 3 |
| Track metadata display on watch | 2 |
| OpenWeatherMap integration + extended weather mapping | 3 |
| Call accept/reject from watch | 2 |
| WhatsApp parser + reply | 3 |
| **Total** | **~13 days** |

---

## 9. Consolidated Effort Summary

| Pillar | Effort (days) | Feasibility | Priority |
|---|---|---|---|
| 1. Fitness Tracking (Health Connect) | 14 | 🟢 HIGH | P1 |
| 2. Activity History (Strava) | 17 | 🟢 HIGH | P1 |
| 3. Dial Customisation | 17 | 🟢 HIGH | P1 |
| 4. itel Buds Neo 3 | 8 (Phase 1) | 🟡 MODERATE | P2 |
| 5. Notification Handling | 10 | 🟢 HIGH | P1 |
| 6. Smart Alarms / DND | 9 | 🟡 MODERATE | P2 |
| 7. Phone Integration (itel P55) | 11 | 🟡→🟢 | P1 |
| 8. App Integrations | 13 | 🟢 HIGH | P1 |
| **Total** | **~99 dev-days** | | |

At 5 days/week: **~20 weeks solo / ~10 weeks 2-person**

---

## 10. Blockers & Open Questions

| # | Blocker | Mitigation |
|---|---|---|
| B-1 | itel Buds Neo 3 proprietary GATT profile unknown | BLE sniff pairing session with `nRF Sniffer` or enumerate services via `EchoGattScanner` at runtime |
| B-2 | Transsion OS signature permission for `transsion.permission.health` | Use OxygenBus IPC (no signature needed) + DataContentProvider read-path |
| B-3 | Strava API rate limits | Batch uploads via `WorkManager`, cache on device |
| B-4 | itel P55 aggressive background process kill (Transsion OS battery saver) | Register as system health app via `HubSDK`; use foreground service |
| B-5 | Gemini voice API availability | Use `ACTION_ASSIST` intent (no API key needed); full SDK is optional |
| B-6 | ISW-O23 firmware OTA compatibility | Pin to tested firmware version; `FEATURE_FIRMWARE_CHECK` gate OTA |

---

## 11. Architecture Decision Records (ADR)

### ADR-001: Health Connect over Google Fit
**Decision:** Use `androidx.health.connect.client` as primary health data sink.
**Rationale:** Google Fit deprecated May 2024; Health Connect is the platform standard on Android 9+. itel P55 (Android 14) fully supports it.

### ADR-002: MoYoung SDK retained as BLE transport
**Decision:** Keep all MoYoung GATT framing; build ECHO layers above `MoYoungBleManager`.
**Rationale:** Protocol reverse-engineering cost > 30 days. MoYoung SDK is a stable, complete implementation for ISW-O23.

### ADR-003: OpenWeatherMap over WeatherContract
**Decision:** Use OpenWeatherMap API v3 for weather data.
**Rationale:** Android `WeatherContract` is not public; Transsion OS weather provider is proprietary. OpenWeatherMap free tier (1000 calls/day) sufficient for personal use.

### ADR-004: CompanionDeviceManager for Buds Neo 3
**Decision:** Use `CompanionDeviceManager` (not raw `BluetoothAdapter` scan) for earbuds association.
**Rationale:** CDM provides persistent pairing across reboots, works with Android 14 BT permission model, and handles background BT scan without `BLUETOOTH_SCAN` permission window.

---

*Document generated from decompiled artifacts of `com.transsion.healthifit 5.9.1.17`. All class references verified against `decompiled/sources/` tree.*
*Evidence sources: `ItelISWO23.java`, `OperateFeature.java`, `MoYoungBleManager.java`, `MusicControlManager.java`, `PhoneStateReceiver.java`, `AlarmBuilder.java`, `WatchFaceUtils.java`, `WeatherBuilder.java`, `HeartRateChangeListener.java`, `StepsChangeListener.java`, `GPSTrainingUtils.java`*
