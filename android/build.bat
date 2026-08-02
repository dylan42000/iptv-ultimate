@echo off
REM ===========================================================================
REM  DYLANDOS IPTV ULTIMATE - Build Signed FireStick Release APK (Command Prompt)
REM ===========================================================================
REM  The simplest possible way to build a signed FireStick APK. Run this from
REM  the `android` folder by typing:  build
REM
REM  It will:
REM    1. Locate a JDK (uses JAVA_HOME if set, else common Adoptium/Studio paths).
REM    2. Auto-generate the signing keystore if one does not exist (keytool).
REM    3. Write keystore.properties if missing.
REM    4. Run gradlew assembleFirestickRelease.
REM    5. Copy the signed APK to android\dist\.
REM    6. Verify the signature with apksigner if it can find it.
REM
REM  FIXED for JDK 21.0.12+8 (folder name with +), spaces in project path,
REM  and Android Studio JBR fallback that lacks keytool.
REM ===========================================================================
setlocal EnableExtensions EnableDelayedExpansion
set "ROOT=%~dp0"
cd /d "%ROOT%"

echo.
echo ==========================================================
echo   DYLANDOS IPTV ULTIMATE - Signed FireStick Release Build
echo   FIXED build.bat for Temurin 21.0.12+8 + spaces in path
echo ==========================================================
echo.

REM ---------- 1. Locate a JDK ------------------------------------------------
set "JAVA_BIN="
set "KEYTOOL="
set "JAVA_HOME_CLEAN="

REM If JAVA_HOME is set, clean quotes and use it FIRST (highest priority)
if defined JAVA_HOME (
  REM Remove surrounding quotes if user pasted with quotes and get full path
  for %%I in ("%JAVA_HOME%") do set "JAVA_HOME_CLEAN=%%~fI"
  REM Trim trailing backslash
  if "!JAVA_HOME_CLEAN:~-1!"=="\" set "JAVA_HOME_CLEAN=!JAVA_HOME_CLEAN:~0,-1!"
  echo [INFO] JAVA_HOME detected as: !JAVA_HOME_CLEAN!
  if exist "!JAVA_HOME_CLEAN!\bin\java.exe" (
    set "JAVA_BIN=!JAVA_HOME_CLEAN!\bin\java.exe"
    set "KEYTOOL=!JAVA_HOME_CLEAN!\bin\keytool.exe"
    set "JAVA_HOME=!JAVA_HOME_CLEAN!"
    echo [OK] Using JAVA_HOME JDK: !JAVA_BIN!
  ) else (
    echo [WARN] JAVA_HOME=!JAVA_HOME_CLEAN! but bin\java.exe not found, ignoring JAVA_HOME and searching...
    set "JAVA_HOME="
    set "JAVA_HOME_CLEAN="
  )
)

REM Try Adoptium - search all jdk* folders, newest wins (loop overrides)
if not defined JAVA_BIN (
  echo [INFO] Searching C:\Program Files\Eclipse Adoptium\ for Temurin JDK 21...
  for /d %%d in ("C:\Program Files\Eclipse Adoptium\jdk*") do (
    if exist "%%d\bin\java.exe" (
      set "JAVA_BIN=%%d\bin\java.exe"
      set "KEYTOOL=%%d\bin\keytool.exe"
      set "JAVA_HOME=%%d"
      echo [FOUND] %%d
    )
  )
  REM Also check temurin* naming variant
  for /d %%d in ("C:\Program Files\Eclipse Adoptium\temurin*") do (
    if exist "%%d\bin\java.exe" (
      set "JAVA_BIN=%%d\bin\java.exe"
      set "KEYTOOL=%%d\bin\keytool.exe"
      set "JAVA_HOME=%%d"
      echo [FOUND] %%d
    )
  )
  REM Wildcard for 21.x specifically if user installed elsewhere (robust)
  for /d %%d in ("C:\Program Files\Eclipse Adoptium\*21*") do (
    if exist "%%d\bin\java.exe" (
      if exist "%%d\bin\keytool.exe" (
        set "JAVA_BIN=%%d\bin\java.exe"
        set "KEYTOOL=%%d\bin\keytool.exe"
        set "JAVA_HOME=%%d"
        echo [FOUND] %%d
      )
    )
  )
)

REM Fallback to Android Studio JBR ONLY as last resort - it often lacks keytool
if not defined JAVA_BIN (
  if exist "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" (
    set "JAVA_BIN=C:\Program Files\Android\Android Studio\jbr\bin\java.exe"
    set "KEYTOOL=C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe"
    for %%I in ("!JAVA_BIN!") do set "JAVA_HOME=%%~dpI.."
    for %%J in ("!JAVA_HOME!") do set "JAVA_HOME=%%~fJ"
    echo [WARN] Using Android Studio JBR fallback: !JAVA_BIN!
    echo [WARN] JBR may not have keytool.exe - if it fails, install Temurin JDK 21
  )
)

if not defined JAVA_BIN (
  echo [ERROR] No JDK found. Install JDK 17+ from https://adoptium.net
  echo         Then set JAVA_HOME, e.g.:
  echo            set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot
  echo         Your current Adoptium folder list:
  dir "C:\Program Files\Eclipse Adoptium\" 2>nul
  goto :fail
)

REM If KEYTOOL still not defined, derive from JAVA_BIN folder
if not defined KEYTOOL (
  for %%I in ("!JAVA_BIN!") do set "KEYTOOL=%%~dpIkeytool.exe"
)

echo [OK] Using JDK: !JAVA_BIN!
echo [OK] Using keytool: !KEYTOOL!
echo [OK] JAVA_HOME will be: !JAVA_HOME!
echo.

if not exist "!JAVA_BIN!" (
  echo [ERROR] JAVA_BIN not found: !JAVA_BIN!
  goto :fail
)
if not exist "!KEYTOOL!" (
  echo [ERROR] keytool.exe not found at !KEYTOOL!
  echo         Android Studio JBR does NOT include keytool in newer versions.
  echo         You MUST use Eclipse Temurin JDK 21 Hotspot.
  echo         Fix:
  echo            set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot
  echo         Then run: build
  echo         List your Adoptium installs:
  dir "C:\Program Files\Eclipse Adoptium\" 2>nul
  goto :fail
)

REM Ensure JAVA_HOME is set for Gradle
if not defined JAVA_HOME (
  for %%I in ("!JAVA_BIN!") do set "JAVA_HOME=%%~dpI.."
  for %%J in ("!JAVA_HOME!") do set "JAVA_HOME=%%~fJ"
)

REM ---------- 2. Generate release keystore if missing ------------------------
set "KEYSTORE=%ROOT%app\dylandos-release.jks"
set "ALIAS=dylandos"
set "PASSWORD=dylandos2026"

if not exist "%KEYSTORE%" (
  echo [INFO] No release keystore found - generating one for you...
  echo [INFO] Keystore: %KEYSTORE%
  echo [CMD] "!KEYTOOL!" -genkeypair -v -keystore "%KEYSTORE%" -alias %ALIAS% -keyalg RSA -keysize 4096 -validity 10000 -storepass %PASSWORD% -keypass %PASSWORD% -dname "CN=Dylandos IPTV, OU=Mobile, O=Dylandos, L=Unknown, ST=Unknown, C=US" --noprompt
  "!KEYTOOL!" -genkeypair -v -keystore "%KEYSTORE%" -alias %ALIAS% -keyalg RSA -keysize 4096 -validity 10000 -storepass %PASSWORD% -keypass %PASSWORD% -dname "CN=Dylandos IPTV, OU=Mobile, O=Dylandos, L=Unknown, ST=Unknown, C=US"
  if not exist "%KEYSTORE%" (
    echo [ERROR] Keystore generation failed. keytool returned error.
    echo         Try running manually:
    echo         "!KEYTOOL!" -genkeypair -v -keystore "%KEYSTORE%" -alias dylandos -keyalg RSA -keysize 4096 -validity 10000 -storepass dylandos2026 -keypass dylandos2026 -dname "CN=Dylandos IPTV, OU=Mobile, O=Dylandos, L=Unknown, ST=Unknown, C=US"
    goto :fail
  )
  echo [OK] Keystore created.
) else (
  echo [OK] Using existing keystore: %KEYSTORE%
)

REM ---------- 3. Write keystore.properties if missing -------------------------
set "KSPROPS=%ROOT%keystore.properties"
if not exist "%KSPROPS%" (
  (
    echo storeFile=app/dylandos-release.jks
    echo keyAlias=%ALIAS%
    echo storePassword=%PASSWORD%
    echo keyPassword=%PASSWORD%
  ) > "%KSPROPS%"
  echo [OK] Wrote keystore.properties
) else (
  echo [OK] keystore.properties already present - keeping it.
)

REM ---------- 4. Run the FireStick release build ------------------------------
echo.
echo [BUILD] Running: gradlew.bat assembleFirestickRelease
echo [BUILD] JAVA_HOME=%JAVA_HOME%
call "%ROOT%gradlew.bat" assembleFirestickRelease
if errorlevel 1 (
  echo [ERROR] Gradle build failed.
  goto :fail
)

REM ---------- 5. Copy the signed APK to dist\ ---------------------------------
set "APK_DIR=%ROOT%app\build\outputs\apk\firestick\release"
set "APK="
for %%f in ("%APK_DIR%\*.apk") do set "APK=%%f"
if not defined APK (
  echo [ERROR] Build finished but no APK found in %APK_DIR%
  dir "%APK_DIR%" 2>nul
  goto :fail
)

set "DIST=%ROOT%dist"
if not exist "%DIST%" mkdir "%DIST%"
REM Use powershell for date if wmic is gone on Win11 24H2
set "STAMP="
for /f "usebackq delims=" %%i in (`powershell -NoProfile -Command "Get-Date -Format yyyyMMdd"`) do set "STAMP=%%i"
if not defined STAMP set "STAMP=20250801"
set "DEST=%DIST%\DylandosIPTV-Firestick-v1.0.0-%STAMP%.apk"
copy /y "%APK%" "%DEST%" >nul

echo.
echo [OK] Signed APK created:
echo      %DEST%
dir "%DEST%" | findstr /i "Dylandos"

REM ---------- 6. Verify signature with apksigner if available ------------------
set "APKSIGNER="
for /d %%d in ("%LOCALAPPDATA%\Android\Sdk\build-tools\*") do if exist "%%d\apksigner.bat" set "APKSIGNER=%%d\apksigner.bat"
if defined APKSIGNER (
  echo.
  echo [VERIFY] Checking signature...
  call "%APKSIGNER%" verify --verbose "%DEST%"
) else (
  echo [INFO] apksigner not found - skipping automatic verify.
  echo [INFO] Install build-tools in Android Studio SDK Manager to get apksigner.
)

echo.
echo ==========================================================
echo   DONE - transfer %DEST% to your FireStick.
echo   Example: adb install -r "%DEST%"
echo ==========================================================
echo.
goto :eof

:fail
echo.
echo Build did not complete. See the error above.
echo.
echo QUICK FIX for your case (JDK 21.0.12+8):
echo    1. Open File Explorer to C:\Program Files\Eclipse Adoptium\
echo    2. Copy the exact folder name (e.g. jdk-21.0.12.8-hotspot)
echo    3. In this SAME black window run:
echo       set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot
echo       build
echo.
exit /b 1
