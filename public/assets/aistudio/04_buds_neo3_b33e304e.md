# ECHO Implementation Spec — 04: itel Buds Neo 3 Integration

**Pillar:** 4 of 8 | **Feasibility:** 🟡 MODERATE | **Effort:** ~8 days (Phase 1) | **Priority:** P2

---

## 1. Overview

The itel Buds Neo 3 are TWS (True Wireless Stereo) earbuds connecting to the itel P55 via Bluetooth Classic (A2DP + HFP) and optionally BLE for battery/GATT services. The existing iPulse codebase has **zero earbuds management** — this is a fully net-new ECHO feature.

ECHO integrates the Buds Neo 3 in two phases:
- **Phase 1:** Device pairing, connection status, battery monitoring, call audio routing (all using standard Android APIs — HIGH confidence)
- **Phase 2:** EQ profiles, touch gesture configuration (requires GATT reverse engineering — confidence UNKNOWN)

---

## 2. itel P55 Bluetooth Capabilities

| Capability | API | Android Version |
|---|---|---|
| BT Classic discovery | `BluetoothAdapter.startDiscovery()` | API 5+ |
| A2DP profile | `BluetoothProfile.A2DP` | API 11+ |
| HFP/HSP profile | `BluetoothProfile.HEADSET` | API 11+ |
| Companion Device pairing | `CompanionDeviceManager` | API 26+ |
| Battery level (Action) | `BluetoothDevice.ACTION_BATTERY_LEVEL_CHANGED` | API 33 |
| Battery level (GATT) | `BluetoothGattCharacteristic` UUID `0x2A19` (Battery Service `0x180F`) | API 18+ |
| BLE scan for GATT | `BluetoothLeScanner` | API 21+ |

itel P55 runs Android 14 (API 34) — all APIs above are available.

---

## 3. CompanionDeviceManager Pairing

```kotlin
// echo-earbuds/src/main/kotlin/com/echo/earbuds/EchoEarbudsManager.kt

class EchoEarbudsManager @Inject constructor(
    private val context: Context,
    private val tokenStore: EchoEarbudsTokenStore
) {
    private val cdm = context.getSystemService(CompanionDeviceManager::class.java)

    fun startPairing(activity: Activity, resultLauncher: ActivityResultLauncher<IntentSenderRequest>) {
        val deviceFilter = BluetoothDeviceFilter.Builder()
            .setNamePattern(Pattern.compile(".*itel.*|.*Buds.*Neo.*", Pattern.CASE_INSENSITIVE))
            .build()

        val request = AssociationRequest.Builder()
            .addDeviceFilter(deviceFilter)
            .setSingleDevice(false)  // allow any matching device
            .build()

        cdm.associate(request, object : CompanionDeviceManager.Callback() {
            override fun onDeviceFound(chooserLauncher: IntentSender) {
                val intentSenderRequest = IntentSenderRequest.Builder(chooserLauncher).build()
                resultLauncher.launch(intentSenderRequest)
            }
            override fun onFailure(error: CharSequence?) {
                // Report pairing failure to UI
            }
        }, null)
    }

    fun onPairingResult(device: BluetoothDevice) {
        tokenStore.savePairedDevice(device.address, device.name)
        BudsConnectionMonitor.attach(device)
    }

    fun getPairedBuds(): BluetoothDevice? {
        val address = tokenStore.getPairedAddress() ?: return null
        return BluetoothAdapter.getDefaultAdapter()?.getRemoteDevice(address)
    }

    fun forgetBuds() {
        tokenStore.clear()
    }
}
```

---

## 4. Connection State Monitor

```kotlin
// echo-earbuds/src/main/kotlin/com/echo/earbuds/BudsConnectionMonitor.kt

class BudsConnectionMonitor @Inject constructor(
    context: Context
) {
    sealed class BudsState {
        object Disconnected : BudsState()
        data class Connected(val a2dpActive: Boolean, val hfpActive: Boolean) : BudsState()
        data class Playing(val trackTitle: String, val artist: String) : BudsState()
    }

    private val _state = MutableStateFlow<BudsState>(BudsState.Disconnected)
    val state: StateFlow<BudsState> = _state

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)
                    if (state == BluetoothProfile.STATE_CONNECTED) {
                        _state.value = BudsState.Connected(a2dpActive = true, hfpActive = false)
                    } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                        _state.value = BudsState.Disconnected
                    }
                }
                BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)
                    val current = _state.value
                    if (current is BudsState.Connected && state == BluetoothProfile.STATE_CONNECTED) {
                        _state.value = current.copy(hfpActive = true)
                    }
                }
                BluetoothDevice.ACTION_BATTERY_LEVEL_CHANGED -> {
                    val level = intent.getIntExtra(BluetoothDevice.EXTRA_BATTERY_LEVEL, -1)
                    if (level >= 0) EventBus.post(BudsBatteryEvent(level))
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_BATTERY_LEVEL_CHANGED)
        }
        context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
    }
}
```

---

## 5. Battery GATT Client

If `ACTION_BATTERY_LEVEL_CHANGED` is not broadcast (device-dependent), use standard GATT Battery Service:

```kotlin
// echo-earbuds/src/main/kotlin/com/echo/earbuds/BatteryGattClient.kt

object BatteryGattService {
    val SERVICE_UUID: UUID = UUID.fromString("0000180F-0000-1000-8000-00805F9B34FB")
    val CHARACTERISTIC_UUID: UUID = UUID.fromString("00002A19-0000-1000-8000-00805F9B34FB")
}

class BatteryGattClient(
    private val device: BluetoothDevice,
    private val context: Context
) : BluetoothGattCallback() {

    private var gatt: BluetoothGatt? = null
    var batteryLevel: Int = -1
        private set

    fun connect() {
        gatt = device.connectGatt(context, false, this, BluetoothDevice.TRANSPORT_LE)
    }

    override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
        if (newState == BluetoothProfile.STATE_CONNECTED) g.discoverServices()
        else if (newState == BluetoothProfile.STATE_DISCONNECTED) g.close()
    }

    override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
        val svc = g.getService(BatteryGattService.SERVICE_UUID) ?: return
        val char = svc.getCharacteristic(BatteryGattService.CHARACTERISTIC_UUID) ?: return
        g.readCharacteristic(char)
    }

    override fun onCharacteristicRead(
        g: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        status: Int
    ) {
        if (characteristic.uuid == BatteryGattService.CHARACTERISTIC_UUID && status == BluetoothGatt.GATT_SUCCESS) {
            batteryLevel = value[0].toInt() and 0xFF
            EventBus.post(BudsBatteryEvent(batteryLevel))
        }
    }

    fun disconnect() { gatt?.disconnect(); gatt?.close() }
}
```

---

## 6. Phase 2 — EQ & Touch Gesture Reverse Engineering Plan

> [!WARNING]
> Phase 2 features depend on proprietary GATT services unique to the itel Buds Neo 3. These are not documented and require BLE sniffing.

### 6.1 BLE Sniffing Procedure

1. Enable Bluetooth HCI snoop log on itel P55: `Settings → Developer Options → Enable Bluetooth HCI snoop log`
2. Install itel earbuds companion app (if one exists on Play Store)
3. Perform touch gesture config and EQ changes in the companion app
4. Pull HCI log: `adb pull /data/misc/bluetooth/logs/btsnoop_hci.log`
5. Analyse in Wireshark with Bluetooth plugin: filter `btle` protocol
6. Identify GATT write commands to unknown service UUIDs

### 6.2 Likely GATT Structure (Hypothesis)

| Service | Characteristic | Purpose |
|---|---|---|
| `0x180F` | `0x2A19` | Standard battery level (confirmed available) |
| Unknown (vendor) | Unknown | EQ profile select (hypothesis: 1-byte command) |
| Unknown (vendor) | Unknown | Touch gesture mapping (hypothesis: 2-byte bitmap) |

### 6.3 EchoGattScanner (Phase 2 Discovery Tool)

```kotlin
// Built-in GATT service enumerator (run once after pairing)
class EchoGattScanner(device: BluetoothDevice, context: Context) : BluetoothGattCallback() {
    override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
        g.services.forEach { service ->
            Log.d("ECHO_GATT", "Service: ${service.uuid}")
            service.characteristics.forEach { char ->
                Log.d("ECHO_GATT", "  Char: ${char.uuid} props:${char.properties}")
            }
        }
    }
}
```

Run this during development to enumerate all Buds Neo 3 GATT services and build Phase 2 protocol map.

---

## 7. Watch → Earbuds Media Control

The existing `MusicControlManager` from iPulse controls phone audio volume via `AudioManager.STREAM_MUSIC`. ECHO extends this to explicitly route to Buds Neo 3 when connected:

```kotlin
// Extended MusicControlManager with routing awareness

class EchoMusicController @Inject constructor(
    private val audioManager: AudioManager,
    private val budsMonitor: BudsConnectionMonitor
) {
    fun volumeUp() {
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
    }

    fun volumeDown() {
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
    }

    fun isBudsConnected(): Boolean =
        budsMonitor.state.value is BudsConnectionMonitor.BudsState.Connected

    // Route audio explicitly to Buds Neo 3 if connected
    fun ensureAudioRoutedToBuds() {
        val budsDevice = EchoEarbudsManager.getInstance().getPairedBuds() ?: return
        audioManager.setCommunicationDevice(
            audioManager.availableCommunicationDevices.firstOrNull {
                it.address == budsDevice.address
            } ?: return
        )
    }
}
```

---

## 8. UI Screens

| Screen | Description |
|---|---|
| **EarbudsHomeCard** | Persistent card on home screen: left/right battery %, connection status, music playing indicator |
| **EarbudsPairScreen** | CompanionDeviceManager chooser integration |
| **EarbudsSettingsScreen** | EQ profiles (Phase 2), touch gesture map (Phase 2), auto-answer calls toggle |
| **EarbudsDebugScreen** (Dev only) | GATT service dump for Phase 2 reverse engineering |

---

## 9. Required Permissions

```xml
<uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30"/>
<uses-permission android:name="android.permission.BLUETOOTH_SCAN"
    android:usesPermissionFlags="neverForLocation"/>
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT"/>
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE"/>
```

---

## 10. Evidence Log

| Claim | Source | Evidence |
|---|---|---|
| iPulse has zero earbuds management | Full codebase scan | No `BluetoothA2dp`, `BluetoothHeadset`, or `CompanionDeviceManager` references in decompiled source |
| `MusicControlManager` controls phone AudioManager only | `MusicControlManager.java` | `audioManager.setStreamVolume(3, ...)` — stream 3 = `STREAM_MUSIC` on phone |
| Standard GATT Battery Service UUID `0x180F` | Bluetooth SIG specification | Universally standardised |
| `ACTION_BATTERY_LEVEL_CHANGED` available API 33 | Android docs | `BluetoothDevice.EXTRA_BATTERY_LEVEL` |
| itel P55 is Android 14 | Context summary | Confirmed Transsion OS Android 14 base |
