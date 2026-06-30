#!/usr/bin/env bash
# Release build → install on the connected device.
# Note: release is signed with the debug key (see app/build.gradle.kts) so it installs without
# keystore setup. Swap in a real keystore before distributing.
# Usage: ./install.sh
set -euo pipefail
cd "$(dirname "$0")"

PKG="com.musiclock"
MAIN="$PKG/com.musiclock.ui.permissions.MainActivity"

echo "==> Building release APK (minify + shrink)"
./gradlew :app:assembleRelease

APK=app/build/outputs/apk/release/app-release.apk
[ -f "$APK" ] || APK=app/build/outputs/apk/release/app-release-unsigned.apk
echo "==> APK: $APK"

echo "==> Checking for a connected device"
adb get-state >/dev/null 2>&1 || { echo "No device/emulator. Connect one and retry. (APK is built at $APK)"; exit 0; }

echo "==> Installing"
adb install -r "$APK"

echo "==> Launching"
adb shell am start -n "$MAIN" || true
echo "Done. Grant permissions in-app (Notification access, Display over apps, Full-screen intent)."
