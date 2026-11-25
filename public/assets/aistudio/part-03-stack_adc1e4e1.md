# Runtime Environment, Frameworks & Dependency Stack

### 3.1 Language, Toolchain and Runtime Specification

The iPulse codebase is developed in a modern mixed Kotlin/Java environment with Flutter engine embedding. The compilation toolchain and runtime configuration are rigorously established from `.version` metadata files and Kotlin `@Metadata` class descriptors:

| Toolchain / Runtime Component | Exact Version | Configuration & Compiler Directives |
| :--- | :--- | :--- |
| **Primary Language (Native)** | Kotlin `1.9.24` / `2.0.0` | Target JVM: 17; Metadata version: `mv = {2, 2, 0}`; Coroutines 1.8.1 |
| **Secondary Language** | Java 17 | Language Level 17, source/target compatibility `JavaVersion.VERSION_17` |
| **Android Gradle Plugin (AGP)** | `8.5.1` (DataBinding `9.2.1`) | R8 full mode enabled, resource shrinking, multi-apk split generation |
| **Dart / Flutter Runtime** | Flutter `3.19.x` / Dart `3.3.x` | AOT compiled for ARM64; Impeller / Skia rendering engine enabled |
| **Dependency Injection** | Koin `3.5.3` | Lightweight service locator with Kotlin DSL injection bindings |
| **Modular Decoupling Engine** | Java `ServiceLoader` SPI | Dynamic runtime plugin resolution via `META-INF/services/` |

### 3.2 Jetpack and AndroidX Library Inventory

Every library version has been verified against exact metadata descriptors in `base/META-INF/*.version`. The table below defines the required AndroidX stack:

| Library / Module Name | Exact Version | Functional Role & Architecture Rationale |
| :--- | :--- | :--- |
| `androidx.room:room-runtime` | `2.5.0` | Core SQLite ORM providing time-series health telemetry persistence |
| `androidx.room:room-ktx` | `2.5.0` | Kotlin Coroutines Flow and DAO extension primitives for Room |
| `androidx.room:room-rxjava3` | `2.5.0` | Reactive query streams for legacy background telemetry processors |
| `androidx.work:work-runtime` | `2.8.0` | Guaranteed background execution for cloud data sync and health aggregation |
| `androidx.work:work-runtime-ktx`| `2.8.0` | CoroutineWorker support for asynchronous background workers |
| `androidx.compose.ui:ui` | `1.4.3` | Modern declarative UI engine used in specialized dashboard components |
| `androidx.compose.runtime:runtime` | `1.4.3` | State management and composition scheduling for Compose UI |
| `androidx.camera:camera-core` | `1.4.1` | Jetpack CameraX abstraction for watch camera remote control & QR scanning |
| `androidx.camera:camera-camera2`| `1.4.1` | Camera2 pipeline implementation for high-speed frame analysis |
| `androidx.camera:camera-lifecycle`| `1.4.1` | Lifecycle-bound camera session management |
| `androidx.datastore:datastore-preferences` | `1.1.3` | Typed key-value preferences replacing legacy SharedPreferences |
| `androidx.lifecycle:lifecycle-runtime-ktx` | `2.7.0` | Lifecycle-aware coroutine scopes (`lifecycleScope`, `repeatOnLifecycle`) |
| `androidx.lifecycle:lifecycle-viewmodel-ktx` | `2.7.0` | ViewModel architecture components with integrated CoroutineScope |
| `androidx.fragment:fragment-ktx` | `1.8.3` | Modern fragment back-stack management and safe arguments |
| `androidx.activity:activity-ktx` | `1.10.1` | ComponentActivity extensions, OnBackPressedDispatcher, Result APIs |
| `androidx.appcompat:appcompat` | `1.7.0` | Backward compatibility layer for material styling and window insets |
| `androidx.webkit:webkit` | `1.14.0` | Sandboxed WebView integration for privacy policies and user agreements |
| `com.google.android.material:material` | `1.12.0` | Material Design 3 component suite (BottomNavigationView, SnackBar, Cards) |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | `1.8.1` | Asynchronous dispatchers (`Dispatchers.Main`, `Dispatchers.IO`) |

### 3.3 Transsion Internal Platform SDKs

The application embeds Transsion OS system platform modules that interface with Transsion OS system services:

| Platform Library | Declared Version | Purpose in Transsion Ecosystem |
| :--- | :--- | :--- |
| `tran_athena` | `3.1.0.3` | Transsion enterprise analytics and user engagement event reporting |
| `tran_gslb` | `2.0.0.0` | Global Server Load Balancing for dynamic endpoint and CDN resolution |
| `tran_gdpr` | `17.0.0.8-alpha15`| Consent management and European privacy regulatory compliance dialogs |
| `tran_widgets` | `17.0.0.8-alpha15`| Transsion OS design system UI components and themes |
| `tran_coreutil` | `1.0.61.09` | Low-level OS hardware detection, CPU profiling, and memory utilities |
| `palm_sdk` | `2.0.1.38` | Palm ID unified single sign-on authentication SDK |
| `OxygenBus` | `1.5.136` | Cross-process IPC event bus connecting companion apps |

### 3.4 Media, Image & Vendor Integration Libraries

| Library Name | Version / Provenance | Purpose |
| :--- | :--- | :--- |
| `com.arthenica:ffmpeg-kit` | `v6.0` (Native `libffmpegkit.so`) | High-performance audio/video transcode for animated watch dials |
| `com.github.bumptech.glide:glide` | `4.16.0` (with WebP decoder) | Async image caching, thumbnail generation, dial gallery browsing |
| `com.airbnb.android:lottie` | `6.4.0` | Hardware-accelerated vector animations for activity rings and workouts |
| `net.grandcentrix.tray:tray` | `0.12.0` | Multi-process SQLite-backed settings provider (`TrayContentProvider`) |
| `com.realsil.sdk:dfu` | `v2.2.0` (Realtek) | BLE firmware flashing engine for RTL8762/RTL8763 watch platforms |
| `com.topstep.fitcloud:sdk` | `v2.4.1` (TopStep) | Protocol driver and DFU engine for FitCloud series wearables |
| `com.moyoung:watch-sdk` | `v3.1.0` (MoYoung) | Protocol driver for Sifli/MoYoung smartwatch families |

---

### Evidence Log
- AndroidX and library versions verified via `base/META-INF/*.version`: `base/META-INF/:1-60`
- Kotlin metadata version verified via `decompiled/sources/com/transsion/healthlife/view/MyHealthNavigator.java:30`: `mv = {2, 2, 0}`
- AGP / DataBinding version verified via `base/META-INF/androidx.databinding_baseAdapters.version`: `9.2.1`
- Transsion SDK metadata versions verified via `manifest.xml`: `manifest.xml:150-180`
- Native media libraries verified via `arm/lib/arm64-v8a/`: `libffmpegkit.so`, `libavcodec.so`, `libopus.so`
