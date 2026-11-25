# Android Component Inventory, Security Findings & Permissions

### 5.1 Component Overview and Breakdown

The application registers 148 top-level Android components inside `manifest.xml`. Every component is explicitly cataloged and categorized by functional domain:

| Component Category | Total Count | Exported Count | Protected Count | Architectural Role |
| :--- | :--- | :--- | :--- | :--- |
| **Activities** | 94 | 4 | 90 | Native UI screens, sport viewports, permission gates, vendor cropping |
| **Services** | 26 | 3 | 23 | BLE telemetry, step counting, DFU firmware flashing, IoT card bind |
| **Broadcast Receivers** | 15 | 6 | 9 | Bluetooth power states, system time changes, boot completed, push triggers |
| **Content Providers** | 13 | 2 | 11 | Inter-process health data sharing, IoT desktop cards, file sharing |

### 5.2 Activities Inventory by Feature Domain

```
+-----------------------------------------------------------------------------------------+
|                                    ACTIVITIES (94)                                      |
+--------------------------+--------------------------------------------------------------+
| Domain / Module          | Primary Activity Classes                                     |
+--------------------------+--------------------------------------------------------------+
| Core & Navigation (8)    | SplashActivity, MainActivity, GuideActivity, GuideLoginActivity |
|                          | CustomFlutterActivity, DeviceFlutterActivity                 |
|                          | NativeFlutterFragmentActivity, PrivacyPolicyActivity         |
+--------------------------+--------------------------------------------------------------+
| Sport & Workout (24)     | SportActivity, SportIndoorActivity, SportOutdoorActivity     |
|                          | SportRecordListActivity, SportRecordDetailActivity           |
|                          | SportSettingsActivity, SportGoalActivity, SportPaceActivity  |
|                          | SportVoiceActivity, SportMapDownloadActivity, SportShare     |
|                          | SportGpsSignalActivity, SportHeartRateZoneActivity           |
+--------------------------+--------------------------------------------------------------+
| Device Subsystem (32)    | DeviceScanActivity, DevicePairActivity, DeviceSearchActivity |
|                          | DeviceSettingActivity, WatchDialMarketActivity               |
|                          | WatchDialCustomActivity, WatchDialDetailActivity             |
|                          | DeviceOtaActivity, DeviceAlarmActivity, DeviceContactActivity|
|                          | DeviceWorldClockActivity, DeviceNotificationAppActivity      |
|                          | DeviceCardSortActivity, DeviceRemindActivity                 |
+--------------------------+--------------------------------------------------------------+
| User & Account (14)      | MineProfileActivity, MinePersonalActivity, MineGoalActivity  |
|                          | MineUnitSettingActivity, MineAboutActivity, MineWebActivity  |
|                          | MineFeedbackActivity, ThirdPartyAuthActivity, StravaActivity |
+--------------------------+--------------------------------------------------------------+
| Media & Utility (16)     | UCropActivity, VideoTrimmerActivity, VideoToGifActivity      |
|                          | VideoToGif2Activity, CropImageActivity, DayActivity          |
|                          | WeekActivity, OnePixelKeepAliveActivity, XPermissionActivity |
+--------------------------+--------------------------------------------------------------+
```

### 5.3 Background Services Inventory

| Service Name | Exported | Process | Functional Purpose |
| :--- | :--- | :--- | :--- |
| `com.transsion.devices.service.DeviceBleService` | `false` | Main | Sustains continuous BLE connection, GATT reconnect loop, packet sync |
| `com.transsion.common.step.StepService` | `false` | Main | Hardware sensor step listener and daily accumulator |
| `com.realsil.sdk.dfu.DfuService` | `false` | `:dfu` | Realtek BLE Over-The-Air firmware updater |
| `com.topstep.fitcloud.sdk.v2.dfu.Dfu6621Service` | `false` | `:dfu` | TopStep / FitCloud Nordic/Sifli DFU engine |
| `com.transsion.welink.iotdevice.HealthDeviceStartService` | `true` | Main | Bound service for Transsion OS desktop smart card synchronization |
| `com.transsion.apiinvoke.ipc.connect.CommonRouterApiService` | `true` | `:ipc` | Cross-process API invocation bus for system launcher widgets |
| `androidx.work.impl.background.systemjob.SystemJobService` | `false` | Main | Android WorkManager scheduled background task executor |
| `com.google.firebase.sessions.SessionLifecycleService` | `false` | Main | Analytics session lifecycle tracker |

### 5.4 Content Providers & IPC Contracts

The application defines 13 content providers. Two providers are public IPC gateways exposing device health metrics to the Transsion OS ecosystem:

| Provider Class | Declared Authority | Exported | Required Permission | Functional Scope |
| :--- | :--- | :--- | :--- | :--- |
| `DataContentProvider` | `com.health.provider.ifit` | `true` | `transsion.permission.health` | Inter-app health queries (steps, goals, battery, biometric summary) |
| `HealthContentProvider`| `com.transsion.healthifit.iotCardProvider`| `true` | `com.transsion.iotcard.IOTCARD_PROVIDER` | Transsion OS Launcher IoT widget and card data provider |
| `FileProvider` | `com.transsion.healthifit` | `false` | None (GrantUri) | Secure internal file URI sharing (camera captures, APK updates) |
| `ScaffoldFileProvider` | `com.transsion.healthifit.yullg.fileprovider` | `false` | None (GrantUri) | Keep-alive scaffold internal file sharing |
| `PalmAccProvider` | `com.transsion.healthifit.PalmAccProvider` | `false` | Signature | Palm ID local account token caching |
| `ImagePickerFileProvider`| `com.transsion.healthifit.flutter.image_provider`| `false` | None (GrantUri) | Flutter image picker temporary capture cache |
| `ShareFileProvider` | `com.transsion.healthifit.flutter.share_provider`| `false` | None (GrantUri) | Social workout card image sharing provider |
| `FirebaseInitProvider` | `com.transsion.healthifit.firebaseinitprovider` | `false` | None | Early Firebase Core component auto-initialization |
| `InitializationProvider`| `com.transsion.healthifit.androidx-startup` | `false` | None | Jetpack Startup runtime initializer |
| `TrayContentProvider` | `com.transsion.healthifit.tray` | `false` | None | Multi-process SQLite key-value settings IPC provider |
| `InitProvider` | `com.transsion.healthifit.InitProvider` | `false` | None | Hoffnung TPMS tire pressure wearable SDK initializer |
| `ImageProvider` | `com.transsion.healthifit.imagepicker.provider` | `false` | None (GrantUri) | Native media picker file provider |

### 5.5 Permissions Audit & Threat Modeling

The manifest declares 112 uses-permission entries covering 54 unique Android permissions:

| Permission Identifier | Protection Level | Technical Necessity & Usage |
| :--- | :--- | :--- |
| `android.permission.BLUETOOTH_SCAN` | Dangerous (Runtime) | Discovers nearby BLE watches in Android 12+ (minSdk 32) |
| `android.permission.BLUETOOTH_CONNECT` | Dangerous (Runtime) | Sustains BLE GATT connection and notifications in Android 12+ |
| `android.permission.ACCESS_FINE_LOCATION` | Dangerous (Runtime) | Precise GPS tracking during outdoor runs; BLE scanning fallback |
| `android.permission.ACCESS_COARSE_LOCATION` | Dangerous (Runtime) | Network-based rough location for weather forecasting |
| `android.permission.ACCESS_BACKGROUND_LOCATION` | Dangerous (Runtime) | Sustained workout tracking while phone display is locked |
| `android.permission.BODY_SENSORS` | Dangerous (Runtime) | Reads continuous on-device heart rate & biometric sensor data |
| `android.permission.ACTIVITY_RECOGNITION` | Dangerous (Runtime) | Hardware step-sensor detection and motion classifier |
| `android.permission.POST_NOTIFICATIONS` | Dangerous (Runtime) | Android 13+ foreground service status notifications |
| `android.permission.READ_MEDIA_IMAGES` | Dangerous (Runtime) | Watch-face custom background photo selection in Android 13+ |
| `android.permission.CAMERA` | Dangerous (Runtime) | QR code watch pairing and watch remote camera shutter control |
| `android.permission.FOREGROUND_SERVICE` | Normal | Mandatory base permission for all foreground services |
| `android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE`| Normal | Android 14+ type for BLE smartwatch communication service |
| `android.permission.FOREGROUND_SERVICE_LOCATION` | Normal | Android 14+ type for continuous outdoor GPS tracking |
| `android.permission.FOREGROUND_SERVICE_HEALTH` | Normal | Android 14+ type for health telemetry processing service |
| `transsion.permission.health` | SignatureOrSystem | Guard for `DataContentProvider` IPC queries |
| `com.transsion.iotcard.IOTCARD_PROVIDER` | SignatureOrSystem | Guard for `HealthContentProvider` desktop widget IPC |

### 5.6 Security Findings & Hardening Recommendations

1. **ContentProvider IPC Protection:** Both `DataContentProvider` and `HealthContentProvider` are exported with custom permissions (`transsion.permission.health` and `com.transsion.iotcard.IOTCARD_PROVIDER`). In a clean-room rebuild intended for non-Transsion OS Android devices, these permissions must be configured as `normal` or guarded by a shared cryptographic signing key, else third-party launchers cannot read step cards.
2. **Cleartext Traffic Policy:** Manifest explicitly configures `android:usesCleartextTraffic="false"`, enforcing HTTPS/TLS across all network requests. All cloud mock endpoints must use TLS or override `network_security_config.xml` during development.
3. **OnePixelKeepAliveActivity Review:** The application includes `com.yullg.android.scaffold.core.OnePixelKeepAliveActivity` (a 1x1 transparent activity used for process immortality). Modern Android 14+ CDD guidelines penalize this pattern. The rebuild should deprecate this class in favor of modern standard `FOREGROUND_SERVICE_CONNECTED_DEVICE`.

---

### Evidence Log
- Manifest component totals and exported flags verified via `manifest.xml`: `manifest.xml:1-930`
- Content provider authorities and permissions verified via `manifest.xml:443-446, 537, 557`: `com.health.provider.ifit`, `com.transsion.healthifit.iotCardProvider`
- Background service processes verified via `manifest.xml:533, 903`: `DfuService`, `CommonRouterApiService`
- Permission list verified via `grep -n "uses-permission" manifest.xml`: `manifest.xml:15-125`
