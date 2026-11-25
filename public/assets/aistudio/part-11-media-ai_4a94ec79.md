# Media Processing, Audio Pipelines & Multimodal AI Integration

### 11.1 Audio & Voice Pipeline (Opus & PCM Streaming)

iPulse implements a high-throughput, low-latency audio capture and compression pipeline engineered for smartwatch voice notes and AI assistant voice interaction. The audio subsystem integrates 3 native shared libraries:

```
+==================================================================================================+
|                                    AUDIO PROCESSING PIPELINE                                     |
+==================================================================================================+
|  [Microphone Audio Source] (Android AudioRecord 16kHz 16-bit Mono PCM)                          |
|             |                                                                                    |
|             v                                                                                    |
|  [JNI Buffer Bridge] -> libpcm2opus.so (Transforms raw PCM buffers to framed Opus packets)       |
|             |                                                                                    |
|             v                                                                                    |
|  [Encoder Optimization] -> libopus.so / libeasyopus.so (VBR, 16–24 kbps compression ratio)       |
|             |                                                                                    |
|             +----------------------------------+----------------------------------+              |
|             |                                                                     |              |
|             v                                                                     v              |
|  [Smartwatch Outbound Stream]                             [AI Voice Assistant Stream]            |
|  (Dispatches voice message via BLE GATT                   (Streams chunks to Transsion AI        |
|   chunks to peripheral watch speaker)                      WebSocket gateway in real time)       |
+==================================================================================================+
```

1. **Native Encoder Stack:** `libpcm2opus.so` (258 KB) consumes direct Android `ByteBuffer` references, avoiding JNI array copy overhead. Framed packets pass into `libopus.so` (431 KB) configured at a 16 kHz sampling rate with a 20 ms frame duration.
2. **Audio Decoding:** For incoming smartwatch voice memos, `libeasyopus.so` (307 KB) unpacks incoming BLE chunks and feeds Android `AudioTrack` for immediate playback.

### 11.2 Video, GIF Transcoding & Dial Compression Engine

Smartwatch display controllers cannot process standard MP4/H.264 video streams directly due to severe RAM and CPU constraints. iPulse embeds the complete FFmpeg native suite (`v6.0`) to transcode custom video clips into proprietary smartwatch animation sequences:

| Native Module | Size (Bytes) | Role in Video-to-Dial Transcoding |
| :--- | :--- | :--- |
| `libavcodec.so` | 11,152,584 | Decodes input video files (MP4, WebM, MOV) selected by user |
| `libavformat.so` | 5,857,648 | Demuxes multimedia containers and parses container metadata |
| `libavfilter.so` | 3,026,696 | Crops video to exact watch aspect ratios (1:1 circular, 4:5 rectangular) |
| `libswscale.so` | 313,936 | Converts YUV420p color buffers into 16-bit RGB565 / RGB888 bitmaps |
| `libpl_droidsonroids_gif.so` | 42,464 | Renders live preview loops inside the watch dial studio |
| `libminilzo.so` | 408,328 | Compresses frame payloads using LZO algorithm for high-speed BLE transfer |
| `libezipmy.so` | 393,976 | Compresses MoYoung proprietary dial sprite animation bundles |
| `libezipqw.so` | 516,648 | Compresses Qiwo/HuaWo proprietary dial sprite animation bundles |
| `libjl_bmp_convert.so` | 379,360 | Converts frame sequences for JieLi (JL) dual-mode Bluetooth chipsets |

**Processing Workflow:**
1. User selects a 5-second video in `VideoTrimmerActivity`.
2. `VideoToGifActivity` invokes `libffmpegkit.so` to extract frames at 15 FPS capped at 240x280 or 466x466 resolution.
3. `libswscale.so` reduces color depth to RGB565.
4. Frames are compressed via `libminilzo.so` or vendor-specific codecs (`libezipmy.so` / `libezipqw.so`) into an installable `.bin` dial package.

### 11.3 AI Voice Assistant & Multimodal Bot Gateway

The application incorporates Transsion's Ella AI voice ecosystem implemented in `com.transsion.transvasdk`. The service connects to regional AI streaming endpoints:

```
+==================================================================================================+
|                                  TRANSSION AI VOICE ARCHITECTURE                                 |
+==================================================================================================+
|  [Voice Capture / Speech Input]                                                                  |
|             |                                                                                    |
|             v                                                                                    |
|  [WebSocket Transport Engine] (TTSWebSocketInterface)                                            |
|             |                                                                                    |
|             | === 1. Secure WSS Connection ===> https://ai-api.transsion.com                     |
|             |                                  (or regional endpoint in Singapore / India)       |
|             |                                                                                    |
|             | === 2. Stream Audio Chunks =====> Automated Speech Recognition (ASR)               |
|             |                                                                                    |
|             | <== 3. Incremental Text Tokens == Natural Language Understanding (NLU)             |
|             |                                                                                    |
|             | <== 4. Stream Synthesized Audio = Text-to-Speech (TTS Engine)                      |
|             |                                                                                    |
|             v                                                                                    |
|  [Audio Playback & UI Update] (Renders textual response & plays TTS voice reply via speaker)     |
+==================================================================================================+
```

### 11.4 Generative AI Watch-Face Creation Engine

Integrated within `packages/trans_flutter_module_ai_watch_dial`, users generate custom watch faces by providing natural language prompts (e.g. "Cyberpunk neon cat running through rain"):
1. **Prompt Submission:** Prompt dispatched via HTTPS POST to `https://ai-api.transsion.com/v1/dial/generate`.
2. **Task Queue Polling:** Client monitors generation task status using task ID stored in `AiWatchHistoryEntity`.
3. **Asset Ingest:** Cloud returns generated high-resolution WebP image (`imgUrl`).
4. **Local Dial Synthesis:** Client crops image to target watch screen resolution, packages time hands and complication coordinates via `libsfwatchface.so`, and streams finished `.bin` file to the connected watch via BLE.

---

### Evidence Log
- Native audio/video processing libraries verified via `arm/lib/arm64-v8a/`: `libavcodec.so`, `libopus.so`, `libpcm2opus.so`
- Video processing activities verified in `manifest.xml:541, 909, 911`: `VideoTrimmerActivity`, `VideoToGifActivity`, `VideoToGif2Activity`
- AI SDK classes and WebSocket interfaces verified in `decompiled/sources/com/transsion/transvasdk/`: `TTSWebSocketInterface`
- AI dial history table verified via `HealthDataBase_Impl.java:817`: `AiWatchHistoryEntity`
- AI backend URLs verified in `classes.dex`: `https://ai-api.transsion.com`, `https://open-ai-api.singapore.aibotplatform.com`
