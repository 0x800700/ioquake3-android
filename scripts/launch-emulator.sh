#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

export PATH="$HOME/Library/Android/sdk/platform-tools:$HOME/Library/Android/sdk/emulator:$PATH"

EMULATOR="$HOME/Library/Android/sdk/emulator/emulator"
AVD_NAME="Quake_ARM64"

if [ ! -f "$EMULATOR" ]; then
    echo "Error: Android emulator not found at $EMULATOR"
    exit 1
fi

echo "=== Checking running devices ==="
if adb devices | grep -q "emulator-"; then
    echo "Emulator is already running!"
else
    echo "=== Launching Android Emulator: $AVD_NAME ==="
    "$EMULATOR" -avd "$AVD_NAME" -gpu host > /dev/null 2>&1 &
    
    echo "Waiting for emulator to connect to adb..."
    adb wait-for-device
    
    echo "Waiting for Android boot to complete..."
    while [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" != "1" ]; do
        sleep 2
    done
    echo "Android booted successfully!"
fi

echo "=== Installing ioquake3 APK ==="
adb install -r "$PROJECT_DIR/android/app/build/outputs/apk/debug/app-debug.apk"

echo "=== Deploying Quake III Arena assets ==="
"$SCRIPT_DIR/push-assets.sh"

echo "=== Launching ioquake3 ==="
adb shell am start -n org.ioquake.android/.MainActivity

echo "=== Streaming logs (Ctrl+C to exit) ==="
adb logcat -s ioquake3:V SDL:V

