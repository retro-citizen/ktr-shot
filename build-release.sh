#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")"
: "${ANDROID_HOME:?Set ANDROID_HOME to the Android SDK directory}"
: "${KTR_KEYSTORE_PATH:?Set KTR_KEYSTORE_PATH to an absolute keystore path}"
: "${KTR_KEYSTORE_PASSWORD_FILE:?Set KTR_KEYSTORE_PASSWORD_FILE to an absolute password file path}"
TOOLS="$ANDROID_HOME/build-tools/35.0.0"
OUTPUT="dist/KTR-Shot-0.3.1-KTR2-release.apk"
export PATH="${JAVA_HOME:+$JAVA_HOME/bin:}$PATH"
bash test-core.sh
./gradlew :app:assembleRelease :app:lintRelease --console=plain
mkdir -p dist
"$TOOLS/zipalign" -f -p 4 app/build/outputs/apk/release/app-release-unsigned.apk dist/aligned-release.apk
set --
if [[ -n "${KTR_KEY_PASSWORD_FILE:-}" && "$KTR_KEY_PASSWORD_FILE" != "$KTR_KEYSTORE_PASSWORD_FILE" ]]; then
    set -- --key-pass "file:$KTR_KEY_PASSWORD_FILE"
fi
"$TOOLS/apksigner" sign \
    --ks "$KTR_KEYSTORE_PATH" \
    --ks-key-alias "${KTR_KEY_ALIAS:-ktr-shot}" \
    --ks-pass "file:$KTR_KEYSTORE_PASSWORD_FILE" \
    "$@" \
    --out "$OUTPUT" dist/aligned-release.apk
"$TOOLS/apksigner" verify --verbose --print-certs "$OUTPUT"
"$TOOLS/zipalign" -c -p 4 "$OUTPUT"
cd dist
shasum -a 256 KTR-Shot-0.3.1-KTR2-release.apk > SHA256SUMS