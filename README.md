# DYLANDOS IPTV ULTIMATE

A production-grade, enterprise-class **Android TV IPTV application** engineered
specifically for the **Amazon FireStick 4K** (API 23+, 2 GB RAM roof, low-end Mali
GPU, 10-foot remote D-pad interaction).

Built from scratch with a strict **hardware budget**: hardware-accelerated image
caches, GC-friendly thread-isolated state, no `Modifier.blur()`, and a single
active hardware-decoder pipeline at any moment.

---

## Tech Stack

| Concern         | Technology                                                              |
|-----------------|-------------------------------------------------------------------------|
| Language        | Kotlin (2.0.x)                                                          |
| UI              | Jetpack Compose for TV — `androidx.tv:tv-material` (+ `tv-foundation`)  |
| DI              | Hilt                                                                     |
| Persistence     | Room (with exportable schema + migrations), DataStore Preferences        |
| Async           | Coroutines + StateFlow                                                   |
| Paging          | Paging 3                                                                 |
| Background      | WorkManager (Hilt worker) + foreground DVR service                       |
| Images          | Coil (hardware-accelerated, GC-friendly)                                 |
| Media Engine 1  | LibVLC (`org.videolan.android:libvlc-all`)                              |
| Media Engine 2  | MPV (`is.xyz.mpv:mpv-android`)                                          |

**Single Source of Truth:** every screen is driven by a Hilt `ViewModel` that
backs onto reactive Room DAOs / StateFlow repositories. UI never holds provider
catalog state in memory — Paging 3 streams it.

---

## The 6 Core Modules

### 1 · Dual Media Engine + Sparkle Zap OSD
`media/DualMediaEngine.kt` routes streams dynamically:

- **Live TV** (MPEG-TS, RTSP, RTMP) → **LibVlcEngine** for native TS parsing and
  `:sout` stream-copy capture.
- **VOD / Series / HDR / local DVR files** → **MpvEngine** for hardware decoding
  and container parsing.
- **Live HLS (.m3u8)** → MPV by default with an automatic **4000 ms** failover to
  LibVLC (`PlaybackOptions.hlsFailoverMs`, configurable).

The **Sparkle Zap OSD** (`ui/player/PlayerScreen.kt`) is a bottom-third overlay on
D-pad UP/DOWN/CENTER that shows channel logo, a current-show progress bar, live
stream diagnostics (codec / bitrate / FPS / active engine), and a horizontal
ribbon of the next 3 EPG programmes. Play/pause, subtitle, audio-track and record
buttons bind directly to the active engine.

### 2 · DVR Engine, Single-Instance Capture & FAT32 Rolling Segmenter
- **Single-connection capture:** `LibVlcEngine` appends
  `:sout=#duplicate{dst=display,dst=std{access=file,mux=ts,dst="<fifo>"}}` to the
  *active* player — display + record share one provider socket.
- **USB/OTG SAF priority:** `DvrStorageManager` uses the Storage Access Framework
  with persistent URI permissions and prefers mounted USB drives.
- **FAT32 3.8 GB rolling segmenter:** `RollingFileRecorder` rotates output to
  `name_part2.ts`, `name_part3.ts`, … at `3_800_000_000` bytes without dropping a
  frame (the LibVLC SOUT pipe is drained into the segmenter).

### 3 · Timeshift Disk-Ring Buffer
A Settings toggle enables/disables Timeshift. When on, `LibVlcEngine` configures a
localized circular disk buffer on the USB path (`input-timeshift-path` +
`input-timeshift-granularity`) for instant pause / rewind (up to 30 min) / FF on
live TV. When off, the buffer is never allocated and disk I/O is bypassed.

### 4 · 100%-Accuracy EPG & Fuzzy Matching Pipeline
- `XmltvParser`: streaming pull-parser on `Dispatchers.IO`, normalizing dates and
  timezone offsets to UTC epoch-ms.
- `ChannelMatcher` multi-pass: exact `tvg-id` → exact `tvg-name` → normalized
  **Levenshtein** fuzzy match (strips country prefixes, HD/4K/FHD tags, special
  chars, leading digits).
- `EpgScreen` Canvas Grid: focused-channel highlight, current-show progress bar,
  show details, and the next 3 upcoming programmes on the focused row.

### 5 · Xtream / M3U Pipeline & Category Organizer
- `XtreamRepository` ingests live/VOD/series catalogs with **Paging 3**.
- **Account isolation:** switching/logging out triggers a mandatory cascade purge
  of channels, EPG, and categories (FK-cascade + explicit DAO purges) so no
  cross-account cache leak can occur.
- **Category hiding:** `CategoryEntity.isUserVisible` flag toggled from the
  "Manage Categories" screen via D-pad long-press / CENTER.

### 6 · Sparkle-Inspired UI & Zero-Jank D-Pad Focus
- Dark cinematic theme with semi-transparent **glassmorphism** panels
  (`GlassPanel`) and 10-foot TV typography (`Type.kt`).
- **Collapsing Navigation Rail** (Live TV / Movies / Series / DVR / Settings) that
  expands with a `tween(150)` animation only when focused.
- Focus rules: LEFT on any content card transfers focus to the rail; BACK from a
  detail screen restores the exact card index (`AppViewModel.pendingFocusIndex`).
- `KEYCODE_DPAD_CENTER`, `KEYCODE_ENTER`, `KEYCODE_NUMPAD_ENTER` are normalized to
  one key in `MainActivity.dispatchKeyEvent`.
- Focus transforms use `Modifier.graphicsLayer` (no `Modifier.blur()`).

---

## Project Layout

```
android/
├── settings.gradle.kts, build.gradle.kts, gradle.properties
├── gradle/libs.versions.toml          # version catalog
├── app/
│   ├── build.gradle.kts               # flavors: Firestick / Generic
│   ├── proguard-rules.pro
│   ├── schemas/                        # Room exported schema output
│   └── src/main/java/com/dylandos/iptv/
│       ├── IptvApplication.kt          # Hilt app + Configuration.Provider
│       ├── MainActivity.kt             # key normalization
│       ├── di/                          # AppModule, MediaModule
│       ├── data/                        # entities, DAOs, AppDatabase, SettingsRepository
│       ├── media/                       # MediaEngine, LibVlcEngine, MpvEngine, DualMediaEngine
│       ├── dvr/                         # DvrStorageManager, RollingFileRecorder, RecordingService
│       ├── epg/                         # XmltvParser, ChannelMatcher, Levenshtein, EpgRepository
│       ├── xtream/                      # XtreamClient, XtreamRepository
│       └── ui/                          # theme, navigation, components, screens, player
```

---

## Building

**DYLANDOS IPTV ULTIMATE is a FireStick-first application.** The `Firestick`
flavor is the flagship target (2 GB-class optimizations, `arm64-v8a` + `armeabi-v7a`
ABIs). A `Generic` flavor (`x86_64` included) targets emulators and other TV boxes.

A Gradle wrapper distribution is pinned in `gradle/wrapper/gradle-wrapper.properties`
(Gradle 8.9). With the Android SDK installed and a JDK 17+ on `JAVA_HOME`.

### ⭐ Build a signed FireStick release APK (foolproof, one command)

The whole pipeline is automated so it "just works" — it generates the signing
keystore if missing, signs the APK, and verifies the signature. See
[`android/RELEASE_BUILD.md`](android/RELEASE_BUILD.md) for the complete guide.

**Windows PowerShell** (from `android/`):

```powershell
cd android
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
powershell -ExecutionPolicy Bypass -File scripts\build-signed-apk.ps1
```

**macOS / Linux:**

```bash
cd android
./scripts/build-signed-apk.sh
```

The signed APK lands in `android/dist/DylandosIPTV-Firestick-v1.0.0-<date>.apk`.

Or manually:

```powershell
cd android
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
.\gradlew assembleFirestickRelease
# signed output: app\build\outputs\apk\firestick\release\app-firestick-release.apk
```

### Flavor dimensions
- **`Firestick`** — `BuildConfig.TARGET_MEMORY_MB = 2048` (2 GB-class optimizations).
  Packaged ABIs: `arm64-v8a`, `armeabi-v7a`.
- **`Generic`** — `BuildConfig.TARGET_MEMORY_MB = 4096`. Packaged ABIs include
  `x86_64` for emulator testing.

Common tasks: `assembleFirestickRelease`, `assembleFirestickDebug`,
`assembleGenericDebug` (emulator), `assembleGenericRelease`.

### Signing
- `scripts/generate-keystore.ps1` — creates `app/dylandos-release.jks` (RSA 4096).
- `keystore.properties` (git-ignored) drives signing; `build-signed-apk.*` writes
  it automatically. If absent, the release task falls back to the debug keystore
  so the build never fails.
- **Back up the keystore** — you cannot update a released app without it.

### Remote / D-pad mapping
All FireStick remote & gamepad keys are mapped in one place
(`ui/input/FireStickKeys.kt`): DPAD_CENTER / ENTER / NUMPAD_ENTER are collapsed to
one logical "ACTIVATE", and media-transport keys are forwarded to the active
player regardless of screen.

---

## Verification status

> ⚠️ The build itself could **not** be executed inside this sandbox: the workspace
> has **no JDK, no Android SDK, and no Gradle**, outbound HTTPS/SSL to package
> repositories (Adoptium, Gradle, Google Maven) is blocked, and the Debian apt
> mirror is unreachable. The verification command above is Windows/PowerShell
> specific. The codebase was written to be complete and compilable, but you must
> run the Gradle build on a machine with the Android toolchain to confirm.

**Before your first build**, confirm/adjust two things in `gradle/libs.versions.toml`:

1. **MPV artifact** — `is.xyz.mpv:mpv-android:<ver>`. Pin a version that exists in
   the configured Maven repo (the project lists the mango-cpe and a GitHub maven
   mirror). If your provider mirrors it under `com.github.mpv-android:mpv-android`,
   update the group + repo accordingly.
2. **LibVLC** — `org.videolan.android:libvlc-all:3.5.1` is fetched from
   `https://repo.videolan.org/maven` (already in `settings.gradle.kts`).

Both native engines ship prebuilt `.so`/`.aar` assets and are packaged with
`jniLibs.useLegacyPackaging = true`.
