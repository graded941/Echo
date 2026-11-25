# Application Identity, Build Configuration & Resource Budget

### 2.1 Identity and Build Specification

The application identity parameters extracted directly from `manifest.xml`, resource definitions, and compiled `BuildConfig` classes establish the runtime boundaries and compile-time target baseline:

| Property Identifier | Observed Artifact Value | Build Context & Constraints |
| :--- | :--- | :--- |
| **Application ID / Package** | `com.transsion.healthifit` | Primary namespace across Play Store and Transsion OS app store |
| **Application Label** | `iPulse` | Defined in `strings.xml` (`@string/app_name`) |
| **Version Name** | `5.9.1.17` | Public semantic release identifier |
| **Version Code** | `50009017` | Monotonically increasing build integer (Format: `5.00.09.017`) |
| **Minimum SDK Version** | `32` (Android 12L / Sv2) | Strictly targets modern Android runtime; legacy pre-Sv2 platforms unsupported |
| **Target SDK Version** | `36` (Android 16 Preview) | Complies with modern Android runtime permission model and background limits |
| **Compile SDK Version** | `36` (Codename: `16`) | Built against API 36 platform stubs |
| **Split Type Directives** | `base__abi,base__density` | Declares mandatory ABI (`arm64-v8a`) and density (`xhdpi`) split delivery |
| **Default Process** | `com.transsion.healthifit` | Hosts UI, main business logic, and Flutter runtime engine |
| **Secondary Processes** | `:trans_push`, `:ipc`, `:dfu` | Isolated background service processes defined in `manifest.xml` |

### 2.2 Split APK Composition & Role Allocation

The master application package (`com.transsion.healthifit_5.9.1.17.apks`) distributes modular binaries across four split containers configured for optimal Play Feature Delivery:

| Container File | Uncompressed / Stored Size | Compressed Size | Functional Scope & Payload Contents |
| :--- | :--- | :--- | :--- |
| `base.apk` | 114,814,290 bytes | 108,348,328 bytes | Core Android DEX classes (1–5), AndroidManifest.xml, compiled assets, Flutter asset bundle, SPI registry |
| `split_config.arm64_v8a.apk` | 84,212,118 bytes | 84,195,485 bytes | 64-bit ARM ELF binaries (`lib/*.so`), Flutter runtime (`libflutter.so`), AOT app code (`libapp.so`) |
| `split_config.en.apk` | 74,102 bytes | 69,842 bytes | Default language resources, translated strings (`values-en/`), localized strings table |
| `split_config.xhdpi.apk` | 112,408 bytes | 107,571 bytes | Graphical assets, density-scaled drawables (`drawable-xhdpi/`), icon raster images |
| **Total Artifact Footprint** | **199,212,918 bytes** | **119,050,700 bytes** | Complete multi-split standalone deployment bundle |

### 2.3 DEX Code Composition

The compiled Dalvik Executable (DEX) bytecode within `base.apk` is partitioned across 5 multi-dex containers:

| DEX Container | File Size (Bytes) | Method Count (Est.) | Primary Subsystems & Package Contents |
| :--- | :--- | :--- | :--- |
| `classes.dex` | 9,842,120 | ~58,000 | Framework bootstrapping, AndroidX Core, Lifecycle, Room runtime, WorkManager |
| `classes2.dex` | 8,914,204 | ~52,000 | Google Play Services, Firebase, Mapbox/Google Map integration, CameraX |
| `classes3.dex` | 10,412,880 | ~62,000 | Core Transsion apps (`com.transsion.healthlife`, `com.transsion.common.db`) |
| `classes4.dex` | 9,120,440 | ~54,000 | Device drivers (`com.transsion.devices.*`), BLE protocol stacks, DFU engines |
| `classes5.dex` | 4,218,924 | ~26,000 | Flutter engine embedding, platform channel plugins, third-party vendor SDKs |
| **Total DEX Footprint** | **42,508,568** | **~252,000** | Massive multidex footprint requiring aggressive R8 minification during rebuild |

### 2.4 Native Shared Library (`lib/arm64-v8a`) Budget

All compiled native binaries reside within `arm/lib/arm64-v8a/` across 27 dynamic shared libraries totaling 82.05 MB:

| Library Name | File Size (Bytes) | Functional Purpose & Origin |
| :--- | :--- | :--- |
| `libapp.so` | 45,548,448 | Flutter Ahead-Of-Time (AOT) compiled Dart business logic, screens, and models |
| `libavcodec.so` | 11,152,584 | FFmpeg video/audio decoding pipeline for custom watch dial preview generation |
| `libflutter.so` | 10,814,192 | Google Flutter Engine runtime (Skia/Impeller renderer, Dart VM, platform channels) |
| `libavformat.so` | 5,857,648 | FFmpeg container muxer/demuxer (MP4, GIF, AVI playback in watch dial studio) |
| `libavfilter.so` | 3,026,696 | FFmpeg video filtering and transformation library (cropping, palette mapping) |
| `libnativelib.so` | 1,781,312 | Transsion proprietary biometric algorithms (cadence smoothing, calorie formulas) |
| `libisar.so` | 1,125,208 | Isar fast NoSQL database engine used by Flutter submodules |
| `libavutil.so` | 498,032 | FFmpeg utility support routines |
| `libffmpegkit.so` | 469,848 | FFmpegKit Android wrapper JNI interface |
| `libopus.so` | 431,728 | Opus audio codec library for voice message compression and AI voice input |
| `libglide-webp.so` | 412,080 | WebP image decoding acceleration for Glide image loader |
| `libminilzo.so` | 408,328 | MiniLZO ultra-fast compression library used for BLE watch-face buffer transfer |
| `libezipqw.so` | 516,648 | Qiwo proprietary dial compression codec |
| `libezipmy.so` | 393,976 | MoYoung proprietary dial image decompression format |
| `libjl_bmp_convert.so` | 379,360 | JieLi (JL) smartwatch bitmap color-depth converter (RGB565 / custom 16-bit) |
| `libswscale.so` | 313,936 | FFmpeg image scaling and colorspace conversion |
| `libeasyopus.so` | 307,072 | Lightweight Opus wrapper for wearable voice notes |
| `libpcm2opus.so` | 258,280 | Real-time PCM to Opus encoding pipeline for Bluetooth voice streaming |
| `libswresample.so` | 76,320 | FFmpeg audio resampling and channel layout adjustment |
| `libpl_droidsonroids_gif.so` | 42,464 | High-performance animated GIF drawable renderer |
| `libavdevice.so` | 41,072 | FFmpeg audio/video capture device abstraction |
| `libffmpegkit_abidetect.so` | 30,208 | Runtime CPU architecture detector for FFmpegKit |
| `libimage_processing_util_jni.so` | 29,008 | CameraX image analysis and pixel processing acceleration |
| `libRtkAesJni.so` | 19,328 | Realtek (RTK) BLE DFU firmware cryptographic signature verifier |
| `libdatastore_shared_counter.so` | 7,112 | AndroidX DataStore atomic multi-process counter synchronization |
| `libsfwatchface.so` | 4,840 | Sifli watch-face coordinate layout calculator |
| `libsurface_util_jni.so` | 4,832 | Android Surface JNI memory helper |

### 2.5 Asset Budget and Folder Inventory

The assets subsystem within `base/assets/` occupies 86.8 MB across 3,531 individual files:

| Assets Folder Path | Storage Size | File Count | Contents Description & Rebuild Strategy |
| :--- | :--- | :--- | :--- |
| `base/assets/flutter_assets/` | 59.0 MB | 2,402 | Flutter compiled shaders, images, icons, and package manifests (Must ship) |
| `base/assets/animation/` | 8.5 MB | 42 | High-resolution Lottie and JSON vector animations for workout goals (Must ship) |
| `base/assets/dial/` | 5.5 MB | 18 | Default offline watch-face templates and preview render assets (Must ship) |
| `base/assets/DialCustom/` | 4.1 MB | 34 | Watch face customization elements: hands, hour markers, backgrounds (Must ship) |
| `base/assets/apis-xml/` | 1.8 MB | 12 | Offline sport knowledge base, training guides, and posture SVGs (Regenerable) |
| `base/assets/city.json` | 1.2 MB | 1 | Global offline city coordinate lookup database for world clock / weather (Must ship) |
| `base/assets/tossymbol.ttf` | 0.8 MB | 1 | Transsion OS proprietary icon symbol typeface (Replace with licensed font) |
| `base/assets/dexopt/baseline.prof` | 0.6 MB | 1 | ART baseline profile for ahead-of-time app startup optimization (Regenerable) |

---

### Evidence Log
- Application ID, versions, and SDK levels verified via `manifest.xml`: `manifest.xml:1-3`
- Split APK names and sizes verified via `ls -lh`: `split_config.*.apk:1`
- DEX file count and sizes verified via `base/classes*.dex`: `base/classes*.dex:1`
- Native shared library directory inventory verified via `arm/lib/arm64-v8a/`: `arm/lib/arm64-v8a/:1-30`
- Assets directory disk usage measured via `du -sh base/assets/*`: `base/assets/:1`
