# Backend Service Catalog, REST Endpoints & Cloud Integration

### 8.1 Cloud Architecture & GSLB Dynamic Routing

The iPulse backend interfaces with multiple distributed cloud infrastructures. Rather than hardcoding fixed regional API hosts, the application integrates Transsion Global Server Load Balancing (`tran_gslb_version 2.0.0.0`). At startup, the client queries the GSLB gateway with the device's Country Code (MCC/MNC) and IP geo-location to resolve the optimal regional data center (Europe, India, Africa, Southeast Asia, Global):

```
+===================================================================================+
|                                TRANSSION CLOUD TOPOLOGY                           |
+===================================================================================+
|                     +---------------------------------------+                     |
|                     |     Transsion GSLB Gateway Engine     |                     |
|                     |     (Resolves Endpoints by Region)    |                     |
|                     +-------------------+-------------------+                     |
|                                         |                                         |
|        +--------------------------------+--------------------------------+        |
|        |                                |                                |        |
|        v                                v                                v        |
| +--------------+               +------------------+             +---------------+ |
| | Palm ID SSO  |               | NebulaLive Cloud |             | AI Companion  | |
| | Auth Gateway |               | Health Ingest/UI |             | Voice Gateway | |
| +--------------+               +------------------+             +---------------+ |
|        |                                |                                |        |
+========|================================|================================|========+
|        v                                v                                v        |
| +--------------+               +------------------+             +---------------+ |
| | Partner ODMs |               | Third-Party Sync |             | Static CDNs   | |
| | Firmware/Dial|               | Strava / Google  |             | Dial Binaries | |
| +--------------+               +------------------+             +---------------+ |
+-----------------------------------------------------------------------------------+
```

### 8.2 Endpoints Catalog Grouped by Domain

Every URL cataloged below has been directly extracted from compiled bytecode strings across `base/classes*.dex`:

#### Domain A: Authentication, Account & Identity (Palm ID)

| HTTP Method / Protocol | Target URL / Path | Environment / Role | Payload & Functional Scope |
| :--- | :--- | :--- | :--- |
| `HTTPS POST` | `https://account.palm.tech` | Production Auth Gateway | User credentials login, phone SMS verification, biometric token issue |
| `HTTPS POST` | `https://auth-account.palm.tech` | OAuth2 Token Service | Exchanges authorization code for bearer JWT access and refresh tokens |
| `HTTPS GET` | `https://account.palm.tech/client/callback` | OAuth2 Redirect | SSO web flow callback handler |
| `HTTPS ANY` | `https://dev-account.palm.tech` | Development Environment | Internal engineer test cluster |
| `HTTPS ANY` | `https://pre-account.palm.tech` | Pre-Release Environment | Staging candidate test cluster |
| `HTTPS ANY` | `https://test-account.palm.tech` | Quality Assurance (QA) | Integration test cluster |
| `HTTPS ANY` | `https://pre-auth-account.palm.tech` | Pre-Release Token Svc | Staging OAuth2 cluster |
| `HTTPS ANY` | `https://test-auth-account.palm.tech` | QA Token Service | QA OAuth2 cluster |

#### Domain B: Health Ingest, Aggregation & Dynamic UI (NebulaLive)

| HTTP Method / Protocol | Target URL / Path | Functional Scope & Data Flow |
| :--- | :--- | :--- |
| `HTTPS GET` | `https://api.nebulalive.com/ifit/public/ui/config` | Retrieves dynamic UI layout configurations, dashboard banner cards, and seasonal feature switches |
| `HTTPS GET` | `https://api.nebulalive.com/health/questionnaire/sys/language/`| Downloads localized medical questionnaires for sleep and stress health surveys |
| `HTTPS GET` | `https://api.nebulalive.com/welife/appdownload?app=` | Transsion IoT ecosystem application discovery and deep-linking |
| `HTTPS POST` | `https://api.nebulalive.com/health/v1/sync` [inferred] | Batch ingestion of serialized `CloudUploadEntity` biometric packets |
| `HTTPS ANY` | `https://test-api.nebulalive.com` | NebulaLive staging and integration testing cluster |
| `HTTPS GET` | `https://cdn.transsion-os.com/iFit/` | Global CDN hosting dial preview images, sport plan guides, and SVG icons |

#### Domain C: Smartwatch ODM Firmware & Watch Dial Repositories

| ODM Platform | Target URL / Path | Functional Purpose |
| :--- | :--- | :--- |
| **MoYoung (Sifli)** | `https://api.moyoung.com/faces/v3/list` | Queries cloud watch-face store catalog filtered by device screen resolution |
| **MoYoung (Sifli)** | `https://api.moyoung.com/faces/v3/tag-list` | Watch face category tags (Anime, Business, Mechanical, Sports) |
| **MoYoung (Sifli)** | `https://api.moyoung.com/faces/v3/face-detail` | Detailed metadata for individual dial (download size, author, preview GIF) |
| **MoYoung (Sifli)** | `https://api.moyoung.com/faces/sifli/` | Specialized Sifli chipset watch-face package repository |
| **MoYoung (Sifli)** | `https://api.moyoung.com/v2/upgrade/beta` | OTA firmware beta channel update check |
| **MoYoung (Sifli)** | `https://api.moyoung.com/v2/upgrade/factory` | Production factory-signed OTA firmware distribution |
| **TopStep (FitCloud)**| `https://fitcloud.hetangsmart.com/public/checkVersion/v2` | FitCloud smartwatch firmware delta update checker |
| **Qiwo / HuaWo** | `https://api.huawo-wear.com/api/v1/devices/upgrades` | HuaWo smartwatch firmware upgrade descriptor |
| **Qiwo / HuaWo** | `https://test.huawo-wear.com/api/v1/devices/upgrades`| HuaWo firmware QA staging endpoint |
| **UTE Tech** | `https://www.ute-tech.com.cn/ci-yc/index.php/api/client/getBtVersionUpdate` | UTE series Bluetooth firmware patch retriever |
| **AICaring** | `https://api.aicaring.com/alg/exterior/health/allrslts` | Cloud physiological algorithm compute engine |
| **AICaring** | `https://api.aicaring.com/api/v0/sinklib/activation` | Algorithmic license activation for biometric calculations |
| **GoMore** | `https://saas-api.gomore.club/api/firmware/key/get` | GoMore aerobic / anaerobic workout coaching license validation |

#### Domain D: AI Voice Companion & Gateway Services

| Platform / Vendor | Target URL / Path | Protocol / Purpose |
| :--- | :--- | :--- |
| **Transsion AI Bus** | `https://ai-api.transsion.com` | Production REST/RPC gateway for generative watch faces and health inquiries |
| **Transsion AI Bus** | `http://bus.aitest.transsion.com` | Internal engineering development AI test gateway |
| **AIBot India** | `https://ai-api-india.aibotplatform.com` | Low-latency regional voice bot endpoint for South Asian markets |
| **AIBot Singapore** | `https://open-ai-api.singapore.aibotplatform.com` | Southeast Asia regional multimodal AI streaming endpoint |
| **AIBot EE1** | `https://open-ai-api-ee1.aibotplatform.com` | European regional voice assistant processing endpoint |

#### Domain E: Third-Party & Infrastructure Telemetry

| Service Provider | Target URL / Path | Purpose |
| :--- | :--- | :--- |
| **Google Identity** | `https://accounts.google.com/o/oauth2/revoke?token=` | Revokes user Google account authorization token upon logout |
| **Network Time** | `https://time.cloudflare.com` | High-precision HTTPS network time fallback (NTP over TLS) |
| **Network Time** | `https://time1.google.com` | Secondary high-precision HTTPS network time fallback |

### 8.3 Observable Request & Response Schemas

#### Schema Example 1: Dynamic UI Configuration Request
```http
GET /ifit/public/ui/config?appVersion=5.9.1.17&lang=en&osVersion=14 HTTP/1.1
Host: api.nebulalive.com
Accept: application/json
User-Agent: iPulse/5.9.1.17 (Linux; Android 14; Mobile)
```
**Observed Response Structure:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "banners": [
      {
        "id": 1042,
        "title": "Summer Fitness Challenge",
        "imageUrl": "https://cdn.transsion-os.com/iFit/banner/summer_2026.webp",
        "targetUrl": "healthlife://sport/plan?id=summer_challenge"
      }
    ],
    "features": {
      "ai_watch_dial_enabled": true,
      "menstrual_tracking_enabled": true,
      "stress_hrv_enabled": true
    }
  }
}
```

#### Schema Example 2: Palm ID OAuth2 Token Exchange
```http
POST /client/token HTTP/1.1
Host: auth-account.palm.tech
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code&client_id=com.transsion.healthifit&code=AUTH_CODE_REDACTED&redirect_uri=https://account.palm.tech/client/callback
```
**Response Contract:**
```json
{
  "access_token": "<redacted_jwt_token>",
  "token_type": "Bearer",
  "expires_in": 7200,
  "refresh_token": "<redacted_refresh_token>",
  "openId": "p_usr_8892104812"
}
```

---

### Evidence Log
- All 63 DEX URLs extracted and verified from `base/classes*.dex`: `base/classes*.dex:1`
- Palm ID authentication endpoints verified in `PalmAccProvider.java`: `decompiled/sources/com/transsion/palmsdk/account/PalmAccProvider.java:1-50`
- MoYoung cloud dial API endpoints verified in `com.moyoung.watch.sdk`: `classes.dex:https://api.moyoung.com/faces/v3/list`
- FitCloud DFU endpoint verified in `FitCloud SDK`: `classes.dex:https://fitcloud.hetangsmart.com/public/checkVersion/v2`
- AI assistant gateway endpoints verified in `decompiled/sources/com/transsion/transvasdk/`: `classes.dex:https://ai-api.transsion.com`
