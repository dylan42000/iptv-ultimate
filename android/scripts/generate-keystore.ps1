# ===========================================================================
#  DYLANDOS IPTV ULTIMATE — Release Keystore Generator (Windows PowerShell)
# ===========================================================================
#  Creates `app/dylandos-release.jks` using keytool from the JDK on PATH.
#  This is the SAME keystore you use to sign every release APK, so store it
#  somewhere safe and back it up. If you lose it you cannot update the app.
#
#  Usage:
#      powershell -ExecutionPolicy Bypass -File scripts\generate-keystore.ps1
#
#  Optional overrides (use the defaults for quick local testing):
#      $env:KS_ALIAS       alias          (default: dylandos)
#      $env:KS_PASSWORD    keystore+pw    (default: dylandos2026)
#      $env:KS_VALIDITY    days           (default: 10000)
# ===========================================================================

$ErrorActionPreference = "Stop"

# --- Locate keytool --------------------------------------------------------
function Get-KeyTool {
    $kt = Get-Command keytool -ErrorAction SilentlyContinue
    if ($kt) { return $kt.Source }

    # Fall back to common JDK install locations.
    $candidates = @(
        "$env:JAVA_HOME\bin\keytool.exe",
        "C:\Program Files\Eclipse Adoptium\jdk-21*\bin\keytool.exe",
        "C:\Program Files\Java\jdk-*\bin\keytool.exe",
        "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe"
    )
    foreach ($pattern in $candidates) {
        $found = Get-ChildItem $pattern -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($found) { return $found.FullName }
    }
    return $null
}

$keytool = Get-KeyTool
if (-not $keytool) {
    Write-Host "[ERROR] keytool not found. Set JAVA_HOME to a JDK 17+ install and re-run." -ForegroundColor Red
    exit 1
}

$alias     = if ($env:KS_ALIAS)    { $env:KS_ALIAS }    else { "dylandos" }
$password  = if ($env:KS_PASSWORD) { $env:KS_PASSWORD } else { "dylandos2026" }
$validity  = if ($env:KS_VALIDITY) { $env:KS_VALIDITY } else { 10000 }

$keystoreDir  = Join-Path $PSScriptRoot "..\app"
$keystorePath = Join-Path $keystoreDir "dylandos-release.jks"
$keystorePath = [System.IO.Path]::GetFullPath($keystorePath)

if (Test-Path $keystorePath) {
    Write-Host "[OK] Keystore already exists: $keystorePath" -ForegroundColor Green
    Write-Host "     (delete it to regenerate, or use the same file to keep the same key)"
    exit 0
}

Write-Host "[INFO] Generating release keystore at $keystorePath" -ForegroundColor Cyan
Write-Host "[INFO] Alias=$alias  Validity=${validity} days"

& $keytool -genkeypair -v `
    -keystore $keystorePath `
    -alias $alias `
    -keyalg RSA `
    -keysize 4096 `
    -validity $validity `
    -storepass $password `
    -keypass $password `
    -dname "CN=Dylandos IPTV, OU=Mobile, O=Dylandos, L=Unknown, ST=Unknown, C=US" `
    --noprompt 2>&1 | ForEach-Object { Write-Host $_ }

if (Test-Path $keystorePath) {
    Write-Host "[OK] Keystore created: $keystorePath" -ForegroundColor Green
    Write-Host ""
    Write-Host "Next step: run scripts\build-signed-apk.ps1  (it will use this keystore automatically)." -ForegroundColor Yellow
} else {
    Write-Host "[ERROR] Keystore generation failed." -ForegroundColor Red
    exit 1
}
