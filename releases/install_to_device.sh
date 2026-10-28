#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
APK_PATH="$SCRIPT_DIR/ioquake3-v1.0.0-release.apk"

echo "=== Quake III Arena Android Auto-Installer ==="

# Check ADB
if ! command -v adb &> /dev/null; then
    echo "Error: adb not found. Please install Android Platform Tools or connect your phone."
    exit 1
fi

DEVICE_COUNT=$(adb devices | grep -v "List" | grep "device$" | wc -l | tr -d ' ')

if [ "$DEVICE_COUNT" -eq 0 ]; then
    echo "No connected Android devices found. Connect your phone via USB with USB Debugging enabled, or via wireless adb."
    exit 1
fi

echo "Installing $APK_PATH..."
adb install -r "$APK_PATH"

echo "Creating game folders on device..."
adb shell mkdir -p /sdcard/Android/data/org.ioquake.android/files/baseq3
adb shell mkdir -p /sdcard/Android/data/org.ioquake.android/files/osp

echo ""
read -p "Do you want to also push game data (baseq3 + osp, ~530MB) to the device? (y/n): " PUSH_DATA
if [[ "$PUSH_DATA" =~ ^[Yy]$ ]]; then
    echo "Pushing baseq3..."
    adb push "$PROJECT_DIR/game_data/baseq3/." /sdcard/Android/data/org.ioquake.android/files/baseq3/
    echo "Pushing osp..."
    adb push "$PROJECT_DIR/game_data/osp/." /sdcard/Android/data/org.ioquake.android/files/osp/
    echo "Game data copied successfully!"
fi

echo "=== All done! You can now start Quake III Arena on your device! ==="
