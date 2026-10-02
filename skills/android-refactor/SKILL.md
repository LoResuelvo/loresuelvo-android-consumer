---
name: android-refactor
description: Refactor Android code for smaller composable and class contracts, clearer module boundaries, and maintainable state/action APIs without changing behavior.
metadata:
  short-description: Refactor Android contracts and boundaries
---

# Android refactor

Use this skill when an Android change involves parameter-heavy functions,
large Compose screens, duplicated callbacks, unclear state ownership, or
layering problems.

## Refactor shape

- Inspect the current implementation, callers, tests, and Git status before
  editing. Preserve unrelated local changes.
- Keep the production boundary small: prefer immutable `State`/`UiState` plus
  an intent-specific `Actions` contract, and add a `Config` only for stable
  rendering configuration that is not state or an event.
- Group actions by responsibility (`navigation`, `media`, `review`,
  `proposals`, etc.). Do not create a generic bag with ambiguous fields.
- Keep rendering functions stateless. Routes and ViewModels own navigation,
  launchers, repositories, and side effects; Composables emit events.
- Do not retain a parameter-heavy overload in production for compatibility.
  If existing tests need migration time, a compatibility adapter may live only
  under `src/test` and must delegate to the new contract.

## Boundaries

- Domain code must not import UI, Android framework, Hilt, or data
  implementations.
- UI code must depend on domain contracts, not concrete data clients.
- Move shared models to the lowest layer that owns them and provide concrete
  implementations through dependency injection.
- Preserve immutable state and unidirectional data flow; do not hide mutable
  state inside a configuration object.

## Validation

Run the narrowest relevant tests while iterating, then run the repository
gates before handoff:

```bash
./gradlew :app:testDevDebugUnitTest --no-daemon
./gradlew :app:assembleDevDebug --no-daemon
./gradlew :app:lintDevDebug --no-daemon
```

Run connected instrumented tests when UI, navigation, Activity, or instrumented
test wiring changes. Report environment failures separately from regressions.
Keep `git diff --check` clean and do not modify another repository as part of
an Android consumer refactor.
