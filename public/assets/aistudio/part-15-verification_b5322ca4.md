# Verification Matrix, Quality Assurance & Parity Certification

### 15.1 Quality Assurance Matrix Across Engineering Tiers

To validate that the clean-room reconstructed application delivers total behavioral equivalence to the original production artifact, verification spans 7 distinct testing disciplines:

| Test Discipline | Target Subsystem | Automated Framework | Success Gate Criteria |
| :--- | :--- | :--- | :--- |
| **Unit Testing** | Biometric calculators, pace estimators, CRC codecs | JUnit 5, MockK, Truth | 100% test pass rate across all edge cases (zero cadence, GPS loss) |
| **DB Migration Testing** | Room `HealthDataBase` (v1 -> v27) | AndroidX Room Testing, Robolectric | Zero column loss; identical SQLite schema hash `884db9bc426...` |
| **BLE Simulation** | GATT connection, MTU 247, packet reassembly | Android BLE Mock Server, Virtual Peripheral | Sustained 24-hour synthetic telemetry streaming without packet loss |
| **API Contract Testing** | Palm ID, NebulaLive, Ella AI Voice Gateway | WireMock, MockWebServer | JSON schemas match captured production request/response payloads |
| **Channel Contract** | 10 Platform Channels, 133 Capability Methods | Flutter Driver, Pigeon Test Harness | Complete bidirectional invocation with type-safe response handling |
| **UI Smoke Testing** | 4 Main Tabs, Sport HUD, Watch Dial Studio | Espresso, UI Automator | Seamless navigation across all 71 `MyHealthNavigator` routes |
| **Performance Budget** | Memory heap, CPU utilization, battery drain | Android Profiler, Battery Historian | Idle RAM < 180 MB, background CPU < 1.5%, zero wakelock leaks |

### 15.2 Room Schema Migration Test Specification

The Room migration test harness guarantees that upgrading an existing user's database from any legacy schema version to version 27 completes without data loss or table recreation:

```kotlin
@RunWith(AndroidJUnit4::class)
class RoomDatabaseMigrationTest {
    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        HealthDataBase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun testAllMigrations_FromV1ToV27_MaintainsIntegrity() {
        // Create earliest database schema v1
        var db = helper.createDatabase(TEST_DB, 1).apply {
            execSQL("INSERT INTO StepEntity (time, step, openId, stepSource) VALUES (1600000000, 5280, 'test_user', 1)")
            close()
        }

        // Run migrations sequentially up to v27
        db = helper.runMigrationsAndValidate(
            TEST_DB, 27, true,
            *HealthDataBase.ALL_MIGRATIONS // All 52 bidirectional migrations
        )

        // Verify migrated records survive with intact columns
        val cursor = db.query("SELECT step, openId, stepSource FROM StepEntity WHERE time = 1600000000")
        assertThat(cursor.moveToFirst()).isTrue()
        assertThat(cursor.getInt(0)).isEqualTo(5280)
        assertThat(cursor.getString(1)).isEqualTo("test_user")
        cursor.close()
    }
}
```

### 15.3 Synthetic BLE Hardware Simulation Harness

Because physical hardware watches from all 49 models may not be immediately available during agentic rebuilding, testing utilizes a software-defined BLE peripheral simulator:

```
+==================================================================================================+
|                                    BLE HARDWARE SIMULATION HARNESS                               |
+==================================================================================================+
|  [Virtual BLE Peripheral Engine] (Node.js bleno / Python bleak-server / Android BLE Server)      |
|             |                                                                                    |
|             +---> Advertises Service UUID: 0x180D (Heart Rate), 0xFEE7 (Transsion Custom)        |
|             +---> Responds to MTU Request: Grants 247-byte MTU frame size                        |
|             +---> Simulates Handshake: Computes AES-128 challenge matching libRtkAesJni         |
|             +---> Emits Telemetry Loops:                                                         |
|                     - 1-minute Heart Rate indication (60–140 BPM randomized walking profile)     |
|                     - 15-minute Step packet (WatchStepListEntity array)                          |
|                     - Daily Hypnogram array (Light / Deep / REM stage sequence)                  |
|             +---> Injects Error Scenarios:                                                       |
|                     - Out-of-range disconnect (Tests exponential backoff reconnect)              |
|                     - GATT Error 133 injection (Tests refreshDeviceCache reflection call)        |
|                     - Incomplete DFU chunk drop (Tests abortFirmwareUpgrade recovery)            |
+==================================================================================================+
```

### 15.4 Platform Channel Verification Test Harness

Validates that all 133 device manager capability methods and the 10 platform channels execute without unhandled method exceptions:

```dart
// Flutter Platform Channel Integration Test
void main() {
  testWidgets('Verify Device Capability Channel Invocations', (WidgetTester tester) async {
    const channel = MethodChannel('trans_flutter_service_native_call_devicemanager');
    
    // Test 1: Real-time heart rate start
    final hrStartResult = await channel.invokeMethod<bool>('startMeasureHeartRateData', {'sampleInterval': 1});
    expect(hrStartResult, isTrue);

    // Test 2: Sync time
    final syncTimeResult = await channel.invokeMethod<bool>('syncTime', {'timestamp': DateTime.now().millisecondsSinceEpoch});
    expect(syncTimeResult, isTrue);

    // Test 3: Watch dial query
    final dialLayout = await channel.invokeMapMethod<String, dynamic>('queryWatchDialLayout');
    expect(dialLayout, isNotNull);
    expect(dialLayout!['supportedFormat'], contains('RGB565'));
  });
}
```

### 15.5 Proving Full Parity with the Original Application

To demonstrate 100% functional equivalence between the rebuilt application and the original production APK:

1. **Dual Device Side-by-Side Execution:** Deploy the original APK on Device A and the reconstructed APK on Device B (identical Android 14 hardware, e.g. Tecno Camon 30 / Infinix Note 40).
2. **Synchronized Watch Pairing:** Pair each phone sequentially to the same physical hardware watch (e.g. itel ISW-O20 or Infinix Watch 1).
3. **Telemetry Comparison:** Capture 24 hours of continuous telemetry. Export the SQLite databases from both devices:
   - Run SQL diff comparing row count and values across `WatchHeartRateListEntity`, `WatchStepListEntity`, and `WatchSleepListEntity`.
   - Tolerance threshold: 0% divergence on identical raw packets.
4. **Workout Track Parity:** Perform a 3 km outdoor run with both phones mounted simultaneously. Compare recorded GPX/KML routes, average pace calculation, and 1 km split timing.
5. **Memory & CPU Profiling Parity:** Confirm the reconstructed build does not exceed 110% of the original memory footprint and produces 0 unhandled crash loop events in `MyHealthShellCrashBreaker`.

---

### Evidence Log
- Room testing framework requirements mapped from `HealthDataBase_Impl.java`: `HealthDataBase_Impl.java:314`
- Channel test methods mapped from 133 capability methods: `TransFlutterServiceNativeCallDevicemanagerPlugin.java:1-133`
- BLE reconnect error handling verified in `DeviceBleService.java`: `decompiled/sources/com/transsion/devices/service/DeviceBleService.java:1-80`
- Navigation routes verified in `MyHealthNavigator.java`: `MyHealthNavigator.java:1-250`
