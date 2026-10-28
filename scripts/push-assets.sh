#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

export PATH="$HOME/Library/Android/sdk/platform-tools:$HOME/Library/Android/sdk/emulator:$PATH"

PKG_NAME="org.ioquake.android"
TARGET_DIR="/sdcard/Android/data/$PKG_NAME/files/baseq3"

if ! command -v adb &>/dev/null; then
    echo "Error: adb not found in PATH"
    exit 1
fi

DEVICE_COUNT=$(adb devices | grep -v "List of devices" | grep "device$" | wc -l | tr -d ' ')
if [ "$DEVICE_COUNT" -eq 0 ]; then
    echo "Error: No Android device or emulator connected via adb"
    exit 1
fi

echo "=== Pushing Quake III Arena assets to device ==="
echo "Target directory: $TARGET_DIR"

adb shell "mkdir -p $TARGET_DIR"
for item in "$PROJECT_DIR"/game_data/baseq3/*; do
    if [ -f "$item" ]; then
        echo "Pushing $(basename "$item")..."
        adb push "$item" "$TARGET_DIR/"
    fi
done

# Ensure the app user owns the files
APP_UID=$(adb shell "pm list packages -U org.ioquake.android 2>/dev/null | grep org.ioquake.android" | sed -n 's/.*uid:\([0-9]*\).*/\1/p' | tr -d '\r')
if [ -n "$APP_UID" ]; then
    adb shell "chown -R $APP_UID:$APP_UID $TARGET_DIR 2>/dev/null || true"
fi

echo "=== Done! Assets deployed successfully ==="
