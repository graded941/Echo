# Target Gradle Build System, R8 Optimization & CI/CD Pipeline

### 13.1 Multi-Module Gradle Architecture

To reconstruct iPulse cleanly while maintaining strict modular boundaries, the build system is organized into a modular Gradle project structure:

```
iPulse-Rebuild/
├── build.gradle.kts                # Root build configuration
├── settings.gradle.kts             # Module definitions & plugin repositories
├── gradle/libs.versions.toml       # Version Catalog (dependencies & plugins)
├── app/                            # Application entry point, manifest, shell binders
├── core/
│   ├── common/                     # Logging, base utilities, Result helpers
│   ├── database/                   # Room HealthDataBase, migrations, DAOs
│   ├── ble/                        # Bluetooth GATT client, MTU, packet fragmentation
│   └── network/                    # Retrofit, OkHttp, GSLB resolver, Palm ID OAuth2
├── features/
│   ├── devices/                    # 49 ODM device drivers, DFU engines, dial pipeline
│   ├── sport/                      # Outdoor GPS tracking, pace calculator, sport plans
│   ├── health/                     # 3-circle activity rings, sleep staging, biometrics
│   └── mine/                       # User profile, goal settings, third-party integrations
└── flutter_embedding/              # FlutterEngine host, platform channel bindings
```

### 13.2 Target `build.gradle.kts` Specification (`app/build.gradle.kts`)

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
}

android {
    namespace = "com.transsion.healthifit"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.transsion.healthifit"
        minSdk = 32
        targetSdk = 36
        versionCode = 50009017
        versionName = "5.9.1.17"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.addAll(listOf("arm64-v8a"))
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a")
            isUniversalApk = false
        }
        density {
            isEnable = true
            reset()
            include("xhdpi", "xxhdpi")
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_PATH") ?: "keystore/release.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "<redacted>"
            keyAlias = System.getenv("KEY_ALIAS") ?: "ipulse_release"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "<redacted>"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-Xopt-in=kotlin.RequiresOptIn")
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
    }
}
```

### 13.3 ProGuard / R8 Rules Specification (`proguard-rules.pro`)

Due to extensive reflection across Java SPI, Room Database schemas, Flutter platform channels, and native JNI bindings, aggressive minification requires strict keep rules:

```proguard
# 1. Java ServiceLoader SPI (Mandatory for modular plugin discovery)
-keepnames class com.transsion.spi.** { *; }
-keepclassmembers class * implements com.transsion.spi.** {
    public <init>();
    public *;
}
-keep class * implements com.transsion.spi.devicemanager.device.IHealthDevice { *; }

# 2. Room Database & Master Table Hashes
-keep class androidx.room.** { *; }
-keep class com.transsion.common.db.HealthDataBase { *; }
-keep class com.transsion.common.db.HealthDataBase_Impl { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# 3. Native JNI & C++ Shared Library Bridge
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class com.realsil.sdk.dfu.** { *; }
-keep class com.topstep.fitcloud.sdk.** { *; }
-keep class com.arthenica.ffmpegkit.** { *; }

# 4. Flutter Platform Channels & Pigeon RPC
-keep class io.flutter.** { *; }
-keep class dev.flutter.pigeon.** { *; }
-keep class * implements io.flutter.plugin.common.MethodChannel$MethodCallHandler { *; }
-keep class com.transsion.healthlife.view.MyHealthNavigator { *; }

# 5. Serialization & JSON Encoders
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keepclassmembers enum * { *; }
```

### 13.4 Continuous Integration & Automated Delivery Outline

The automated build pipeline runs on GitHub Actions / GitLab CI targeting multi-split APK artifact generation:

```yaml
name: iPulse Clean-Room Rebuild CI

on:
  push:
    branches: [ main, release/* ]
  pull_request:
    branches: [ main ]

jobs:
  build-and-verify:
    runs-on: ubuntu-24.04
    steps:
      - name: Checkout Source Tree
        uses: actions/checkout@v4

      - name: Setup JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'

      - name: Setup Flutter 3.19.6
        uses: subosito/flutter-action@v2
        with:
          flutter-version: '3.19.6'
          channel: 'stable'

      - name: Verify Room Schema Identity Hash
        run: ./gradlew :core:database:test --tests "RoomSchemaVerificationTest"

      - name: Compile Native ELF Libraries
        run: ./gradlew :features:devices:assembleRelease

      - name: Assemble Multi-Split Bundle
        run: ./gradlew :app:bundleRelease

      - name: Execute Parity Acceptance Matrix
        run: ./gradlew :app:connectedCheck
```

---

### Evidence Log
- AGP version and DataBinding version verified via `base/META-INF/androidx.databinding_baseAdapters.version`: `9.2.1`
- Target SDK 36 and min SDK 32 verified via `manifest.xml`: `manifest.xml:1-3`
- R8 keep requirements derived from SPI files in `base/META-INF/services/`: `services/:1-86`
- Native ABI filter (`arm64-v8a`) confirmed by binary inventory in `arm/lib/arm64-v8a/`: `arm/lib/arm64-v8a/:1-27`
