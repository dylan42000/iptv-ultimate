# DYLANDOS IPTV ULTIMATE — GitHub Signed Release APK — Windows 11 Pro Complete Guide

This guide is tailored **specifically for Windows 11 Pro** and shows you how to build a **production-signed release APK** for your FireStick 4K / 4K Max and publish it as a GitHub release.

> Source repo: `dylan42000/iptv-ultimate`
> Current branch: `arena/019fbb20-iptv-ultimate`
> App ID: `com.dylandos.iptv.ultimate`
> Version: `1.0.0 (code 1)` — defined in `android/app/build.gradle.kts`

---

## 1. What you get

After the build you will have **2 copies of the same signed APK**:

1. **Gradle output (raw):**
   ```
   android\app\build\outputs\apk\firestick\release\app-firestick-release.apk
   ```
2. **Dated distributable (recommended for GitHub Releases):**
   ```
   android\dist\DylandosIPTV-Firestick-v1.0.0-YYYYMMDD.apk
   ```
   Example: `DylandosIPTV-Firestick-v1.0.0-20250801.apk`

### Flavor specs

| Flavor | What | ABIs | Memory Flag | Use |
|--------|------|------|-------------|-----|
| **Firestick** (flagship) | FireStick 4K / 4K Max | `arm64-v8a`, `armeabi-v7a` | `2048 MB` | Real device installs — **use this for GitHub Release** |
| Generic | Emulator / other ATV boxes | `arm64-v8a`, `armeabi-v7a`, `x86_64` | `4096 MB` | Windows 11 Android Studio emulator |

### Signing details

- Keystore auto-generated: `android\app\dylandos-release.jks`
- Type: `RSA 4096`, validity 10000 days, alias `dylandos`
- Default dev passwords (in `keystore.properties`): `dylandos2026` — Change for production
- `keystore.properties` and `*.jks` are **git-ignored** — never committed
- If no keystore exists, Gradle **falls back to debug keystore** so build never fails (but then it's NOT production-signed)

> **CRITICAL:** Back up `dylandos-release.jks` to USB + cloud. Lose it = cannot update same Play/App identity. Google/Amazon requires same key for updates.

---

## 2. Prerequisites — Windows 11 Pro Checklist

Install these **once** on Windows 11 Pro:

1. **JDK 21 — Eclipse Adoptium (recommended)**
   Download: https://adoptium.net/temurin/releases/
   Install MSI to: `C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot`
   Verify:
   ```powershell
   java -version
   keytool -help | Select-Object -First 5
   ```

2. **Android Studio — Ladybug or newer**
   Download: https://developer.android.com/studio
   During setup ensure you install:
   - SDK Platform `android-35`
   - Build-Tools `35.x`
   - Platform-Tools (for `adb`)
   Default SDK path: `%LOCALAPPDATA%\Android\Sdk`

3. **Git + GitHub CLI (optional but for pushing release)**
   ```powershell
   winget install Git.Git
   winget install GitHub.cli
   gh auth login
   ```

Set `JAVA_HOME` **permanently** on Windows 11:

```powershell
# Run PowerShell as Administrator
[System.Environment]::SetEnvironmentVariable("JAVA_HOME","C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot","Machine")
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
```

Or for current session only:
```powershell
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
```

---

## 3. EASIEST BUILD — 1 Command (Windows 11 Pro)

### Option A: Command Prompt — `build.bat` (zero PowerShell needed)

```bat
cd /d D:\path\to\iptv-ultimate\android
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot
build
```

What it does (fully automated):
1. Finds JDK via JAVA_HOME → common Adoptium/Studio paths
2. Generates `app\dylandos-release.jks` if missing (via `keytool`)
3. Writes `keystore.properties` if missing
4. Runs `gradlew.bat assembleFirestickRelease` (R8 + minify)
5. Copies APK to `dist\` with dated name
6. Verifies signature with `apksigner` if found

**Done.** APK is at `android\dist\DylandosIPTV-Firestick-v1.0.0-20250801.apk`

### Option B: PowerShell — `build-signed-apk.ps1` (same result)

Open **Windows Terminal (PowerShell 7)** or **PowerShell 5.1** as **normal user** (not admin needed):

```powershell
cd D:\path\to\iptv-ultimate\android
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
powershell -ExecutionPolicy Bypass -File scripts\build-signed-apk.ps1
```

### Option C: Manual control (if you want custom passwords)

```powershell
cd D:\path\to\iptv-ultimate\android
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"

# 1. Generate keystore once
powershell -ExecutionPolicy Bypass -File scripts\generate-keystore.ps1
# Or custom:
$env:KS_ALIAS="dylandos"
$env:KS_PASSWORD="YOUR_STRONG_PASSWORD_123!"
powershell -ExecutionPolicy Bypass -File scripts\generate-keystore.ps1

# 2. Edit keystore.properties if you used custom password
notepad keystore.properties.example
copy keystore.properties.example keystore.properties
notepad keystore.properties

# 3. Build
.\gradlew.bat assembleFirestickRelease

# 4. Verify
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\35.0.0\apksigner.bat" verify --verbose app\build\outputs\apk\firestick\release\app-firestick-release.apk
```

Gradle tasks you can use:
- `assembleFirestickRelease` — production signed FireStick (MAIN)
- `assembleFirestickDebug` — debuggable FireStick
- `assembleGenericDebug` — x86_64 emulator build (test on Windows 11 emulator)
- `assembleGenericRelease` — signed Generic

---

## 4. Verify APK Signature — Windows 11

```powershell
$APK="D:\path\to\iptv-ultimate\android\dist\DylandosIPTV-Firestick-v1.0.0-20250801.apk"
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\35.0.0\apksigner.bat" verify --verbose $APK

# Also check cert details:
& "$env:JAVA_HOME\bin\keytool.exe" -printcert -jarfile $APK

# Quick info
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\35.0.0\aapt2.bat" dump badging $APK | findstr "package versionName"
```

Expected `apksigner` output:
```
Verifies
Verified using v1 scheme (JAR signing): true
Verified using v2 scheme (APK Signature Scheme v2): true
Verified using v3 scheme (APK Signature Scheme v3): true
```

If you see `Verified using v2/v3 = true` you're good to publish.

---

## 5. Install on FireStick from Windows 11 Pro

### ADB over WiFi (fastest)

On FireStick:
Settings → My Fire TV → About → Click 7x on your Fire TV Stick name to unlock Developer Options
Settings → My Fire TV → Developer Options → Enable ADB Debugging + Install unknown apps → Allow Downloader / Allow if prompted

On Windows 11:
```powershell
cd D:\path\to\iptv-ultimate\android

# Find FireStick IP: Settings → My Fire TV → About → Network
$FIRE_IP="192.168.1.123"
adb connect ${FIRE_IP}:5555
adb devices
adb install -r dist\DylandosIPTV-Firestick-v1.0.0-20250801.apk
# -r = allow reinstall / update

# Logs if needed
adb logcat | Select-String "Dylandos"
```

### Via Downloader app
1. Upload APK to Dropbox / Google Drive direct link, or to your GitHub Release
2. On FireStick open Downloader app, enter URL, Download → Install
3. Enable Install unknown apps for Downloader when prompted

### Test on Windows 11 Emulator first (optional)
```powershell
.\gradlew.bat assembleGenericDebug
adb install -r app\build\outputs\apk\generic\debug\app-generic-debug.apk
```
Use keyboard arrows = D-pad, Enter = CENTER, Backspace = BACK

---

## 6. Push to GitHub Releases — Windows 11 Pro

### Via GitHub Web (simplest)
1. Go to https://github.com/dylan42000/iptv-ultimate/releases → Draft new release
2. Tag: `v1.0.0-firestick-20250801` — Title: `Dylandos IPTV Ultimate v1.0.0 FireStick Signed`
3. Upload `DylandosIPTV-Firestick-v1.0.0-20250801.apk` from `dist\`
4. Paste release notes below → Publish

### Via GitHub CLI (from Windows Terminal)
```powershell
cd D:\path\to\iptv-ultimate
gh release create v1.0.0-firestick-$(Get-Date -Format yyyyMMdd) `
  android\dist\DylandosIPTV-Firestick-v1.0.0-*.apk `
  --title "Dylandos IPTV Ultimate v1.0.0 FireStick Signed" `
  --notes-file RELEASE_NOTES.md `
  --latest

# Or specific file
gh release create v1.0.0 --title "v1.0.0" android\dist\DylandosIPTV-Firestick-v1.0.0-20250801.apk --generate-notes
```

**Release notes template to paste:**
```
## DYLANDOS IPTV ULTIMATE v1.0.0 — Firestick Signed Release

Production-signed APK for FireStick 4K / 4K Max (arm64-v8a + armeabi-v7a)

**APK:** DylandosIPTV-Firestick-v1.0.0-YYYYMMDD.apk
**Package:** com.dylandos.iptv.ultimate
**Target:** API 35 (min 23), 2GB RAM optimized, hardware-accelerated
**Engines:** LibVLC (Live TS/RTSP/RTMP) + MPV (VOD/Series/HLS) with 4000ms HLS failover
**Features:** Sparkle Zap OSD, FAT32 3.8GB rolling DVR, Timeshift ring buffer, EPG fuzzy matching, Xtream/M3U Paging3

**Install:**
adb connect <FIRESTICK_IP>:5555
adb install DylandosIPTV-Firestick-v1.0.0-YYYYMMDD.apk

**Build info:**
Gradle 8.9, AGP 8.5.2, Kotlin 2.0.21, JDK 21
Signed with dylandos-release.jks (RSA 4096) + apksigner v2/v3 verified

**SHA256:** (run: certutil -hashfile apk SHA256)
```

### Get SHA256 on Windows 11 for release page
```powershell
certutil -hashfile android\dist\DylandosIPTV-Firestick-v1.0.0-20250801.apk SHA256
Get-FileHash android\dist\DylandosIPTV-Firestick-v1.0.0-20250801.apk -Algorithm SHA256
```

---

## 7. Troubleshooting Windows 11 Pro

| Error | Fix |
|-------|-----|
| `JAVA_HOME not set / JDK not found` | `set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot` and ensure `java.exe` exists there |
| `keytool not found` | Add `%JAVA_HOME%\bin` to PATH, restart terminal |
| `SDK location not found` | Create `android\local.properties` with `sdk.dir=C\:\\Users\\YOURNAME\\AppData\\Local\\Android\\Sdk` (escaped) — Android Studio creates this automatically |
| `apksigner not found` | Install build-tools 35 via SDK Manager, path is `%LOCALAPPDATA%\Android\Sdk\build-tools\<ver>\apksigner.bat` |
| `> Task :app:compileDebugKotlin FAILED` | Update Kotlin in `libs.versions.toml`, or run `.\gradlew clean` |
| `mpv artifact not found` | Edit `gradle/libs.versions.toml` → `mpv = "0.38.0"` and repo URLs in `settings.gradle.kts` — try `com.github.mpv-android:mpv-android` mirror if `is.xyz.mpv` fails |
| `libvlc not found` | Check `https://repo.videolan.org/maven` is in `settings.gradle.kts` (it is) and internet not blocked |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` on FireStick | Uninstall old version first: `adb uninstall com.dylandos.iptv.ultimate` or `adb uninstall com.dylandos.iptv.ultimate.debug` — different keystores can't update each other |
| `Timeout gradle wrapper download` | Gradle 8.9 wrapper needs internet; on first build allow firewall. Wrapper jar already in repo. |

---

## 8. Security Checklist Before GitHub Release

- [ ] `app\dylandos-release.jks` **backed up** to 2 locations (encrypted USB + cloud drive)
- [ ] `keystore.properties` **NOT committed** (already in `.gitignore`)
- [ ] For real production, change default password `dylandos2026` to strong password via `$env:KS_PASSWORD`
- [ ] APK verified with `apksigner verify --verbose` → v2/v3 true
- [ ] Tested `adb install` on one FireStick before publishing release
- [ ] Added SHA256 hash to release notes
- [ ] Release tag uses semver: `v1.0.0`

---

## 9. One-liner you can copy-paste on fresh Windows 11 Pro

Open **Command Prompt (not PowerShell)** as normal user:

```bat
cd /d %USERPROFILE%\Desktop\iptv-ultimate\android
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot
build
```

Wait ~2-5 min first time (Gradle download + build). Output:

```
[OK] Signed APK created:
     D:\...\android\dist\DylandosIPTV-Firestick-v1.0.0-20250801.apk

[VERIFY] Checking signature...
Verifies
...
DONE - transfer D:\...\dist\DylandosIPTV-Firestick-v1.0.0-20250801.apk to your FireStick.
```

Now publish that file to GitHub Releases.

---

**Next:** `gh release create` or drag-drop the APK to https://github.com/dylan42000/iptv-ultimate/releases/new
