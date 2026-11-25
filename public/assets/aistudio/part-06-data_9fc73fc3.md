# Persistence Architecture, Room Schemas & IPC Contracts

### 6.1 Room Database Infrastructure

The local relational storage engine is built on AndroidX Room `2.5.0` backing SQLite with Write-Ahead Logging (WAL). The database instance configuration is declared in `HealthDataBase.kt` and initialized via `vr1.java`:

| Parameter | Observed Production Value | Architecture Constraint |
| :--- | :--- | :--- |
| **Database Name** | `HealthDataBase` | Stored in `/data/data/com.transsion.healthifit/databases/HealthDataBase` |
| **Current Schema Version**| `27` | Highest migration state implemented in `vr1.java` |
| **Identity Hash** | `884db9bc4260cdec2e631f304e208dbd` | Stored in `room_master_table`; checked by Room on database open |
| **Legacy Identity Hash** | `3d03c2c2491e3013124937d378e889d1` | Secondary validation hash in `HealthDataBase_Impl.java` |
| **Journal Mode** | `RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING` | High-throughput concurrent telemetry writes during live sports |
| **Threading Model** | Dedicated single-thread executor (`Executors.newSingleThreadExecutor()`) | Prevents write contention and SQLite database locked exceptions |
| **Entity Table Count** | 32 tables | 32 tables registered in `rz4` invalidation tracker |

### 6.2 Entity Schema Catalog (32 Tables)

Every table structure has been extracted directly from the compiled SQL definitions inside `HealthDataBase_Impl.java`:

| Table Name | Primary Key | Key Columns & SQLite Types | Domain & Semantics |
| :--- | :--- | :--- | :--- |
| `MotionRecordEntity` | `mStartTime` (INTEGER) | `type` (INT), `mDurationInSecond` (INT), `mTotalDistance` (INT), `mGpsTrackers` (TEXT), `mCalories` (INT), `mStepCount` (INT), `oneKmRecords` (TEXT), `openId` (TEXT) | Outdoor running/walking workouts with serialized GPS trajectory points |
| `StepEntity` | `time` (INTEGER) | `step` (INT), `openId` (TEXT), `stepSource` (INT) | Normalized daily phone/watch step time series |
| `WeightEntity` | `id` (INTEGER AUTO) | `time` (INT), `weight` (REAL), `openId` (TEXT) | User weight scale measurements and BMI telemetry |
| `WatchStepListEntity` | `time` (INTEGER) | `mac` (TEXT), `stepList` (TEXT), `complete` (INT), `deviceName` (TEXT), `openId` (TEXT), `did` (TEXT) | 15-minute raw interval step telemetry uploaded from smartwatch |
| `WatchSportListEntity` | `startTime` (INTEGER) | `endTime` (INT), `type` (INT), `steps` (INT), `distance` (INT), `calories` (INT), `heartRateList` (TEXT), `mac` (TEXT), `oneKmRecords` (TEXT), `sportPath` (TEXT) | Workouts recorded autonomously on watch hardware |
| `WatchHeartRateListEntity`| `time` (INTEGER) | `heartRateList` (TEXT), `mac` (TEXT), `complete` (INT), `interval` (INT), `openId` (TEXT), `did` (TEXT) | Continuous 24-hour heart rate samples (1–5 min interval) |
| `WatchSleepListEntity` | `time` (INTEGER) | `sleepList` (TEXT), `mac` (TEXT), `complete` (INT), `openId` (TEXT), `did` (TEXT) | Hypnogram sleep intervals (deep, light, REM, awake) |
| `WatchBloodOxygenEntity` | `time` (INTEGER) | `bloodOxygen` (INT), `mac` (TEXT), `openId` (TEXT), `did` (TEXT) | Daytime and nocturnal SpO2 percentage samples |
| `WatchConfigEntity` | `mac` (TEXT) | `callIn` (INT), `reject` (INT), `appListEnable` (INT), `appList` (TEXT), `zenMode` (INT), `missingCall` (INT) | Hardware configuration sync table (DND, app push list, call rules) |
| `WatchTempEntity` | `time` (INTEGER) | `temp` (INT), `mac` (TEXT) | Wrist skin temperature samples |
| `DataEntity` | `month` (TEXT) | `hasWeight` (INT), `hasPhoneStep` (INT), `hasWatchStep` (INT), `hasWatchHeartRate` (INT), `hasWatchSleep` (INT) | Monthly calendar bitmask tracking active metric days |
| `CloudUploadEntity` | `id` (INTEGER AUTO) | `uploadType` (INT), `payload` (TEXT), `status` (INT), `retryCount` (INT), `timestamp` (INT) | Cloud synchronization queue for offline telemetry packets |
| `DeviceHistoryEntity` | `mac` (TEXT) | `deviceName` (TEXT), `lastConnectTime` (INT), `firmwareVersion` (TEXT), `deviceType` (INT) | Bonded device metadata and connection timestamps |
| `City` | `cityId` (INTEGER) | `cityName` (TEXT), `country` (TEXT), `latitude` (REAL), `longitude` (REAL), `keyString` (TEXT) | Offline world clock and weather city directory |
| `WatchDownloadDialEntity`| `dialStoreId` (INTEGER)| `pid` (TEXT), `nickname` (TEXT), `thumbnail` (TEXT), `fileAddress` (TEXT), `styleName` (TEXT), `nativePath` (TEXT) | Installed and downloaded watch dial catalog |
| `DialUsedMappingEntity` | `mac` (TEXT) | `dialStoreId` (INT), `cloudDialThumb` (TEXT), `used` (INT), `dialIndex` (INT), `isInnerDial` (INT) | Active watch face slot mapping per connected watch |
| `ThreeCircleEntity` | `startDate` (INTEGER) | `steps` (TEXT), `calories` (TEXT), `durations` (TEXT), `stepGoal` (INT), `caloriesGoal` (INT), `complete` (INT) | Daily activity ring metrics (Move, Exercise, Stand) |
| `ThreeCircleGoalEntity` | `nowDay` (INTEGER) | `stepNumberOfCompliance` (INT), `caloriesGoal` (INT), `sportDurationGoal` (INT), `openId` (TEXT) | User goal configuration per target day |
| `ExtraSportEntity` | `startTime` (INTEGER) | `type` (INT), `endTime` (INT), `duration` (INT), `calories` (INT), `openId` (TEXT) | Manually entered fitness activities |
| `WholePlanEntity` | `mStartTime` (INTEGER)| `mEndTime` (INT), `mPlanDayList` (TEXT), `mTimeGoal` (INT), `mWeightGoal` (REAL), `mExerciseDay` (TEXT) | Multi-week training and weight loss programs |
| `MenstrualEntity` | `month` (TEXT) | `monthData` (TEXT), `complete` (INT), `openId` (TEXT) | Menstrual cycle forecasts, ovulation, and symptom logs |
| `PhoneHeartRateEntity` | `time` (INTEGER) | `value` (INT), `complete` (INT), `deviceName` (TEXT), `tagType` (INT), `openId` (TEXT) | Camera or phone-sensor measured pulse data |
| `PhoneBloodOxygenEntity`| `time` (INTEGER) | `value` (INT), `complete` (INT), `deviceName` (TEXT), `tagType` (INT), `openId` (TEXT) | Manually logged blood oxygen records |
| `HeartBloodTagEntity` | `tagType` (INTEGER) | `tagName` (TEXT), `openId` (TEXT) | Biometric context tags (Resting, Post-Workout, Fasting) |
| `AiWatchHistoryEntity` | `id` (INTEGER) | `taskId` (INT), `imgUrl` (TEXT), `path` (TEXT), `isRead` (INT), `pid` (TEXT), `dialStoreId` (INT) | Generative AI generated watch-face task queue and history |
| `WatchPressureEntity` | `date` (INTEGER) | `pressureList` (TEXT), `complete` (INT), `intervalTime` (INT), `openId` (TEXT), `did` (TEXT) | Daily stress/pressure 0–100 score intervals |
| `WatchHrvEntity` | `date` (INTEGER) | `hrvList` (TEXT), `complete` (INT), `intervalTime` (INT), `openId` (TEXT), `did` (TEXT) | Heart rate variability (rMSSD in milliseconds) time series |
| `PhoneHrvEntity` | `time` (INTEGER) | `value` (INT), `complete` (INT), `deviceName` (TEXT), `tagType` (INT), `openId` (TEXT) | Phone-derived HRV measurements |
| `PhonePressureEntity` | `time` (INTEGER) | `value` (INT), `complete` (INT), `deviceName` (TEXT), `tagType` (INT), `openId` (TEXT) | Phone-derived mental pressure estimates |
| `WatchNapListEntity` | `time` (INTEGER) | `napList` (TEXT), `mac` (TEXT), `complete` (INT), `openId` (TEXT), `did` (TEXT) | Afternoon nap sleep interval records |
| `PhoneTodoEntity` | `id` (INTEGER AUTO) | `periodId` (INT), `content` (TEXT), `status` (INT), `createTime` (INT), `remindTime` (INT) | Smartwatch synced to-do tasks and reminders |
| `AppConfigEntity` | `id` (INTEGER) | `uniqueId` (TEXT) | Global app instance configuration and installation UUID |

### 6.3 Database Migration Strategy (v1 -> v27)

`vr1.java` registers 52 bidirectional migrations covering versions 1 through 27. Key migration milestones include:
- **Migration(11, 12):** Added `did` (Device ID) column across watch telemetry tables to support multi-watch switching without data collision.
- **Migration(13, 14):** Normalized phone-based biometric tables (`PhoneHeartRateEntity`, `PhoneBloodOxygenEntity`).
- **Migration(17, 18):** Added `ThreeCircleEntity` and `ThreeCircleGoalEntity` supporting the modernized Apple-style 3-circle activity ring dashboard.
- **Migration(18, 19):** Added `oneKmRecords` and `sportPath` to `WatchSportListEntity` for pacing splits and watch GPS trajectory syncing.
- **Migration(19, 20):** Added `WatchHrvEntity` and `WatchPressureEntity` introducing stress and recovery monitoring.
- **Migration(26, 27):** Added `missingCall` notification flag to `WatchConfigEntity` and added `AiWatchHistoryEntity` for generative watch face logging.

### 6.4 DataContentProvider IPC Contract

`DataContentProvider` exposes a standard query interface for external system widgets and Transsion OS launcher cards:

| Match ID | URI Match Pattern | Response Key / Format | Functional Purpose |
| :--- | :--- | :--- | :--- |
| `1` | `content://com.health.provider.ifit/birthday` | `birthday` -> String | Returns user birthdate for age calculations |
| `2` | `content://com.health.provider.ifit/hasPermission`| `hasPermission` -> Boolean | Verifies whether launcher holds read permission |
| `3` | `content://com.health.provider.ifit/stepCount` | `stepCount` -> Integer | Current day aggregate step total |
| `4` | `content://com.health.provider.ifit/stepGoal` | `stepGoal` -> Integer | User configured daily step target |
| `5` | `content://com.health.provider.ifit/stepCalories` | `stepCalories` -> Integer | Cumulative calories expended from steps |
| `6` | `content://com.health.provider.ifit/sportIsInMotion`| `sportIsInMotion` -> Boolean | Returns true if an outdoor run/walk is currently active |
| `7` | `content://com.health.provider.ifit/login_id` | `login_id` -> String | Current authenticated Palm ID openId |
| `8` | `content://com.health.provider.ifit/login_status`| `login_status` -> Integer | Session status (0: logged out, 1: logged in) |
| `9` | `content://com.health.provider.ifit/iot_health` | `iot_health` -> JSON Object | Full composite biometric telemetry package |

### 6.5 Cloud Upload & Telemetry Sync State Machine

Cloud synchronization operates on an offline-first staged queue architecture backed by `CloudUploadEntity`:

```
[Local Biometric Ingest]
       |
       v
[Insert/Update Room DB] ---> [Queue in CloudUploadEntity (status=PENDING)]
                                                |
                                                v
                              [WorkManager SyncWorker Triggered]
                                                |
                               +----------------+----------------+
                               | Check Network Availability     |
                               +----------------+----------------+
                                                |
                                       [Network Available]
                                                v
                                 [Batch Payload Serialization]
                                                |
                                                v
                                  [HTTPS POST to NebulaLive]
                                                |
                         +----------------------+----------------------+
                         |                                             |
                  [HTTP 200 OK]                                  [HTTP Error / 5xx]
                         |                                             |
                         v                                             v
              [Delete Queue Entry]                           [Increment retryCount]
              [Mark Entity Synced]                           [Exponential Backoff]
```

---

### Evidence Log
- Room database implementation, tables, identity hashes verified via `HealthDataBase_Impl.java`: `decompiled/sources/com/transsion/common/db/HealthDataBase_Impl.java:308-320, 811-820`
- Room version (27) and migration chain verified via `vr1.java`: `decompiled/sources/defpackage/vr1.java:40-110`
- Migration definitions verified in `HealthDataBase.java`: `decompiled/sources/com/transsion/common/db/HealthDataBase.java:60-200, 1059-1120`
- Content provider routes and columns verified via `DataContentProvider.java`: `decompiled/sources/com/transsion/common/db/DataContentProvider.java:72-130`
