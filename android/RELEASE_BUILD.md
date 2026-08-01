# DYLANDOS IPTV ULTIMATE — Signed Release Build & Install Guide

This is the **step-by-step, foolproof** guide to producing a **signed release APK**
for the **FireStick 4K / FireStick 4K Max** and getting it onto your device. It is
written for **Windows 11 Pro** (the shell scripts also work on macOS/Linux).

---

## TL;DR (Windows)

```powershell
cd android
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
powershell -ExecutionPolicy Bypass -File scripts\build-signed-apk.ps1
```

When it finishes, your signed APK is at:

```
android\dist\DylandosIPTV-Firestick-v1.0.0-YYYYMMDD.apk
```

That's it. The script generates the signing keystore, signs the app, builds the
FireStick flavor, copies the APK out, and verifies the signature for you.

---

## What "signed release" means here

- **Release** build → optimized (`isMinifyEnabled`, R8, resource shrinking) and
  not debuggable.
- **Signed** → the APK is signed with a keystore. The script auto-generates
  `app\dylandos-release.jks` on first run (RSA 4096, alias `dylandos`) and writes
  `keystore.properties` so Gradle finds it.
- If you already supply your own `keystore.properties` / keystore, the script and
  Gradle will **respect yours** and never overwrite it.
- **Foolproof fallback:** even if you skip signing setup entirely, the release
  task falls back to the Android **debug keystore** so the build can never fail to
  produce an APK during development. You'll still get a playable APK (just not a
  production-signed one).

---

## Prerequisites (Windows 11)

| Tool | Why | Where |
|------|-----|-------|
| **JDK 17+** (Adoptium recommended) | Compiling | `https://adoptium.net` — use the exact path in the command above |
| **Android Studio** (or Android SDK cmdline-tools) | SDK + build-tools | `https://developer.android.com/studio` |
| **Android SDK** installed | `platforms;android-35`, `build-tools` | Auto-installed by Studio or `sdkmanager` |

The project's **Gradle wrapper** (`gradlew.bat`) is pinned to Gradle 8.9 and
downloads Gradle automatically — you do **not** install Gradle yourself.

---

## Option A — One-command script (recommended)

```powershell
cd android
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
powershell -ExecutionPolicy Bypass -File scripts\build-signed-apk.ps1
```

What it does:
1. Validates `JAVA_HOME`.
2. Creates `app\dylandos-release.jks` if missing.
3. Writes `keystore.properties` (only if absent).
4. Runs `.\gradlew assembleFirestickRelease`.
5. Copies the signed APK to `android\dist\` with a dated name.
6. Verifies the signature with `apksigner`.

macOS / Linux:

```bash
cd android
./scripts/build-signed-apk.sh
```

---

## Option B — Manual (if you prefer full control)

### 1. Generate a keystore (once)
```powershell
powershell -ExecutionPolicy Bypass -File scripts\generate-keystore.ps1
```

### 2. Configure signing
Copy `keystore.properties.example` → `keystore.properties` and edit, or rely on
the script. The Gradle signing block reads it automatically.

### 3. Build
```powershell
.\gradlew assembleFirestickRelease
```
Output (signed if a keystore exists):
```
app\build\outputs\apk\firestick\release\app-firestick-release.apk
```

### 4. Verify the signature
```powershell
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\<ver>\apksigner.bat" verify --verbose app\build\outputs\apk\firestick\release\app-firestick-release.apk
```

---

## Installing on the FireStick

### Fastest — ADB over network
1. On the FireStick: **Settings → My Fire TV → About →** tap the network/name
   field 7× to enable **Developer Options**.
2. **Settings → My Fire TV → Developer Options →** enable **ADB debugging** and
   **Install unknown apps**.
3. Note the FireStick's IP address (**Settings → My Fire TV → About → Network**).
4. On your PC:
   ```powershell
   adb connect <FIRE_STICK_IP>:5555
   adb install android\dist\DylandosIPTV-Firestick-v1.0.0-YYYYMMDD.apk
   ```

### Sideload via Downloader app
Put the APK in your Dropbox/Google Drive or a direct URL, open the **Downloader**
app on the FireStick, enter the URL, and install. Enable **Install unknown apps**
for Downloader first.

---

## Testing on your Windows 11 emulator

The **Generic** flavor includes `x86_64`, so it runs on the Android Studio emulator
(a real FireStick uses `arm64-v8a` from the Firestick flavor).

```powershell
.\gradlew assembleGenericDebug
adb install -r app\build\outputs\apk\generic\debug\app-generic-debug.apk
```

> On the emulator, simulate the remote with the on-screen D-pad or keyboard arrows,
> Enter, Back, and Media keys. Our `FireStickKeys` mapping treats
> DPAD_CENTER / ENTER / NUMPAD_ENTER identically, so all of them "click".

---

## Flavors & build variants

| Flavor      | Target                              | ABIs packaged          | Memory flag           |
|-------------|-------------------------------------|------------------------|-----------------------|
| **Firestick** | FireStick 4K / 4K Max (flagship)  | `arm64-v8a`, `armeabi-v7a` | `2048` MB            |
| **Generic**   | Emulator / other Android TV boxes | `arm64-v8a`, `armeabi-v7a`, `x86_64` | `4096` MB |

Common Gradle tasks:
- `assembleFirestickRelease` — signed FireStick APK (main deliverable).
- `assembleFirestickDebug` — debuggable FireStick APK.
- `assembleGenericDebug` — emulator-friendly debug APK.
- `assembleGenericRelease` — signed APK for other devices.

---

## Safety notes on the keystore

- **Back it up.** `app\dylandos-release.jks` is the key that proves the app is
  yours. If you lose it, you cannot push updates to the same app identity.
- The default passwords (`dylandos2026`) are for **local development only**. For a
  production release, set your own in `keystore.properties` or the `KS_*` env vars,
  and keep them out of version control (already git-ignored).
- `keystore.properties` and `*.jks` are **never committed** (see `.gitignore`).
