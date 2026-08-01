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
REM ===========================================================================
setlocal EnableExtensions EnableDelayedExpansion
set "ROOT=%~dp0"
cd /d "%ROOT%"

echo.
echo ==========================================================
echo   DYLANDOS IPTV ULTIMATE - Signed FireStick Release Build
echo ==========================================================
echo.

REM ---------- 1. Locate a JDK ------------------------------------------------
set "JAVA_BIN="
if defined JAVA_HOME (
  if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_BIN=%JAVA_HOME%\bin\java.exe"
)
if not defined JAVA_BIN (
  if exist "C:\Program Files\Eclipse Adoptium\jdk-21*\bin\java.exe" (
    for /d %%d in ("C:\Program Files\Eclipse Adoptium\jdk-21*") do set "JAVA_BIN=%%d\bin\java.exe"
  )
)
if not defined JAVA_BIN (
  if exist "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" set "JAVA_BIN=C:\Program Files\Android\Android Studio\jbr\bin\java.exe"
)
if not defined JAVA_BIN (
  echo [ERROR] No JDK found. Install JDK 17+ from https://adoptium.net
  echo         and set JAVA_HOME, e.g.:
  echo            set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot
  goto :fail
)
echo [OK] Using JDK: %JAVA_BIN%

REM ---------- 2. Locate keytool (same JDK) -----------------------------------
set "KEYTOOL=%JAVA_BIN%\..\bin\keytool.exe"

REM ---------- 3. Generate release keystore if missing ------------------------
set "KEYSTORE=%ROOT%app\dylandos-release.jks"
set "ALIAS=dylandos"
set "PASSWORD=dylandos2026"

if not exist "%KEYSTORE%" (
  echo [INFO] No release keystore found - generating one for you...
  "%KEYTOOL%" -genkeypair -v -keystore "%KEYSTORE%" -alias %ALIAS% ^
    -keyalg RSA -keysize 4096 -validity 10000 ^
    -storepass %PASSWORD% -keypass %PASSWORD% ^
    -dname "CN=Dylandos IPTV, OU=Mobile, O=Dylandos, L=Unknown, ST=Unknown, C=US" --noprompt
  if not exist "%KEYSTORE%" (
    echo [ERROR] Keystore generation failed.
    goto :fail
  )
) else (
  echo [OK] Using existing keystore: %KEYSTORE%
)

REM ---------- 4. Write keystore.properties if missing -------------------------
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

REM ---------- 5. Run the FireStick release build ------------------------------
echo.
echo [BUILD] Running: gradlew.bat assembleFirestickRelease
call "%ROOT%gradlew.bat" assembleFirestickRelease
if errorlevel 1 (
  echo [ERROR] Gradle build failed.
  goto :fail
)

REM ---------- 6. Copy the signed APK to dist\ ---------------------------------
set "APK_DIR=%ROOT%app\build\outputs\apk\firestick\release"
set "APK="
for %%f in ("%APK_DIR%\*.apk") do set "APK=%%f"
if not defined APK (
  echo [ERROR] Build finished but no APK found in %APK_DIR%
  goto :fail
)

set "DIST=%ROOT%dist"
if not exist "%DIST%" mkdir "%DIST%"
for /f "tokens=2 delims==" %%i in ('wmic os get localdatetime /value ^| find "="') do set "DT=%%i"
set "STAMP=%DT:~0,8%"
set "DEST=%DIST%\DylandosIPTV-Firestick-v1.0.0-%STAMP%.apk"
copy /y "%APK%" "%DEST%" >nul

echo.
echo [OK] Signed APK created:
echo      %DEST%

REM ---------- 7. Verify signature with apksigner if available ------------------
set "APKSIGNER="
for /d %%d in ("%LOCALAPPDATA%\Android\Sdk\build-tools\*") do if exist "%%d\apksigner.bat" set "APKSIGNER=%%d\apksigner.bat"
if defined APKSIGNER (
  echo.
  echo [VERIFY] Checking signature...
  call "%APKSIGNER%" verify --verbose "%DEST%"
) else (
  echo [INFO] apksigner not found - skipping automatic verify.
)

echo.
echo ==========================================================
echo   DONE - transfer %DEST% to your FireStick.
echo ==========================================================
echo.
goto :eof

:fail
echo.
echo Build did not complete. See the error above.
echo.
exit /b 1
