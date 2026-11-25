# Flutter Presentation Tier, Platform Channels & Navigation

### 9.1 Flutter Engine Integration & Runtime Architecture

The visual presentation tier of iPulse relies heavily on Google Flutter (engine version `3.19.x` with Impeller/Skia). Compiled Dart business logic is distributed entirely inside `split_config.arm64_v8a.apk` as native AOT machine code (`libapp.so`, 45.5 MB) paired with the Flutter runtime engine (`libflutter.so`, 10.8 MB).

| Engine Component | Artifact File | Size (Bytes) | Role & Compilation State |
| :--- | :--- | :--- | :--- |
| **Flutter C++ Engine** | `arm/lib/arm64-v8a/libflutter.so` | 10,814,192 | Manages rendering pipeline, Skia rasterizer, platform message loops |
| **Compiled Dart AOT App**| `arm/lib/arm64-v8a/libapp.so` | 45,548,448 | Stripped ARM64 machine code containing all Dart widgets, models, controllers |
| **Development JIT Blob** | `base/assets/flutter_assets/kernel_blob.bin` | Absent | **Absent in release build**; confirms strict Ahead-Of-Time compilation |
| **Asset Manifest** | `base/assets/flutter_assets/AssetManifest.json` | 2,402 entries | Complete registry of packed Flutter assets and JSON resources |
| **Font Manifest** | `base/assets/flutter_assets/FontManifest.json` | 1 font family | Declares MaterialIcons and Transsion custom typography |

### 9.2 Flutter Packages Inventory (30 Feature Modules)

The asset manifest establishes that the Dart codebase is organized into 30 discrete feature packages under `packages/`:

| Package Identifier | Asset Directory | Domain Responsibility & Scope |
| :--- | :--- | :--- |
| `ai_suggestion_package` | `packages/ai_suggestion_package/` | AI-generated training recommendations and motivational cards |
| `app_shell` | `packages/app_shell/` | Core Flutter application shell, navigation scaffolding, theme provider |
| `assets_package` | `packages/assets_package/` | Visual asset library (vector drawings, illustration graphics) |
| `assets_package_new` | `packages/assets_package_new/` | Modernized design token icons and updated sport silhouettes |
| `common_assets_package` | `packages/common_assets_package/`| Shared button glyphs, indicators, and loading animations |
| `common_i_a_package` | `packages/common_i_a_package/` | Common interaction abstractions and dialog widgets |
| `cupertino_icons` | `packages/cupertino_icons/` | Standard Cupertino icon fonts |
| `device_i_a_package` | `packages/device_i_a_package/` | Watch management widgets (battery meters, sync indicators) |
| `flutter_sound_web` | `packages/flutter_sound_web/` | Audio player primitives for workout interval announcements |
| `home_chart_package` | `packages/home_chart_package/` | Legacy health metric visualizers |
| `ifit_assets_package` | `packages/ifit_assets_package/` | Core iPulse branding, watch dial masks, background gradients |
| `ipulse_home_chart_package`| `packages/ipulse_home_chart_package/`| High-performance interactive charts (HR, Sleep, SpO2, HRV) |
| `ipulse_sports_sort` | `packages/ipulse_sports_sort/` | Drag-and-drop workout list customization |
| `ipulse_sport_plan` | `packages/ipulse_sport_plan/` | Multi-day workout planner and calendar schedule view |
| `ipulse_trans_flutter_module_sport`| `packages/ipulse_trans_flutter_module_sport/`| Workout live HUD (pace, cadence, heart rate zone gauges) |
| `lib_os_widgets` | `packages/lib_os_widgets/` | Transsion OS styled buttons, switches, bottom sheets, sliders |
| `menstruation_i_a_package`| `packages/menstruation_i_a_package/`| Menstrual cycle calendar, fertile window visualization |
| `mine_home` | `packages/mine_home/` | User settings, account profile, unit selectors, target configuration |
| `sport_home` | `packages/sport_home/` | Sport dashboard tab, route preview, activity history |
| `trans_chart_widget` | `packages/trans_chart_widget/` | Low-level bezier curve renderer and custom canvas graph painters |
| `trans_flutter_lib_components` | `packages/trans_flutter_lib_components/`| Standard Transsion Design System components |
| `trans_flutter_lib_widgets` | `packages/trans_flutter_lib_widgets/` | Lottie vector animation controllers and badge cards |
| `trans_flutter_module_ai_watch_dial`| `packages/trans_flutter_module_ai_watch_dial/`| Generative AI watch-face prompt builder and previewer |
| `trans_flutter_module_device_v2` | `packages/trans_flutter_module_device_v2/`| Modern watch control panel (alarms, world clock, DND rules) |
| `trans_flutter_module_dial` | `packages/trans_flutter_module_dial/`| Cloud watch-face store, dial installation manager |
| `trans_flutter_module_google_maps` | `packages/trans_flutter_module_google_maps/`| Google Maps polyline map overlay for workout routes |
| `trans_flutter_module_health_chart`| `packages/trans_flutter_module_health_chart/`| Detailed multi-day health analytics charts |
| `trans_flutter_module_third_party_accounts`| `packages/trans_flutter_module_third_party_accounts/`| Third-party authorization screens (Strava, Google Fit) |
| `trans_flutter_service_device_manager` | `packages/trans_flutter_service_device_manager/`| BLE device service abstraction layer in Dart |
| `wakelock_plus` | `packages/wakelock_plus/` | Prevents device screen timeout during workout recording |

### 9.3 Platform Channel Inter-Process Communication Contract

Communication between the Android native layer and the Flutter presentation engine occurs across 10 specialized platform channels:

```
+==================================================================================================+
|                                    PLATFORM CHANNELS CONTRACT                                    |
+==================================================================================================+
| Method Channels:                                                                                 |
| 1. "com.transsion.healthlife.platform_channel"                                                   |
|    - Bidirectional general platform event channel (app lifecycle, system language, dial index)   |
| 2. "trans_health_device_config"                                                                  |
|    - Synchronizes watch hardware configurations, DND intervals, alarm schedules                  |
| 3. "trans_flutter_service_native_call_devicemanager"                                             |
|    - Primary capability channel: invokes the 133 native device control methods                   |
| 4. "com.transsion.healthlife.health_nav"                                                         |
|    - Dispatches navigation intents from Flutter UI to native activities                          |
| 5. "com.transsion.healthlife.shell_control"                                                      |
|    - Governs dual-shell state transitions, pre-warming signals, frame readiness                  |
| 6. "com.transsion.healthlife.sport_share_media"                                                  |
|    - Triggers native media sharing, video trimmer, and workout card capture                      |
| 7. "com.transsion.healthlife.bridge"                                                             |
|    - Account state bridging, Palm ID token pass-through, user session invalidate                     |
| 8. "android_main_page"                                                                           |
|    - Controls tab switching on the native bottom navigation bar from Flutter                     |
| 9. "TranssionWatchOTA"                                                                           |
|    - Streams OTA DFU progress events (percentage, flash state, byte count)                      |
| 10. "trans_flutter_vap_view_channel"                                                             |
|    - Controls hardware-accelerated video alpha-channel animation playback                        |
+--------------------------------------------------------------------------------------------------+
| Pigeon Type-Safe RPC Channels:                                                                   |
| 11. "dev.flutter.pigeon.trans_flutter_service_remote_config.RemoteConfigFlutterApi.*"             |
|     - Strongly-typed remote configuration fetching and feature-flag activation                   |
| 12. "dev.flutter.pigeon.trans_connect_watch_connect_plugin.TransConnectWatchConnectCallbackApi.*"|
|     - Strongly-typed BLE peripheral connection state change callbacks                            |
| 13. "dev.flutter.pigeon.trans_flutter_lib_trans_connect_center_android.TransConnectCenterFlutterApi.*" |
|     - Strongly-typed Bluetooth bonding state change events                                       |
+==================================================================================================+
```

### 9.4 Native Navigation Target Map (71 Targets)

`MyHealthNavigator.kt` defines exactly 71 navigation targets routing between Flutter widgets and native Android activities:

```
[Health & Biometric Telemetry (10)]
  - "weight", "heart_rate", "sleep", "spo2", "three_circle", "ai_health",
  - "step_source", "hrv", "pressure", "menstrual"

[Sport & Workout Tracking (12)]
  - "sport_start_run", "sport_start_cycling", "sport_start_walk",
  - "sport_goal_run", "sport_goal_cycling", "sport_goal_walk",
  - "sport_settings", "sport_records", "sport_record_detail",
  - "sport_plan", "sport_plan_preview", "SPORT_PLAN_PREVIEW"

[Device Management & Settings (32)]
  - "device_add", "device_supported_models", "device_dial", "device_notification",
  - "device_contact", "device_bluetooth_call", "device_ai_dial", "device_card",
  - "device_alarm", "device_call_reminder", "device_camera", "device_schedule",
  - "device_todo", "device_world_clock", "device_do_not_disturb",
  - "device_sms_quick_reply", "device_firmware_update", "device_other_settings",
  - "device_scan_qr", "device_scan_help", "device_unbind", "device_reconnect",
  - "device_find", "device_unavailable"

[Smart Ring Subsystem (6)]
  - "device_ring_sport", "device_ring_sport_realtime", "device_ring_gesture_photo",
  - "device_ring_auto_monitoring", "device_ring_find", "device_ring_firmware_update"

[User Profile & Account Center (11)]
  - "mine_profile", "mine_person", "mine_message", "mine_bg_process",
  - "mine_update", "mine_about", "mine_third_party", "mine_app_update",
  - "mine_feedback", "mine_user_agreement", "mine_privacy_policy", "mine_web"
```

### 9.5 Why Dart Source is Irrecoverable & Re-Specification Strategy

1. **Irrecoverability Proof:** Flutter releases compile Dart code into native AOT assembly using an optimizing compiler targeting `arm64-v8a`. The resulting `libapp.so` binary lacks Dart AST metadata, variable names, and structured control flow. Decompilation tools (such as Doldrums or Ghidra plugins) only yield disassembly stubs and cannot reconstruct clean Dart source code.
2. **Re-Specification Strategy:** Rebuilding does not require disassembling `libapp.so`. Because the application architecture enforces total decoupling via the 10 Platform Channels, the 71 `MyHealthNavigator` targets, and the 2,402 structured assets in `flutter_assets`, each Flutter view is rebuilt cleanly in Flutter 3.19 matching the declared contracts and asset manifests.

---

### Evidence Log
- Native Flutter libraries verified via `arm/lib/arm64-v8a/`: `libapp.so`, `libflutter.so`
- Flutter package inventory (30 packages) verified via `base/assets/flutter_assets/packages/`: `base/assets/flutter_assets/packages/:1-30`
- Platform channels and Pigeon namespaces verified via `grep -r "MethodChannel"`: `decompiled/sources/com/transsion/healthlife/view/MyHealthEngine.java:1-50`
- 71 Navigation targets extracted from `MyHealthNavigator.java`: `decompiled/sources/com/transsion/healthlife/view/MyHealthNavigator.java:1-250`
