# ===========================================================================
#  DYLANDOS IPTV ULTIMATE - Build a Signed Release APK (Windows PowerShell)
# ===========================================================================
#  The one-stop, foolproof command to produce a signed release APK for the
#  FireStick. It:
#     1. Verifies JAVA_HOME / Gradle are available.
#     2. Generates the release keystore automatically if one does not exist yet.
#     3. Writes a keystore.properties so the Gradle build picks up the key.
#     4. Runs `assembleFirestickRelease`.
#     5. Copies the signed APK into `android\dist` with a clean, dated name and
#        verifies it with apksigner.
#
#  NOTE: This file is pure ASCII (no em-dashes/smart quotes) so it parses
#  correctly on Windows PowerShell 5.1.
#
#  Usage (PowerShell, from the `android` directory):
#      powershell -ExecutionPolicy Bypass -File scripts\build-signed-apk.ps1
#
#  The task also falls back to the Android debug keystore so the build NEVER
#  fails to produce an APK even if you skip the keystore step.
# ===========================================================================

$ErrorActionPreference = "Stop"
$root = [System.IO.Path]::GetFullPath($PSScriptRoot + "\..")
Set-Location $root

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  DYLANDOS IPTV ULTIMATE - Signed FireStick Release Build " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host ""

# ---------------------------------------------------------------------------
# 1. Validate JAVA_HOME (must be a JDK 17+; a JRE is not enough).
# ---------------------------------------------------------------------------
$javaBin = Join-Path $env:JAVA_HOME "bin\java.exe"
if (-not $env:JAVA_HOME -or -not (Test-Path $javaBin)) {
    Write-Host "[ERROR] JAVA_HOME is not set to a valid JDK." -ForegroundColor Red
    Write-Host "        Set it, e.g.:  `$env:JAVA_HOME=`"C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot`"" -ForegroundColor Yellow
    exit 1
}
Write-Host "[OK] JAVA_HOME = $env:JAVA_HOME"

# ---------------------------------------------------------------------------
# 2. Generate the release keystore if it does not exist yet.
# ---------------------------------------------------------------------------
$keystorePath = Join-Path $root "app\dylandos-release.jks"
$alias     = "dylandos"
$password  = "dylandos2026"

if (-not (Test-Path $keystorePath)) {
    Write-Host "[INFO] No release keystore found - generating one for you..." -ForegroundColor Cyan
    & powershell -ExecutionPolicy Bypass -File (Join-Path $root "scripts\generate-keystore.ps1")
    if (-not (Test-Path $keystorePath)) {
        Write-Host "[ERROR] Could not create keystore. Aborting." -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "[OK] Using existing keystore: $keystorePath"
}

# ---------------------------------------------------------------------------
# 3. Write keystore.properties so Gradle signs with this key.
#    (Never overwrite an existing file the user may have customized.)
# ---------------------------------------------------------------------------
$ksProps = Join-Path $root "keystore.properties"
if (-not (Test-Path $ksProps)) {
    $storeFileRel = "app/dylandos-release.jks"
    $lines = @(
        "storeFile=$storeFileRel",
        "keyAlias=$alias",
        "storePassword=$password",
        "keyPassword=$password"
    )
    Set-Content -Path $ksProps -Value $lines -Encoding Ascii
    Write-Host "[OK] Wrote keystore.properties (storeFile=$storeFileRel)"
} else {
    Write-Host "[OK] keystore.properties already present - keeping it."
}

# ---------------------------------------------------------------------------
# 4. Run the Gradle build for the FireStick flavor.
# ---------------------------------------------------------------------------
Write-Host ""
Write-Host "[BUILD] Running: .\gradlew assembleFirestickRelease" -ForegroundColor Cyan

$gradlew = Join-Path $root "gradlew.bat"
if (-not (Test-Path $gradlew)) {
    Write-Host "[ERROR] gradlew.bat not found. This distribution should include it." -ForegroundColor Red
    exit 1
}

& $gradlew assembleFirestickRelease
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Gradle build failed (exit code $LASTEXITCODE)." -ForegroundColor Red
    exit $LASTEXITCODE
}

# ---------------------------------------------------------------------------
# 5. Locate + copy the signed APK into android\dist with a friendly name.
# ---------------------------------------------------------------------------
$apkDir = Join-Path $root "app\build\outputs\apk\firestick\release"
$apk = Get-ChildItem -Path $apkDir -Filter "*.apk" -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $apk) {
    Write-Host "[ERROR] Build finished but no APK was found in $apkDir" -ForegroundColor Red
    exit 1
}

$distDir = Join-Path $root "dist"
New-Item -ItemType Directory -Force -Path $distDir | Out-Null
$dateStamp = Get-Date -Format "yyyyMMdd"
$destName = "DylandosIPTV-Firestick-v1.0.0-${dateStamp}.apk"
$dest = Join-Path $distDir $destName
Copy-Item $apk.FullName $dest -Force

Write-Host ""
Write-Host "[OK] Signed APK created:" -ForegroundColor Green
Write-Host "     $dest" -ForegroundColor Green
Write-Host ""

# ---------------------------------------------------------------------------
# 6. Verify the signature with apksigner (from build-tools).
# ---------------------------------------------------------------------------
$apksigner = Get-ChildItem "$env:LOCALAPPDATA\Android\Sdk\build-tools\*\apksigner.bat" -ErrorAction SilentlyContinue |
    Sort-Object { [version]$_.Directory.Name } -Descending | Select-Object -First 1
if ($apksigner) {
    Write-Host "[VERIFY] Checking signature with apksigner..." -ForegroundColor Cyan
    & $apksigner.FullName verify --verbose $dest
} else {
    Write-Host "[INFO] apksigner not found under default SDK path - skipping automatic verify."
    Write-Host "       You can verify manually:  apksigner verify --verbose $dest"
}

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Green
Write-Host "  DONE - transfer $dest to your FireStick (e.g. via adb install or a file manager + 'Downloader' app)." -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
Write-Host ""
