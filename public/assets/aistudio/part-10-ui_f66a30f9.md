# UI Architecture, Screen Topology, Design Tokens & Theming

### 10.1 Primary Navigation Tabs & Host Screens

The user experience centers around a 4-tab bottom navigation model hosted inside `MainActivity`. Each tab binds to a native fragment provider resolved via `com.transsion.spi.common.MainFragmentSpi`:

```
+==================================================================================================+
|                                    MAIN UI TAB ARCHITECTURE                                      |
+======================+======================+======================+=============================+
| TAB 1: HEALTH        | TAB 2: SPORT         | TAB 3: DEVICE        | TAB 4: MINE                 |
+======================+======================+======================+=============================+
| SPI: HealthFragment  | SPI: SportFragment   | SPI: DeviceFragment  | SPI: MineFragment           |
| Engine: Hybrid/Flut  | Engine: Native View  | Engine: Flutter V2   | Engine: Hybrid              |
+----------------------+----------------------+----------------------+-----------------------------+
| Key Capabilities:    | Key Capabilities:    | Key Capabilities:    | Key Capabilities:           |
| - 3-Circle Activity  | - Outdoor GPS HUD    | - Watch Battery Meter| - Personal Profile          |
| - Heart Rate Graph   | - Cadence & Pace Gau | - Dial Market Store  | - Daily Fitness Goals       |
| - Sleep Staging Bar  | - Auto-Pause Dialog  | - Notification Sync  | - Unit Preferences          |
| - SpO2 & Stress Card | - Offline Map Packs  | - DND & Alarm Config | - Strava / Google Account   |
| - Menstrual Forecast | - Workout History    | - OTA Firmware Check | - Privacy & App Update      |
+======================+======================+======================+=============================+
```

### 10.2 Layout Inventory Grouped by Prefix (496 Layouts)

All 496 native layout resources inside `resdecode/resources/res/layout/` are structured by functional namespace prefixes:

| Layout Prefix | Layout Count | Sample Layout Resources | Functional Area |
| :--- | :--- | :--- | :--- |
| `sport_` | 74 | `sport_activity_running.xml`, `sport_record_detail.xml`, `sport_gps_signal.xml` | Workout recording screens, cadence charts, pause sheets |
| `device_` | 88 | `device_activity_main.xml`, `device_dial_market.xml`, `device_alarm_item.xml` | Smartwatch settings, dial downloader, hardware controls |
| `health_` | 56 | `health_fragment_overview.xml`, `health_card_heart_rate.xml`, `health_dialog_sp02.xml`| Biometric summary cards, daily rings, sleep hypnograms |
| `mine_` | 42 | `mine_activity_profile.xml`, `mine_unit_setting.xml`, `mine_feedback.xml` | User account settings, height/weight sliders, about page |
| `activity_` | 68 | `activity_main.xml`, `activity_splash.xml`, `activity_guide.xml` | Top-level activity window frames and container layouts |
| `fragment_` | 38 | `fragment_health_dashboard.xml`, `fragment_sport_list.xml` | Fragment sub-viewport viewports |
| `dialog_` | 46 | `dialog_confirm.xml`, `dialog_permission_request.xml`, `dialog_ota_progress.xml` | Material alert dialogs, firmware warning modals |
| `item_` | 62 | `item_sport_record.xml`, `item_dial_grid.xml`, `item_notification_app.xml` | RecyclerView row viewholders |
| `os_` / `xn_` | 22 | `os_bottom_sheet.xml`, `xn_login_activity.xml` | Transsion OS base widgets and Palm ID login views |

### 10.3 Design System Tokens & Dark-First Theming

The application styling is strictly governed by `Theme.Health` defined in `styles.xml`. The app enforces an AMOLED dark-first color hierarchy:

```
+==================================================================================================+
|                               DESIGN TOKENS (THEME.HEALTH)                                       |
+==================================================================================================+
| Palette Role               | Hex Value / Token       | Usage & Visual Context                    |
+----------------------------+-------------------------+-------------------------------------------+
| Window Background          | #000000 (Pure Black)    | Battery conservation on AMOLED displays   |
| Surface Background Dark    | #121212 / #1A1A1A       | Card background and modal surface fill    |
| Surface Highlight          | #242424 / #2C2C2C       | Elevated cards, selected list tiles       |
| Primary Brand Accent       | #FF8000 (primary_F80)   | Active tabs, action buttons, sport accent |
| Secondary Accent           | #00D1FF (Cyan)          | Blood oxygen (SpO2) and sleep indicators  |
| Success / Vital Accent     | #00E676 (Green)         | GPS strong signal, completed goal rings   |
| Warning / Stress Accent    | #FFD600 (Yellow/Amber)  | Moderate stress, battery alert            |
| Critical / Heart Rate      | #FF3D00 (Red)           | High heart rate warning, battery critical |
| Text Primary               | #FFFFFF (100% White)    | Primary titles, large numeric telemetry   |
| Text Secondary             | #9E9E9E (Neutral Grey)  | Units (bpm, kcal, km), timestamps         |
| Text Disabled              | #545454 (Dark Grey)     | Inactive controls, secondary prompt labels |
+----------------------------+-------------------------+-------------------------------------------+
| Typography Baseline        | Roboto / Roboto-Medium  | High legibility during high-vibration run |
+==================================================================================================+
```

### 10.4 Screen Map (Tab -> Viewport -> Host Architecture)

```
[MainActivity Host Window]
  |
  +---> [Tab 1: Health Dashboard]
  |       |---> Native Container Layout (activity_main.xml)
  |       |---> 3-Circle Summary Header (Custom Canvas / Lottie animation)
  |       |---> Telemetry Card List (RecyclerView):
  |               |---> Heart Rate (Real-time graph + resting BPM)
  |               |---> Sleep (Total duration + deep/light/REM breakdown)
  |               |---> Blood Oxygen (Latest percentage + 24h trend)
  |               |---> Pressure / Stress (0-100 gauge score)
  |               |---> Weight & BMI (Latest weigh-in + target delta)
  |               +---> Menstrual Prediction (Phase indicator pill)
  |
  +---> [Tab 2: Sport Mode]
  |       |---> Sport Type Selector (Outdoor Run, Outdoor Walk, Outdoor Cycle, Indoor Run)
  |       |---> Mapbox / Google Maps Route Viewport (sport_activity_running.xml)
  |       |---> Large Font Live HUD: Distance, Elapsed Duration, Current Pace, Heart Rate Zone
  |       |---> Slide-to-Unlock / Long-Press Pause Controller
  |       +---> Workout Completed Summary: Elevation Gain, Split Times, Calorie Expenditure
  |
  +---> [Tab 3: Device Center]
  |       |---> Connected Watch Device Banner (Model Name, Bluetooth RSSI, Battery Bar)
  |       |---> Watch Dial Store Showcase (Horizontal Dial Carousel)
  |       |---> Device Settings Menu:
  |               |---> Notification App Filter (Multi-select list)
  |               |---> Alarm Clock Scheduler (Vibration profile editor)
  |               |---> World Clock & Weather City Preferences
  |               |---> Quick SMS Reply Editor
  |               +---> Firmware Update (OTA DFU Progress Screen)
  |
  +---> [Tab 4: Mine / Profile Center]
          |---> User Profile Card (Avatar, Nickname, Palm ID openId)
          |---> Fitness Target Settings (Daily Steps, Active Calories, Workout Minutes)
          |---> Third-Party Cloud Sync (Strava OAuth connect, Google Fit toggle)
          |---> About iPulse (App version 5.9.1.17, Privacy Policy, Terms of Service)
          +---> Developer Options (Shell Mode override: AUTO / FORCE_V1 / FORCE_V2)
```

---

### Evidence Log
- Layout directory count (496 files) verified via `resdecode/resources/res/layout/`: `layout/:1-496`
- Dark theme tokens verified via `styles.xml`: `resdecode/resources/res/values/styles.xml:Theme.Health`
- Primary color token `primary_F80` verified via `colors.xml`: `resdecode/resources/res/values/colors.xml:primary_F80`
- Main tab fragment providers verified via `base/META-INF/services/com.transsion.spi.common.MainFragmentSpi`: `MainFragmentSpi:1-4`
