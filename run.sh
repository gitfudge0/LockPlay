#!/usr/bin/env bash
# Debug build → install → grant test permissions via adb → launch → tail logs.
# Usage: ./run.sh
set -euo pipefail
cd "$(dirname "$0")"

PKG="com.lockplay.debug"                       # debug build has .debug applicationId suffix
LISTENER="$PKG/com.lockplay.media.MediaListenerService"
MAIN="$PKG/com.lockplay.ui.permissions.MainActivity"

echo "==> Building debug APK"
./gradlew :app:assembleDebug

APK=app/build/outputs/apk/debug/app-debug.apk
echo "==> Checking for a connected device"
adb get-state >/dev/null 2>&1 || { echo "No device/emulator. Connect one (adb devices) and retry."; exit 1; }

echo "==> Installing $APK"
adb install -r -g "$APK"

# ponytail: grant what adb can so you can test without tapping through every system screen.
# These mirror the in-app PermissionGate; some OEMs still require manual confirmation.
echo "==> Granting test permissions (best-effort)"
adb shell pm grant "$PKG" android.permission.POST_NOTIFICATIONS 2>/dev/null || true
adb shell appops set "$PKG" SYSTEM_ALERT_WINDOW allow 2>/dev/null || true
adb shell appops set "$PKG" USE_FULL_SCREEN_INTENT allow 2>/dev/null || true
adb shell cmd notification allow_listener "$LISTENER" 2>/dev/null || true
adb shell dumpsys deviceidle whitelist +"$PKG" 2>/dev/null || true

echo "==> Launching"
adb shell am start -n "$MAIN"

echo "==> Tailing logs (Ctrl-C to stop). Lock the phone while music plays to trigger the lockscreen."
adb logcat -c
adb logcat MediaListenerService:* LockLauncher:* LockscreenActivity:* AndroidRuntime:E "*:S"
