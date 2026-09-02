# android-bdd-tdd-process

Load this skill when adding behavior, changing a user journey, or adding a
BDD scenario and its tests.

## Terminology

- Acceptance scenarios are Gherkin files under `app/src/test/resources/features/`.
- Cucumber steps and worlds live under `app/src/test/java/.../bdd/` and run on the JVM.
- Instrumented UI tests live under `app/src/androidTest/` and run on a device or emulator.
- Do not call instrumented tests “acceptance tests”.

## Required loop

1. Write or update the Gherkin scenario.
2. Add the smallest step definitions needed to make it fail correctly.
3. Add or update JVM unit tests for the behavior and error branches.
4. Implement the smallest production change.
5. Refactor while tests remain green.
6. Add an instrumented UI test when the behavior crosses Activity, navigation, or real Android boundaries.

## Scenario rules

- Use Spanish for Gherkin steps; use English for code and test method names.
- Give scenarios stable IDs: `<NN>-<PREFIX> description`.
- Keep one action per step.
- Store scenario state in the world/context, not in shared mutable globals.
- Use fakes for use cases and ViewModels; use mocks only for interaction assertions.
- Do not use `Thread.sleep`; advance coroutine schedulers or wait on Compose idling.

Example: `features/auth/register-consumer.feature` with glue in
`bdd/onboarding/registerconsumer/`.

## Test boundaries

| Behavior | Test location | Main tools |
|---|---|---|
| Domain/use case | `src/test/.../domain/` | JUnit4, fakes, MockK when needed |
| Repository/HTTP | `src/test/.../data/api/` | MockWebServer, OkHttp |
| ViewModel | `src/test/.../ui/` | `runTest`, Turbine |
| Composable without Activity | `src/test/.../ui/` | Robolectric or Compose rule |
| Activity/navigation/device behavior | `src/androidTest/.../instrumented/` | Compose test, Espresso, Hilt |

Acceptance scenarios should assert typed outcomes and observable effects,
not localized UI strings. Assert localized strings in instrumented Compose
tests through the Activity resources.

## Commands

Focused JVM test:

```bash
./gradlew :app:testDevDebugUnitTest --tests "*CompleteProfileViewModelTest*"
```

Focused instrumented test:

```bash
./gradlew :app:connectedDevDebugAndroidTest \
  --tests "*CompleteProfileScreenInstrumentedTest*"
```

Full local validation:

```bash
make test
make instrumented
make build
```

## Anti-patterns

- Writing instrumented tests for behavior already covered by fast JVM tests.
- Coupling JVM tests to Hilt, Auth0, a real backend, or an emulator.
- Asserting Spanish text in JVM BDD tests.
- Sharing mutable state between scenarios.
- Adding a scenario without a deterministic fake or test backend.
