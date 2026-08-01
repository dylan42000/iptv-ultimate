#!/usr/bin/env bash
# ===========================================================================
#  DYLANDOS IPTV ULTIMATE — Build a Signed Release APK (macOS / Linux)
# ===========================================================================
#  Foolproof one-stop script to produce a signed release APK for the FireStick.
#  See scripts/build-signed-apk.ps1 for the Windows equivalent.
#
#  Usage:
#      cd android
#      chmod +x scripts/build-signed-apk.sh
#      ./scripts/build-signed-apk.sh
#
#  Requires a JDK 17+ on JAVA_HOME (or `java`/`keytool` on PATH) and the Android
#  SDK (via ANDROID_HOME or local.properties).
# ===========================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

echo ""
echo "=========================================================="
echo "  DYLANDOS IPTV ULTIMATE — Signed FireStick Release Build"
echo "=========================================================="
echo ""

# --- 1. Validate a JDK ------------------------------------------------------
if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
  echo "[OK] JAVA_HOME = $JAVA_HOME"
elif command -v java >/dev/null 2>&1; then
  echo "[OK] java on PATH: $(command -v java)"
else
  echo "[ERROR] No JDK found. Set JAVA_HOME to a JDK 17+ install and re-run." >&2
  exit 1
fi

# --- 2. Generate release keystore if missing --------------------------------
KEYSTORE="$ROOT/app/dylandos-release.jks"
ALIAS="${KS_ALIAS:-dylandos}"
PASSWORD="${KS_PASSWORD:-dylandos2026}"
VALIDITY="${KS_VALIDITY:-10000}"

if [ ! -f "$KEYSTORE" ]; then
  echo "[INFO] No release keystore found — generating one for you..."
  keytool -genkeypair -v \
    -keystore "$KEYSTORE" \
    -alias "$ALIAS" \
    -keyalg RSA -keysize 4096 \
    -validity "$VALIDITY" \
    -storepass "$PASSWORD" -keypass "$PASSWORD" \
    -dname "CN=Dylandos IPTV, OU=Mobile, O=Dylandos, L=Unknown, ST=Unknown, C=US" \
    --noprompt
fi
echo "[OK] Using keystore: $KEYSTORE"

# --- 3. Write keystore.properties if missing --------------------------------
KSPROPS="$ROOT/keystore.properties"
if [ ! -f "$KSPROPS" ]; then
  cat > "$KSPROPS" <<EOF
storeFile=app/dylandos-release.jks
keyAlias=$ALIAS
storePassword=$PASSWORD
keyPassword=$PASSWORD
EOF
  echo "[OK] Wrote keystore.properties"
else
  echo "[OK] keystore.properties already present — keeping it."
fi

# --- 4. Run the FireStick release build -------------------------------------
echo ""
echo "[BUILD] Running: ./gradlew assembleFirestickRelease"
chmod +x gradlew
./gradlew assembleFirestickRelease

# --- 5. Locate + copy the signed APK to android/dist ------------------------
APK="$(ls "$ROOT"/app/build/outputs/apk/firestick/release/*.apk 2>/dev/null | head -n1 || true)"
if [ -z "$APK" ]; then
  echo "[ERROR] Build finished but no APK found under app/build/outputs/apk/firestick/release" >&2
  exit 1
fi

DIST="$ROOT/dist"
mkdir -p "$DIST"
DATE=$(date +%Y%m%d)
DEST="$DIST/DylandosIPTV-Firestick-v1.0.0-${DATE}.apk"
cp "$APK" "$DEST"

echo ""
echo "[OK] Signed APK created:"
echo "     $DEST"

# --- 6. Verify signature if apksigner is available --------------------------
APKSIGNER="$(ls "$ANDROID_HOME"/build-tools/*/apksigner 2>/dev/null | sort -V | tail -n1 || true)"
if [ -n "$APKSIGNER" ] && [ -x "$APKSIGNER" ]; then
  echo "[VERIFY] Checking signature with apksigner..."
  "$APKSIGNER" verify --verbose "$DEST"
else
  echo "[INFO] apksigner not found — skipping automatic verify."
  echo "       Manual: apksigner verify --verbose $DEST"
fi

echo ""
echo "=========================================================="
echo "  DONE — transfer $DEST to your FireStick."
echo "=========================================================="
echo ""
