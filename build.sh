#!/usr/bin/env bash
set -euo pipefail

export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"

cd "$(dirname "$0")"
./gradlew :app:assembleRelease

# Find the generated APK (handles both app-release.apk and app-release-unsigned.apk)
APK_PATH=$(find app/build/outputs/apk/release -name "*.apk" | head -n 1)

if [ -n "$APK_PATH" ]; then
    cp "$APK_PATH" ./launcher.apk
    echo "Success! APK moved to: ./launcher.apk"
else
    echo "Error: No APK found in app/build/outputs/apk/release/"
    exit 1
fi
