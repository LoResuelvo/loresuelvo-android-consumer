---
name: android-testing-gates
description: Run and interpret Android unit, build, lint, and instrumented validation gates before pushing, releasing, or merging changes.
metadata:
  short-description: Validate Android changes before handoff
---

# Android testing gates

Load this skill before pushing or merging an Android change, or when diagnosing
test execution. Use focused tests during iteration and the complete gates
before handoff.

## Standard gates

This repository does not provide a `Makefile`; use the Gradle tasks below:

```bash
./gradlew :app:testDevDebugUnitTest --no-daemon
./gradlew :app:assembleDevDebug --no-daemon
./gradlew :app:lintDevDebug --no-daemon
```

Run connected instrumented tests when a user flow, Compose screen, navigation
graph, Activity, or instrumented test changed:

```bash
./gradlew :app:connectedDevDebugAndroidTest --no-daemon
```

Use the managed-device task configured by the project when available. For a
physical device or manually started emulator, verify `adb devices` first and
report device lifecycle or package-variant failures separately from code
failures.

## Focused validation

```bash
./gradlew :app:testDevDebugUnitTest \
  --tests "*CompleteProfileViewModelTest*" \
  --no-daemon
```

Run focused tests after a fix, then rerun the complete affected gate. Do not
add sleeps or inflate timeouts to hide nondeterminism; prefer deterministic
fakes, Compose idling, and coroutine test schedulers.

## Failure handling

- Stop at the first actionable failure, identify whether it is code,
  configuration, or device state, and rerun the smallest affected gate.
- Do not claim a gate passed when the process was interrupted or the device
  suite was stopped.
- Do not merge or release with a failing relevant instrumented test unless the
  failure is explicitly documented as an external environment blocker and the
  owner has accepted that risk.
- Keep secrets, tokens, and payloads out of logs and tests.

## Final checklist

- Unit tests pass for the changed behavior.
- `assembleDevDebug` passes.
- `lintDevDebug` passes.
- Instrumented tests were run when UI/navigation wiring changed, or the
  environment blocker is recorded.
- Generated files and debug artifacts are absent from the diff.
- `git diff --check` is clean.
