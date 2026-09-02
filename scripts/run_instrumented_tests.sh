#!/bin/bash
set -e

FLAVOR="${1:-Dev}"

echo "======================================="
echo "Running Instrumented Tests (flavor: $FLAVOR)"
echo "======================================="

./gradlew "connected${FLAVOR}DebugAndroidTest" \
  -Pandroid.testInstrumentationRunnerArguments.package=com.loresuelvo.consumer.instrumented

echo ""
echo "Instrumented Tests Completed"
