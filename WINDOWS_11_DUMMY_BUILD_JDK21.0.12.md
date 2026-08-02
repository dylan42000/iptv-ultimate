# DUMMY PROOF BUILD — Firestick Signed APK — Windows 11 Pro + JDK 21.0.12+8

You have Android Studio + Eclipse Temurin JDK 21.0.12+8 Hotspot installed.
You only want the signed Firestick APK. This is the 5-minute dummy version.

### What you will type is 3 lines total.

---

## Step 0 — Find your JDK folder name (30 sec)

You just installed `Temurin 21.0.12+8 x64`. The installer puts it here:

1. Open File Explorer
2. Go to: `C:\Program Files\Eclipse Adoptium\`
3. You will see a folder like:
   `jdk-21.0.12+8`  OR  `jdk-21.0.12.8-hotspot`

**Right-click that folder -> Copy as path** or note the full path.
For this guide we will use:
```
C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot
```
If yours is slightly different, use YOUR name instead.

Quick check in Command Prompt:
```
dir "C:\Program Files\Eclipse Adoptium\"
```
It will list the exact folder name.

---

## Step 1 — Open Command Prompt INSIDE android folder (20 sec)

1. Open File Explorer
2. Go to where you cloned the repo, e.g.:
   `D:\iptv-ultimate\` or `C:\Users\YourName\Desktop\iptv-ultimate\`
3. Double-click into the `android` folder — you should see `build.bat`, `gradlew.bat`, `app` folder, `scripts` folder.
4. Click in the address bar at the top (where it shows the path), type `cmd` and press Enter.

A black window opens and it is ALREADY in the right folder.

---

## Step 2 — Tell Windows where your JDK is (copy paste this)

**In that black window**, copy-paste this line and press Enter. **Use YOUR folder name from Step 0.**

```bat
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot
```

If your folder was `jdk-21.0.12+8` then use:
```bat
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12+8
```

Verify it worked:
```bat
"%JAVA_HOME%\bin\java.exe" -version
```
You should see:
```
openjdk version "21.0.12" ...
Eclipse Temurin ...
```

If that prints, you are good.

---

## Step 3 — Build the signed FireStick APK (the only command)

Still in same black window, now type:

```bat
build
```

And press Enter.

That's it.

Do NOT close the window. What will happen:

1. `[OK] Using JDK: C:\...` -> good
2. `[INFO] No release keystore found - generating one...` -> only first time, it creates `app\dylandos-release.jks` (your private signing key)
3. `[OK] Wrote keystore.properties` -> first time only
4. `> Task :app:assembleFirestickRelease` -> this is Gradle building, first time it downloads Gradle 8.9 (1-3 min, looks stuck but it's working)
5. Finally:
```
[OK] Signed APK created:
     D:\...\android\dist\DylandosIPTV-Firestick-v1.0.0-20250801.apk
[VERIFY] Checking signature...
Verifies
...
DONE
```

First build = 3-5 minutes. Next builds = ~30-60 sec.

---

## Step 4 — Find your APK

Leave the black window open, go to File Explorer:

`android\dist\`

You will see:
```
DylandosIPTV-Firestick-v1.0.0-20250801.apk  (or today's date)
```

That is your **signed release APK**. That's the file you send to FireStick / upload to GitHub.

Double-check size: Right-click -> Properties, should be ~25-60 MB depending on abis.

Optional but recommended — get SHA256 for your release notes:
```bat
certutil -hashfile dist\DylandosIPTV-Firestick-v1.0.0-*.apk SHA256
```

---

## Step 5 — You are DONE — close Android Studio is fine

You do NOT need to open Android Studio at all for this.

If you ever want to rebuild after code changes, just repeat:

```bat
cd /d D:\path\to\iptv-ultimate\android
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot
build
```

### PowerShell version (if you prefer PowerShell)

```powershell
cd D:\path\to\iptv-ultimate\android
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot"
powershell -ExecutionPolicy Bypass -File scripts\build-signed-apk.ps1
```

---

## Troubleshooting for dummies

**`[ERROR] No JDK found`**
You typed wrong JAVA_HOME. Do Step 0 again and copy exact folder name. Must have `\bin\java.exe` inside it.

**`SDK location not found`**
1. Open Android Studio once
2. Open the `android` folder as project
3. It will create `local.properties` automatically
4. Close Android Studio, run `build` again.

Manual fix if needed: Create file `android\local.properties` with:
```
sdk.dir=C\:\\Users\\YOURNAME\\AppData\\Local\\Android\\Sdk
```
Replace YOURNAME with your Windows username. Note double `\\`.

**`build is not recognized`**
You are not inside `android` folder. Make sure you see `build.bat` when you type `dir`.

**Stuck on `Downloading https://...gradle...`**
First build needs internet. Allow through firewall, wait.

**`apksigner not found - skipping`**
Not a problem, APK is still signed and usable. To get verification, install Android SDK Build-Tools from Android Studio > SDK Manager > SDK Tools > Check Android SDK Build-Tools.

**Rebuild fails after you changed password**
Delete `keystore.properties` and `app\dylandos-release.jks` if you want fresh dev key, then run `build` again. But backup old .jks if you already published!
