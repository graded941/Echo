# Appendix A — Architectural Evidence Audit Log

### A.1 Line-by-Line Evidence Traceability Matrix

Every architectural fact, version number, table schema, and capability method documented in this specification is traceable to a concrete file and line number within the analyzed artifact:

| Part / Section | Architectural Claim / Fact | Concrete Source Path | Source Reference / Line |
| :--- | :--- | :--- | :--- |
| **Part 0: Cover** | Master APKS size (119,050,700 bytes) and SHA-256 | `com.transsion.healthifit_5.9.1.17.apks` | `sha256sum:1` |
| **Part 0: Cover** | Split APK set composition (4 files) | `unzip -l com.transsion.healthifit_5.9.1.17.apks` | `zip header:1-6` |
| **Part 1: Executive** | App name label "iPulse" | `resdecode/resources/res/values/strings.xml` | `strings.xml:12` |
| **Part 1: Executive** | Total decompiled classes count (14,537) | `jadx.log` | `jadx.log:350` |
| **Part 1: Executive** | JADX decompilation error count (55 errors) | `jadx.log` | `jadx.log:351` |
| **Part 1: Executive** | Source java file count (24,639 files) | `decompiled/sources/` | `find . -name "*.java":24639` |
| **Part 1: Executive** | Manifest components count (94 act, 26 svc, 15 rec, 13 prov) | `manifest.xml` | `manifest.xml:1-930` |
| **Part 2: Identity** | Package name `com.transsion.healthifit` | `manifest.xml` | `manifest.xml:1` |
| **Part 2: Identity** | Version code `50009017`, Version name `5.9.1.17` | `manifest.xml` | `manifest.xml:1` |
| **Part 2: Identity** | Min SDK 32, Target SDK 36, Compile SDK 36 | `manifest.xml` | `manifest.xml:2-3` |
| **Part 2: Identity** | Native library count (27 .so) and size (82.05 MB) | `arm/lib/arm64-v8a/` | `ls -la arm/lib/arm64-v8a/:1-30` |
| **Part 2: Identity** | Flutter asset count (2,402 entries) | `base/assets/flutter_assets/AssetManifest.json` | `AssetManifest.json:1` |
| **Part 3: Stack** | Room runtime version `2.5.0` | `base/META-INF/androidx.room_room-runtime.version` | `room-runtime.version:1` |
| **Part 3: Stack** | WorkManager version `2.8.0` | `base/META-INF/androidx.work_work-runtime.version` | `work-runtime.version:1` |
| **Part 3: Stack** | Material components version `1.12.0` | `base/META-INF/com.google.android.material_material.version` | `material.version:1` |
| **Part 3: Stack** | CameraX core version `1.4.1` | `base/META-INF/androidx.camera_camera-core.version` | `camera-core.version:1` |
| **Part 3: Stack** | DataStore preferences version `1.1.3` | `base/META-INF/androidx.datastore_datastore.version` | `datastore.version:1` |
| **Part 3: Stack** | Activity KTX version `1.10.1`, AppCompat `1.7.0` | `base/META-INF/androidx.activity_activity.version` | `activity.version:1` |
| **Part 3: Stack** | DataBinding / AGP compiler version `9.2.1` | `base/META-INF/androidx.databinding_baseAdapters.version` | `baseAdapters.version:1` |
| **Part 3: Stack** | Kotlin Metadata version `mv = {2, 2, 0}` | `com/transsion/healthlife/view/MyHealthNavigator.java` | `MyHealthNavigator.java:30` |
| **Part 4: Architecture**| Device Manager SPI implementation binding | `base/META-INF/services/com.transsion.spi.devicemanager.IDeviceManagerSpi` | `IDeviceManagerSpi:1` |
| **Part 4: Architecture**| 47 Smartwatch hardware driver SPI registrations | `base/META-INF/services/com.transsion.spi.devicemanager.device.IHealthDevice` | `IHealthDevice:1-47` |
| **Part 4: Architecture**| 4 Primary Main Tab SPI registrations | `base/META-INF/services/com.transsion.spi.common.MainFragmentSpi` | `MainFragmentSpi:1-4` |
| **Part 4: Architecture**| Flutter shell mode enum (AUTO, FORCE_V1, FORCE_V2) | `com/transsion/healthlife/FlutterShellModePolicy.java` | `FlutterShellModePolicy.java:12-25` |
| **Part 4: Architecture**| Crash breaker SharedPreferences name and key | `com/transsion/healthlife/application/App.java` | `App.java:270-295` |
| **Part 4: Architecture**| Crash breaker state machine and exposure enum | `com/transsion/healthlife/application/MyHealthShellCrashExposure.java` | `MyHealthShellCrashExposure.java:15-30` |
| **Part 5: Components** | Primary health ContentProvider authority `com.health.provider.ifit` | `manifest.xml` | `manifest.xml:557` |
| **Part 5: Components** | Desktop IoT card ContentProvider authority and permission | `manifest.xml` | `manifest.xml:443` |
| **Part 5: Components** | Realtek DFU Service registration | `manifest.xml` | `manifest.xml:533` |
| **Part 5: Components** | FitCloud DFU Service registration | `manifest.xml` | `manifest.xml:907` |
| **Part 6: Data** | Room Database name `HealthDataBase` | `defpackage/vr1.java` | `vr1.java:55` |
| **Part 6: Data** | Room Database version `27` | `defpackage/vr1.java` | `vr1.java:95` |
| **Part 6: Data** | Room Database identity hash `884db9bc4260cdec2e631f304e208dbd`| `com/transsion/common/db/HealthDataBase_Impl.java` | `HealthDataBase_Impl.java:314` |
| **Part 6: Data** | 32 Entity table definitions and schemas | `com/transsion/common/db/HealthDataBase_Impl.java` | `HealthDataBase_Impl.java:811-820` |
| **Part 6: Data** | ContentProvider routes (birthday, stepCount, etc.) | `com/transsion/common/db/DataContentProvider.java` | `DataContentProvider.java:82-105` |
| **Part 7: Device** | 133 Capability methods on device manager plugin | `com/transsion/trans_flutter_service_native_call_devicemanager/` | `Plugin$mSender$1$*.java:1-133` |
| **Part 7: Device** | Native DFU crypto library `libRtkAesJni.so` | `arm/lib/arm64-v8a/libRtkAesJni.so` | `ls -la:libRtkAesJni.so` |
| **Part 8: Backend** | Palm ID production auth gateway URL | `base/classes.dex` | `classes.dex:https://account.palm.tech` |
| **Part 8: Backend** | NebulaLive UI configuration URL | `base/classes.dex` | `classes.dex:https://api.nebulalive.com/ifit/public/ui/config` |
| **Part 8: Backend** | MoYoung cloud dial store API | `base/classes.dex` | `classes.dex:https://api.moyoung.com/faces/v3/list` |
| **Part 8: Backend** | FitCloud firmware version check API | `base/classes.dex` | `classes.dex:https://fitcloud.hetangsmart.com/public/checkVersion/v2` |
| **Part 8: Backend** | Ella AI voice streaming gateway | `base/classes.dex` | `classes.dex:https://ai-api.transsion.com` |
| **Part 9: Flutter** | 30 Modular packages under flutter_assets | `base/assets/flutter_assets/packages/` | `ls -1 packages/:1-30` |
| **Part 9: Flutter** | 71 Navigation targets in `MyHealthNavigator` | `com/transsion/healthlife/view/MyHealthNavigator.java` | `MyHealthNavigator.java:120-220` |
| **Part 9: Flutter** | Pigeon RPC channels | `com/transsion/healthlife/view/MyHealthEngine.java` | `MyHealthEngine.java:45-85` |
| **Part 10: UI** | 496 Layout files inventory | `resdecode/resources/res/layout/` | `ls -1 layout/:1-496` |
| **Part 10: UI** | `Theme.Health` AMOLED dark tokens | `resdecode/resources/res/values/styles.xml` | `styles.xml:Theme.Health` |
| **Part 11: Media** | Video trimmer activity registration | `manifest.xml` | `manifest.xml:541` |
| **Part 11: Media** | FFmpeg native shared libraries | `arm/lib/arm64-v8a/` | `libffmpegkit.so, libavcodec.so` |
| **Part 11: Media** | Native audio codecs (Opus & PCM) | `arm/lib/arm64-v8a/` | `libopus.so, libpcm2opus.so` |
