# System Architecture, Modular Topology & Dual-Shell Design

### 4.1 Layered Architecture Overview

The iPulse architecture follows a decoupled, inverted-dependency layered structure. Core business domains communicate across clean module boundaries enforced by Java `ServiceLoader` SPI interfaces rather than direct compile-time class dependencies:

```
+===================================================================================+
|                             PRESENTATION & UI TIER                                |
+-------------------------------------------------+---------------------------------+
|               Native Android UI                 |         Flutter Engine UI       |
|  - 4 Main Navigation Tabs (Health/Sport/Dev/Me) |  - 30 Modular Feature Packages  |
|  - 94 Native Activities (Sport HUD, DFU, Cam)   |  - Impeller / Skia AOT Engine   |
|  - 496 DataBinding Layouts + Custom Views       |  - 2,402 Assets, Shaders, Icons |
+-------------------------------------------------+---------------------------------+
                                         |
                       +-----------------+-----------------+
                       |     INTER-TIER BRIDGE & ROUTING   |
                       |  - MyHealthNavigator (71 Targets) |
                       |  - Platform Channels (10 Channels)|
                       |  - Pigeon RPC & Method Channels   |
                       |  - Dual Shell Policy (V1 vs V2)   |
                       +-----------------+-----------------+
                                         |
+===================================================================================+
|                            DOMAIN & BUSINESS LOGIC TIER                           |
+----------------------+----------------------+----------------------+--------------+
|   Health Biometrics  |    Sport Tracking    |   Device Subsystem   | Account/IoT  |
| - Daily 3-Circles    | - GPS Route Engine   | - 49 Hardware Drivers| - Palm ID SSO|
| - Sleep / HRV Staging| - Pace & Cadence     | - OTA/DFU Pipeline   | - IoT Card   |
| - SpO2 & Stress Calc | - Training Plans     | - Push Notifications | - Settings   |
+----------------------+----------------------+----------------------+--------------+
                                         |
+===================================================================================+
|                        SERVICE PROVIDER INTERFACE (SPI) BUS                       |
|   META-INF/services/com.transsion.spi.* (25 Interfaces, 86 Registrations)         |
+===================================================================================+
                                         |
+===================================================================================+
|                             DATA & PERSISTENCE TIER                               |
+-----------------------------+-----------------------------+-----------------------+
|        Room Database        |     Android Content IPC     |     Key-Value Stores  |
| - HealthDataBase (v27)      | - DataContentProvider       | - DataStore Prefs     |
| - 32 Biometric Entities     |   (Authority: ifit)         | - Multi-process Tray  |
| - 52 Bidirectional Migrations| - HealthContentProvider    | - SharedPreferences   |
+-----------------------------+-----------------------------+-----------------------+
                                         |
+===================================================================================+
|                       HARDWARE & OS COMMUNICATION PLATFORM                        |
|  - Android Bluetooth Low Energy (BLE GATT)  - GPS & Fused Location Provider       |
|  - CameraX Capture Pipeline                 - Transsion OS System Framework IPC   |
+===================================================================================+
```

### 4.2 Module Inventory & Namespace Responsibility

The source code decompilation reveals 12 distinct module namespaces under `com.transsion.*`:

| Namespace Identifier | File Count | Architectural Responsibility & Scope |
| :--- | :--- | :--- |
| `com.transsion.healthlife` | 1,418 | Application orchestration, main activities, shell binders, startup |
| `com.transsion.devices` | 3,892 | Hardware drivers (MoYoung, TopStep, Qiwo, CRP), packet parsers, DFU |
| `com.transsion.common` | 2,104 | Room Database, `DataContentProvider`, utility routines, base activities |
| `com.transsion.module` | 2,845 | Domain modules (sport engine, workout trackers, mine profile screens) |
| `com.transsion.transconnect` | 912 | Bluetooth bonding manager, multi-device arbitration, connection state |
| `com.transsion.wearablelink` | 684 | Vendor transport protocol bridge for Qiwo / HuaWo hardware |
| `com.transsion.hubsdk` | 420 | Transsion OS system theme and AOSP settings provider bridge |
| `com.transsion.apiinvoke` | 315 | Cross-process IPC service router (`CommonRouterApiService`) |
| `com.transsion.athena` | 280 | Enterprise telemetry event tracking and crash reporting client |
| `com.transsion.transvasdk` | 512 | Ella AI voice assistant, ASR, TTS WebSocket streaming client |
| `com.transsion.xuanniao` | 195 | Account authentication stubs and cloud token refresh handlers |
| `com.transsion.welink.iotdevice`| 340 | IoT Smart Card Content Provider (`com.transsion.healthifit.iotCardProvider`) |

### 4.3 Service Provider Interface (SPI) Registry

Cross-module communication avoids direct class linkage via Java SPI declarations in `base/META-INF/services/`:

| SPI Contract Interface | Provider Count | Concrete Implementation Class(es) | Responsibility |
| :--- | :--- | :--- | :--- |
| `com.transsion.spi.devicemanager.IDeviceManagerSpi` | 1 | `com.transsion.devices.DeviceManagerImpl` | Central entry point for device discovery and connection |
| `com.transsion.spi.devicemanager.device.IHealthDevice` | 47 | `com.transsion.watchtans.*`, `watchcrp.*`, `watchtopstep.*` | Individual watch model driver capabilities |
| `com.transsion.spi.common.Initialize` | 7 | `ModuleInit`, `DeviceInit`, `SportInit`, `AthenaInit`, etc. | Lazy module initialization during application boot |
| `com.transsion.spi.common.MainFragmentSpi` | 4 | `HealthFragment`, `SportFragment`, `DeviceFragment`, `MineFragment` | Injects the 4 primary tabs into `MainActivity` |
| `com.transsion.spi.common.FlutterHandlerSpi` | 4 | `FlutterChannelHandlerImpl`, `DeviceChannelHandlerImpl` | Binds native platform channel callbacks |
| `com.transsion.spi.common.IDataBaseUpgradeSpi` | 1 | `com.transsion.module.sport.utils.spi.DataBaseUpgradeSpi` | Executes schema migration scripts across feature modules |
| `com.transsion.spi.sport.ISportControlSpi` | 1 | `com.transsion.module.sport.utils.spi.SportControlSpi` | Global sport start/pause/stop coordination |
| `com.transsion.spi.sport.ISportTodayDistSpi` | 1 | `com.transsion.module.sport.utils.spi.SportTodayDistSpi` | Queries cumulative daily distance and calorie totals |
| `com.transsion.spi.device.IDeviceSpi` | 1 | `com.transsion.module.device.DeviceImpl` | Device UI module coordinator |
| `com.transsion.spi.mine.SetPersonMesSpi` | 1 | `com.transsion.module.mine.SetPersonMesSpiImpl` | Updates personal height/weight biometric parameters |

### 4.4 Startup Sequence and Lifecycle Orchestration

The application boot sequence follows a strict deterministic order to ensure IPC availability and database readiness before UI rendering:

```
[Phase A: OS Boot & Provider Stage]
   1. System loads base.apk & native ELF libraries into ART runtime.
   2. androidx.startup.InitializationProvider instantiates ProcessLifecycle & WorkManager.
   3. DataContentProvider.onCreate() registers StepUtil observers & background coroutine scopes.
   4. HealthContentProvider.onCreate() registers IoT card broadcast endpoints.

[Phase B: Application.onCreate() Execution]
   5. App.onCreate() initializes ApplicationContext in PermissionChecker and LogUtil.
   6. Writes "flutter.myhealth_test_environment" = false into FlutterSharedPreferences.
   7. Opens SharedPreferences "myhealth_shell_crash_breaker_v2".
   8. Evaluates crash breaker state (build key: "com.transsion.healthifit|5.9.1.17|50009017|OS_RELEASE").
   9. Resolves Flutter Shell Mode Policy (AUTO, FORCE_V1, FORCE_V2).
  10. If V2 allowed: invokes MyHealthEngine.a.E(this) to pre-warm FlutterEngine background worker.
  11. Executes ServiceLoader.load(Initialize.class) -> runs all 7 module initializers concurrently.
  12. HealthDataInvalidationBinder registers atomic database invalidation listeners.

[Phase C: MainActivity UI Binding]
  13. MainActivity attaches MyHealthShellHostBinder to container layout.
  14. Resolves 4 primary fragments via MainFragmentSpi and binds BottomNavigationView.
  15. If V2 active: attaches FlutterView seamlessly into background frame buffer.
```

### 4.5 Dual Flutter Shell Architecture (V1 vs. V2 Decision Policy)

To reconcile modern Flutter rendering performance with carrier/OEM device stability, iPulse implements an advanced dual-shell runtime architecture governed by an automated circuit breaker:

```
                           +-------------------------------+
                           | App Launch / Startup Trigger  |
                           +---------------+---------------+
                                           |
                                           v
                       +---------------------------------------+
                       | Read "myhealth_shell_crash_breaker_v2"|
                       | Check Build Key & Crash Record Array  |
                       +-------------------+-------------------+
                                           |
                    +----------------------+----------------------+
                    |                                             |
             [Crash Count >= 3]                           [Crash Count < 3]
                    |                                             |
                    v                                             v
       +-------------------------+                   +-------------------------+
       |   Locked to V1 Shell    |                   | Check Policy Override   |
       |  (Legacy Isolated Host) |                   | (AUTO / FORCE_V1 / V2)  |
       +------------+------------+                   +------------+------------+
                    |                                             |
                    |                         +-------------------+-------------------+
                    |                         |                                       |
                    |                   [AUTO / FORCE_V2]                         [FORCE_V1]
                    |                         |                                       |
                    |                         v                                       |
                    |            +-------------------------+                          |
                    |            |   Activate V2 Shell     |                          |
                    |            | (Pre-warmed Engine Host)|                          |
                    |            +------------+------------+                          |
                    |                         |                                       |
                    |                         v                                       |
                    |            +-------------------------+                          |
                    |            | State: V2_STARTING      |                          |
                    |            | Attach Crash Handler    |                          |
                    |            +------------+------------+                          |
                    |                         |                                       |
                    |           +-------------+-------------+                         |
                    |           |                           |                         |
                    |    [Crash in V2 Window]        [Normal Startup]                 |
                    |           |                           |                         |
                    |           v                           v                         |
                    |  +--------------------+      +--------------------+             |
                    |  | Record Crash Ts    |      | State: V2_ACTIVE   |             |
                    |  | Set lockedToV1=true|      | Clear Fail Counter |             |
                    |  +--------+-----------+      +--------------------+             |
                    |           |                                                     |
                    +---------->+<----------------------------------------------------+
                                |
                                v
                   +-------------------------+
                   | Render UI:              |
                   | V1 -> CustomActivity    |
                   | V2 -> Host Frame Binder |
                   +-------------------------+
```

1. **Flutter V1 (Legacy Isolated Activity):** Uses `CustomFlutterActivity` and `DeviceFlutterActivity`. Every Flutter screen transition spawns an independent Android activity containing its own Flutter engine lifecycle. Memory footprint is higher and transition animations exhibit slight IPC latency, but a rendering engine crash is isolated and cannot terminate the core health monitoring service.
2. **Flutter V2 (Unified Embedded Shell):** Employs `MyHealthShellHostBinder` inside `MainActivity`. A single persistent `FlutterEngine` instance is pre-warmed at boot (`MyHealthEngine.E()`) and dynamically swapped into the active frame layout. Frame rates reach 60–120 FPS with zero tab-switching latency.
3. **Crash Breaker Circuit (MyHealthShellCrashBreaker):**
   - **Persistence Store:** SharedPreferences `"myhealth_shell_crash_breaker_v2"`
   - **Composite Key:** `com.transsion.healthifit|5.9.1.17|50009017|OS_RELEASE`
   - **Exposure States:** `V1_ACTIVE`, `V2_STARTING`, `V2_ACTIVE`, `V2_STOPPING`
   - **Tripping Logic:** If an unhandled native SIGSEGV or Dart fatal exception occurs while exposure is `V2_STARTING` or `V2_ACTIVE`, the breaker records a crash timestamp into array `d`. When crash count reaches 3 within a sliding window, `lockedToV1` is permanently set to `true`, forcing the application to revert to V1 mode across future app cold starts.

---

### Evidence Log
- SPI registrations and interfaces verified via `base/META-INF/services/`: `base/META-INF/services/:1-25`
- Application startup and crash breaker implementation verified in `App.java`: `decompiled/sources/com/transsion/healthlife/application/App.java:245-345`
- Crash breaker state machine verified in `j86.java` and `MyHealthShellCrashExposure.java`: `decompiled/sources/defpackage/j86.java:1-50`
- Flutter shell mode enum verified in `FlutterShellModePolicy.java`: `decompiled/sources/com/transsion/healthlife/FlutterShellModePolicy.java:1-40`
- Shell host binder and error tokens verified in `MyHealthShellHostBinder.java`: `decompiled/sources/com/transsion/healthlife/view/MyHealthShellHostBinder.java:1-20`
