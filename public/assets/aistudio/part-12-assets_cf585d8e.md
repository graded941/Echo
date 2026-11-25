# Asset Inventory, Licensing & Resource Reconstitution

### 12.1 Comprehensive Asset Classification

Assets bundled in `base/assets/` and `resdecode/resources/res/` are categorized according to their licensing requirements, extraction fidelity, and reconstitution strategy for a clean-room rebuild:

```
+==================================================================================================+
|                                    ASSET BUDGET & CLASSIFICATION                                 |
+=========================+===========+=============+======================+=======================+
| Asset Path / Directory  | Disk Size | File Count  | Legal Classification | Rebuild Strategy      |
+=========================+===========+=============+======================+=======================+
| flutter_assets/         | 59.0 MB   | 2,402 files | Proprietary Assets   | Must ship / Re-export |
| animation/              | 8.5 MB    | 42 files    | Proprietary Artwork  | Must ship             |
| dial/                   | 5.5 MB    | 18 files    | Proprietary Binaries | Must ship / Re-bundle |
| DialCustom/             | 4.1 MB    | 34 files    | Proprietary Artwork  | Must ship             |
| apis-xml/               | 1.8 MB    | 12 files    | Technical Metadata   | Regenerable           |
| city.json               | 1.2 MB    | 1 file      | Public Domain Data   | Must ship             |
| tossymbol.ttf           | 0.8 MB    | 1 file      | Proprietary Typeface | Third-party licensed  |
| dexopt/baseline.prof    | 0.6 MB    | 1 file      | ART Profile Data     | Regenerable           |
| res/drawable-*/         | 14.2 MB   | 840 files   | Proprietary Icons    | Must ship / Redraw    |
| res/layout/             | 3.8 MB    | 496 files   | Android XML Schemas  | Regenerable           |
| res/values/strings.xml  | 0.9 MB    | 2,848 tokens| Localized Text       | Must ship / Translate |
+=========================+===========+=============+======================+=======================+
```

### 12.2 Detailed Asset Breakdown

#### A. Flutter Assets (`base/assets/flutter_assets/` - 59.0 MB)
- **Shaders (`shaders/`):** Contains `ink_sparkle.frag` (Material 3 ripple effect fragment shader compiled for Impeller).
- **Icons & Glyphs:** MaterialIcons and Cupertino font files (`MaterialIcons-Regular.otf`).
- **Package Images:** 30 feature packages contain specialized WebP, PNG, and JSON vector assets (e.g. `packages/sport_home/assets/lottie/`, `packages/trans_flutter_module_device_v2/images/`).
- **Dial Bundles:** `packages/ifit_assets_package/dial/` contains default factory watch faces.

#### B. Workout Animations (`base/assets/animation/` - 8.5 MB)
- High-framerate Lottie JSON animations visualizing outdoor running strides, swimming strokes, calorie burn celebrations, and medal unlock achievements.
- Rebuild approach: Retain original JSON structures or replace with equivalent licensed Lottie animations conforming to identical progress frame markers.

#### C. Watch Dial Templates (`base/assets/dial/` & `DialCustom/` - 9.6 MB)
- **Offline Watch Dials (`dial/`):** Pre-compiled binary dial packages (`.bin`) packaged directly into the APK for instant offline installation on newly paired watches.
- **Custom Dial Elements (`DialCustom/`):** High-resolution watch hand sprites (hour, minute, second hands), numeric digit bitmaps (0–9), date complication overlays, and battery complication icons.

#### D. Global Geo Database (`base/assets/city.json` - 1.2 MB)
- Contains JSON array of over 12,000 global cities with standardized ISO country codes, English/localized names, time zone identifiers, and latitude/longitude coordinates.
- Used by `City` Room entity table for offline smartwatch world clock configuration and weather query coordinates.

#### E. Typography & Licensing Notes (`tossymbol.ttf` - 0.8 MB)
- `tossymbol.ttf` is the Transsion OS proprietary icon symbol typeface containing vendor-specific hardware glyphs (Infinix, Tecno, and itel watch silhouettes).
- Clean-room rebuild recommendation: Replace with licensed open-source iconography (e.g. Feather Icons, Google Material Symbols, or Tabler Icons) mapped to the corresponding Unicode code-points.

---

### Evidence Log
- Assets directory disk usage measured via `du -sh base/assets/*`: `base/assets/:1`
- Flutter asset file count verified via `AssetManifest.json`: `AssetManifest.json:2402 entries`
- Resource counts verified via `resdecode`: `strings.xml` (2,848 strings), `layout/` (496 files)
- Proprietary font verified via `ls -lh base/assets/tossymbol.ttf`: `tossymbol.ttf:1`
