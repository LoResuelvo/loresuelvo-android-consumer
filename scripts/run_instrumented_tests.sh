#!/bin/bash
set -e

FLAVOR="${1:-Dev}"
DEVICE_MODE="${INSTRUMENTED_DEVICE:-managed}"

if [[ "$DEVICE_MODE" == "managed" ]]; then
  TEST_TASK="pixel2Api35${FLAVOR}DebugAndroidTest"
elif [[ "$DEVICE_MODE" == "connected" ]]; then
  TEST_TASK="connected${FLAVOR}DebugAndroidTest"
else
  echo "Unsupported INSTRUMENTED_DEVICE: $DEVICE_MODE"
  echo "Use managed (default) or connected."
  exit 2
fi

echo "======================================="
echo "Running Instrumented Tests (flavor: $FLAVOR)"
echo "Device mode: $DEVICE_MODE"
echo "======================================="

./gradlew "$TEST_TASK" \
  -Pandroid.testInstrumentationRunnerArguments.package=com.loresuelvo.consumer.instrumented

echo ""
echo "Instrumented Tests Completed"
