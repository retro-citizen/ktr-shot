#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")"
bash test-core.sh
./gradlew :app:assembleDebug :app:lintDebug --console=plain
mkdir -p dist
cp app/build/outputs/apk/debug/app-debug.apk dist/KTR-Shot-0.3.1-debug.apk