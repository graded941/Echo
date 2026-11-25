# Wearable BLE Subsystem, Driver Abstraction & Device Protocols

### 7.1 Wearable Hardware Ecosystem & Driver Registry

The iPulse application interfaces with 49 distinct smartwatch hardware models manufactured across four primary Original Design Manufacturer (ODM) platform partners. All drivers implement the unified `IHealthDevice` SPI contract registered in `base/META-INF/services/com.transsion.spi.devicemanager.device.IHealthDevice`:

| ODM Family / Platform | Hardware Chipset / Transport | Driver Model Implementations | Protocol Characteristics |
| :--- | :--- | :--- | :--- |
| **MoYoung (Sifli)** | Sifli SF32LB55x / Goodix BLE | `ItelISWL1New`, `ItelISWN10New`, `ItelISWN10YNew`, `ItelISWN10_2`, `ItelISWO20New`, `ItelISWO23New`, `Watch3A2`, `Watch3ANew`, `WatchNeo01New`, `XWatch4B2New`, `XWatch4BNew`, `XWatch4LNew`, `XWatch4New`, `XWatch4UNew` | Protobuf-like framed packets, dynamic MTU (up to 247 bytes), multi-stage sleep sync |
| **TopStep (FitCloud)** | Realtek RTL8762D / Actions ATS | `ItelISWO25New`, `ItelISWO45New`, `WatchS5New`, `XWatch3CNew`, `XWatch3ENew`, `XWatch3New`, `XWatch4ENew` | FitCloud binary frame protocol, command byte routing, packed biometric bitfields |
| **Qiwo / HuaWo** | HunterSun / Telink BLE | `ItelISWN8New`, `ItelISWN8PNew`, `VideoTrimmerActivity` | WearableLink transport bridge, proprietary EZIP compression for watch dials |
| **CRP / HunterSun / UTE** | CRP / Actions / UTE SoC | `ISW32CrpDeviceNew`, `ISWO21CrpDeviceNew`, `ISWO41CrpDeviceNew`, `Watch1CrpDeviceNew`, `Watch2CrpDeviceNew`, `Watch3CrpDeviceNew`, `WatchProCrpDeviceNew`, `WatchPro2CrpDeviceNew`, `XWatch3GTCrpDeviceNew`, `XWatch3PlusCrpDeviceNew`, `UteXW1ProWatchDeviceNew`, `UteXW2WatchDeviceNew`, `ItelISWO18ENew`, `ItelISWO20BNew`, `XWatch5ENew`, `XWatch5PNew` | Lightweight CRC16 framed binary packets, sequential packet indexing |

### 7.2 Unified Capability API Surface (133 Methods)

The Flutter-to-Native bridge (`TransFlutterServiceNativeCallDevicemanagerPlugin`) exposes 133 distinct hardware capability methods, structured into 7 functional domains:

```
+==================================================================================================+
|                        DEVICE CAPABILITY SURFACE (133 METHODS)                                   |
+==================================================================================================+
| 1. Connection & Pairing (18 Methods)                                                             |
|    realConnect(), unbind(), queryDeviceInfo(), queryDeviceVersion(), sendUserIdentity(),         |
|    sendUserInfo(), syncLanguage(), syncTime(), sendTimeFormat(), sendTemperatureFormat()         |
+--------------------------------------------------------------------------------------------------+
| 2. Real-Time Telemetry & Measurement (28 Methods)                                                |
|    startMeasureHeartRateData(), stopMeasureHeartRateData(), startMeasureBloodOxygen(),           |
|    stopMeasureBloodOxygen(), startMeasurePressure(), stopMeasurePressure(),                      |
|    startMeasureHRVData(), stopMeasureHRVData(), startSyncSportData(), sendSportStatus()          |
+--------------------------------------------------------------------------------------------------+
| 3. Notification & Alert Mirroring (22 Methods)                                                   |
|    sendWatchContacts(), sendSosContact(), queryDrinkWaterReminder(), sendDrinkWaterReminder(),   |
|    sendSedentaryReminder(), sendAlarmList(), sendMissingCallReminder(), sendAppNotification()    |
+--------------------------------------------------------------------------------------------------+
| 4. Watch Face & Dial Management (24 Methods)                                                     |
|    queryWatchDialLayout(), sendWatchDialLayout(), sendWatchDialBackgroundInfo(),                |
|    getWatchDialOperateResult(), sendDialInstallProgress(), abortDialInstall(),                   |
|    operateElectronCard(), sendSortElectronicCards(), sendWorldClock(), sendSortWorldClock()      |
+--------------------------------------------------------------------------------------------------+
| 5. Hardware Remote Control & Tools (16 Methods)                                                  |
|    sendWatchEnterCamera(), sendWatchExitFindPhone(), sendStopFindDevice(), sendFindWatch(),      |
|    sendWeatherInfo(), sendMusicControl(), sendDoNotDisturbConfig(), sendRaiseToWakeConfig()      |
+--------------------------------------------------------------------------------------------------+
| 6. Firmware Flashing & DFU Maintenance (15 Methods)                                              |
|    sendStartFirmwareUpgrade(), queryFirmwareVersion(), abortFirmwareUpgrade(),                   |
|    sendDfuPackageChunk(), verifyDfuChecksum(), rebootDeviceAfterDfu()                            |
+--------------------------------------------------------------------------------------------------+
| 7. Ring & Smart Accessory Extensions (10 Methods)                                                |
|    sendRingGesturePhoto(), startRingAutoMonitoring(), findSmartRing(), sendRingSportRealtime()   |
+==================================================================================================+
```

### 7.3 Device Pairing & Cryptographic Bonding Workflow

```
[Phone: Bluetooth LE Scanner]                   [Smartwatch Peripheral]
             |                                             |
             | ----- 1. Directed / Periodic BLE Adv -----> | (Broadcasts Manufacturer MAC + Model ID)
             |                                             |
             | <---- 2. BLE Scan Result Received --------- |
             |                                             |
  [User Selects Device in UI]                              |
             |                                             |
             | ----- 3. gatt.connectGatt(autoConnect=false)->
             | <---- 4. onConnectionStateChange(CONNECTED)-|
             |                                             |
             | ----- 5. gatt.discoverServices() ---------> |
             | <---- 6. onServicesDiscovered() ----------- |
             |                                             |
             | ----- 7. gatt.requestMtu(247) ------------> | (Negotiate high-throughput MTU)
             | <---- 8. onMtuChanged(MTU=247) ------------ |
             |                                             |
             | ----- 9. Write Auth Handshake Packet -----> | (AES-128 Challenge using libRtkAesJni)
             | <---- 10. Peripheral Challenge Response --- |
             |                                             |
  [Verify Key & Bind Success]                              |
             |                                             |
             | ----- 11. Send User Profile (Age/Sex/Kg) -> |
             | ----- 12. Send Initial SyncTime & Lang ---> |
             | ----- 13. Enable Notification CCCD -------->| (Enable GATT 0x2902 Indications)
             |                                             |
             | <==== 14. Real-time Telemetry Ingest ======>| (Continuous 24h biometrics active)
```

### 7.4 Watch Face Dial Transmission Pipeline

Watch face installation is handled via a multi-stage streaming pipeline accommodating both pre-compiled binary dials (`.bin`) and user-customized photo watch faces (`DialCustom`):

```
+-----------------------------------------------------------------------------------------+
|                              WATCH FACE COMPILATION & FLASHING                          |
+-----------------------------------------------------------------------------------------+
| [A. Dial Asset Preparation]                                                             |
|    1. User selects dial in Flutter UI or uploads photo from gallery.                    |
|    2. Custom photo cropped via UCropActivity (e.g. 240x280 / 368x448 / 466x466 AMOLED). |
|    3. libjl_bmp_convert.so / libezipmy.so compresses bitmap into RGB565 / compressed RLE.|
|    4. Sifli coordinate engine (libsfwatchface.so) packs widget hands and time overlay.  |
|                                                                                         |
| [B. Pre-flight Handshake]                                                               |
|    5. Phone calls queryWatchDialLayout() to verify target watch storage capacity.       |
|    6. Phone sends sendWatchDialBackgroundInfo() with total binary byte length and CRC32.|
|    7. Watch returns OK; transitions BLE link into high-priority connection interval.    |
|                                                                                         |
| [C. Streaming & Error Handling]                                                         |
|    8. Chunked packet streaming via 244-byte MTU GATT Write Without Response.            |
|    9. UI reflects percentage progress via sendDialInstallProgress().                    |
|   10. In case of link drop, abortDialInstall() frees peripheral heap; retries at offset.|
+-----------------------------------------------------------------------------------------+
```

### 7.5 Over-The-Air (OTA) DFU Flows per Vendor

| Hardware Vendor | DFU Service Class | Native Support Library | Update Protocol & Safety Mechanism |
| :--- | :--- | :--- | :--- |
| **Realtek (RTL)** | `com.realsil.sdk.dfu.DfuService` | `libRtkAesJni.so` | Realtek Dual-Bank DFU; encrypted packet header validation; automatic fallback bank on flash failure |
| **FitCloud / TopStep**| `Dfu6621Service` | FitCloud SDK Core | Sifli/Nordic flash partition streaming; silent background flash transfer; device auto-reboots |
| **MoYoung (Sifli)**| `WatchConnection$Firmware` | MoYoung DFU Core | Multi-binary firmware update (Bootloader + App + UI Res + Bluetooth stack) |
| **Qiwo / HuaWo** | `HuaWoDfuService` | `libezipqw.so` | HTTP firmware delta download via `api.huawo-wear.com` followed by sequential GATT chunking |

### 7.6 BLE Reconnect & Fault-Tolerance Strategy

Smartwatches constantly cycle through out-of-range scenarios. The reconnect state machine ensures automatic silent reconnection:
1. **Exponential Backoff Reconnect:** On `onConnectionStateChange` returning `STATE_DISCONNECTED`, `DeviceBleService` waits 1s, 2s, 4s, 8s, up to a maximum 30s interval.
2. **Bluetooth Adapter Restart Observer:** Broadcast receiver monitors `BluetoothAdapter.ACTION_STATE_CHANGED`. When user turns Bluetooth ON, active scanning initiates immediately without waiting for the backoff timer.
3. **Ghost Bond Cleanup:** If GATT returns error code 133 (`GATT_ERROR`), driver invokes `refreshDeviceCache()` via Android reflection and recycles the `BluetoothGatt` client before re-establishing the physical link.

---

### Evidence Log
- Driver implementations verified via `base/META-INF/services/com.transsion.spi.devicemanager.device.IHealthDevice`: `IHealthDevice:1-47`
- 133 capability methods verified via `TransFlutterServiceNativeCallDevicemanagerPlugin$mSender$1$*.java`: `decompiled/sources/com/transsion/trans_flutter_service_native_call_devicemanager/:1-133`
- Realtek DFU service verified via `manifest.xml:533`: `com.realsil.sdk.dfu.DfuService`
- FitCloud DFU service verified via `manifest.xml:907`: `com.topstep.fitcloud.sdk.v2.dfu.Dfu6621Service`
- Native DFU cryptographic verification verified via `libRtkAesJni.so`: `arm/lib/arm64-v8a/libRtkAesJni.so`
