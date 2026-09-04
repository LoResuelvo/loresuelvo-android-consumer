# android-testing-gates

Load this skill before a PR, release, merge to `main`, or when diagnosing test
execution time. Do not load it for a small local iteration; use the BDD/TDD
skill instead.

## Test layers

- `make test`: JVM unit tests and Cucumber acceptance scenarios; no device required.
- `make instrumented`: Compose/Espresso tests under `androidTest` using the
  `pixel2Api35` Gradle Managed Device by default.
- `make build`: debug APK compilation and generated code validation.
- `make lint`: Android Lint.
- `make test-all-once`: JVM tests plus instrumented tests in one Gradle invocation.
- `make ci`: build, lint, and the complete test gate.

## Required gates

Before a PR:

```bash
make lint
make test
make build
```

Run `make instrumented` when a user flow, navigation graph, Activity, or
instrumented test changed.

Before merging to `main`:

```bash
make ci
```

Stop at the first failure, fix it, and rerun only the affected gate first.
Do not merge with a failing instrumented test.

## Focused commands

```bash
./gradlew :app:testDevDebugUnitTest \
  --tests "*CompleteProfileViewModelTest*"

./gradlew :app:pixel2Api35DevDebugAndroidTest \
  --tests "*CompleteProfileScreenInstrumentedTest*"
```

For a physical device or manually started emulator, use
`INSTRUMENTED_DEVICE=connected make instrumented`. For local API testing with
a physical device, prefer `adb reverse tcp:8080 tcp:8080` and rebuild `devDebug`
after changing `API_URL`.

GitHub Actions uses `INSTRUMENTED_DEVICE=connected` against a prewarmed Pixel 2
API 35 AVD with the `ci-clean` snapshot. Regenerate it from the manual
`Bootstrap CI AVD` workflow when the emulator configuration changes.

## Performance diagnosis

Measure layers separately:

```bash
time make test
time make instrumented
time make test-all-once
```

Do not add arbitrary sleeps or increase timeouts to hide slow tests. Prefer
Compose idling, coroutine test schedulers, deterministic fakes, and targeted
test filters.

## Common failures

- Managed Device startup failure: verify that the Android 35 `aosp`
  `x86_64` system image is installed and that hardware virtualization is
  available.
- Hilt startup failure: verify `HiltTestRunner`, `@HiltAndroidTest`, and the
  `HiltAndroidRule` order.
- `MockWebServer` port conflict: shut down the server in teardown.
- Local HTTP blocked: use the `dev` network-security overlay only.

## Review checklist

- No production secrets or token/payload logging.
- JVM tests do not depend on a device or real backend.
- Instrumented tests use deterministic fakes when testing UI wiring.
- Generated files and debug artifacts are absent from the diff.
- `git diff --check` is clean.
