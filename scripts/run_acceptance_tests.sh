#!/bin/bash

set -e

echo "======================================="
echo "Running Acceptance Tests"
echo "======================================="

./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.package=com.loresuelvo.consumer.acceptance

echo ""
echo "Acceptance Tests Completed"