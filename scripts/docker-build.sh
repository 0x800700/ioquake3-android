#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

mkdir -p "$HOME/.gradle"
mkdir -p "$HOME/.android"

echo "=== Running Gradle inside Docker (ioq3-android-builder) ==="
docker run --platform linux/amd64 --rm \
    -v "$PROJECT_DIR":/workspace \
    -v "$HOME/.gradle":/root/.gradle \
    -v "$HOME/.android":/root/.android \
    -w /workspace/android \
    ioq3-android-builder \
    ./gradlew "$@"
