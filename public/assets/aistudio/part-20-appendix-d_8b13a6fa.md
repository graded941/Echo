# Appendix D — Technical Glossary & System Terminology

### D.1 Architecture & Engineering Glossary

| Term / Acronym | Full Name / Expansion | Architectural Definition & System Context |
| :--- | :--- | :--- |
| **AOT** | Ahead-Of-Time Compilation | Compilation paradigm where Dart code is compiled into native ARM64 machine code (`libapp.so`) prior to execution, stripping high-level AST metadata. |
| **BLE** | Bluetooth Low Energy | 2.4 GHz wireless personal area network technology operating under Bluetooth 5.0+ standards used for continuous smartwatch communication. |
| **CCCD** | Client Characteristic Configuration Descriptor | Standardized GATT attribute descriptor (`UUID: 0x2902`) written by the Android client to enable asynchronous peripheral notifications and indications. |
| **DFU** | Device Firmware Upgrade | Over-The-Air (OTA) binary protocol used to flash new operating system firmware partitions directly to smartwatch flash memory. |
| **GATT** | Generic Attribute Profile | Hierarchical BLE communication specification establishing Services, Characteristics, and Descriptors over an Attribute Protocol (ATT) connection. |
| **GSLB** | Global Server Load Balancing | Distributed traffic routing architecture resolving regional API hostnames based on client IP geolocation and mobile SIM carrier codes (MCC/MNC). |
| **HRV** | Heart Rate Variability | Variation in beat-to-beat intervals (RR intervals), mathematically computed via Root Mean Square of Successive Differences (rMSSD) to score autonomic stress. |
| **Hypnogram** | Sleep Stage Graph | Time-series categorical classification of sleep phases (Deep, Light, REM, Awake) derived from smartwatch photoplethysmography (PPG) and accelerometer sensors. |
| **MTU** | Maximum Transmission Unit | Maximum byte payload accommodated within a single physical Bluetooth packet; negotiated up to 247 bytes in iPulse to accelerate watch dial streaming. |
| **ODM** | Original Design Manufacturer | Third-party hardware manufacturers (MoYoung, TopStep, Qiwo, CRP) designing and fabricating smartwatches branded by Transsion (Infinix, Tecno, itel). |
| **Palm ID** | Palm Identity Platform | Transsion proprietary Single Sign-On (SSO) account infrastructure managing OAuth2 tokens, user profiles, and cloud synchronization permissions. |
| **Pigeon** | Flutter Pigeon Code Generator | Command-line tool generating type-safe, bidirectional communication wrappers between Flutter and host platforms, avoiding raw string-based method channels. |
| **QJS** | QuickJS Engine | Lightweight, embeddable JavaScript engine used in specific wearable platforms for dynamic watch face widget script evaluation. |
| **Room Master Table** | `room_master_table` | Internal SQLite table created by AndroidX Room holding the unique 128-bit schema identity hash (`884db9bc4260cdec2e631f304e208dbd`). |
| **SPI** | Service Provider Interface | Standard Java plugin mechanism (`java.util.ServiceLoader`) resolving concrete module implementations via metadata files declared under `META-INF/services/`. |
| **SpO2** | Peripheral Capillary Oxygen Saturation | Estimation of blood oxygen percentage measured via multi-wavelength optical sensors on the smartwatch wrist module. |
| **VAP** | Video Animation Player | Hardware-accelerated video rendering engine decoding video clips with dedicated alpha-channel transparency masks for high-performance UI effects. |
| **WAL** | Write-Ahead Logging | High-concurrency SQLite journaling mode allowing simultaneous database reads and non-blocking background writes during live workout tracking. |

---

### Evidence Log
- Technical terminology verified across decompiled classes in `com.transsion.*`, `manifest.xml`, and native ELF shared libraries.
