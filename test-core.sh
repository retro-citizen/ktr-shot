#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")"
JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"
JAVAC="${JAVA_HOME:+$JAVA_HOME/bin/}javac"
mkdir -p build/core-tests
"$JAVAC" -d build/core-tests \
    app/src/main/java/dev/ktr/shot/KeyChord.java \
    tests/CoreTest.java
"$JAVA" -cp build/core-tests dev.ktr.shot.CoreTest