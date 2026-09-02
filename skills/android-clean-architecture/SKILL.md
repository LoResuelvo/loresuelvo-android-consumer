# android-clean-architecture

Load this skill when changing `domain/`, `data/`, `ui/`, or
`domain/usecase/`, or when reviewing a dependency boundary.

## Do not load

- Documentation, Gradle, CI, or script-only changes.
- UI-only changes that do not cross a layer boundary.

## Dependency direction

```text
ui → domain/usecase → domain
data ───────────────→ domain
```

- `domain/` contains pure entities, ports, outcomes, and errors.
- `domain/usecase/` orchestrates domain ports and has no infrastructure imports.
- `data/` implements ports and owns HTTP, Auth0, Android storage, DTOs, and mappers.
- `ui/` observes state and emits events; it does not construct repositories or call `data/` directly.

## Hard rules

- Domain code must not import `data`, `ui`, `android.*`, Dagger/Hilt, OkHttp, Retrofit, or serialization.
- DTOs live only in `data/api/dto/`; backend snake_case must not enter domain or UI.
- Mappers live in `data/api/mapper/` and contain no business rules.
- Each use case is a class with one `operator fun invoke(...)`.
- Use case names follow `VerbSubjectUseCase`.
- Outcomes and failures are typed `sealed interface`s, never generic error strings.
- Add new mutable global `object`s only with an explicit architectural exception; normally use Hilt injection.

Examples: `domain/auth/AuthProvider.kt:3-6`,
`domain/api/ApiError.kt`, `data/api/mapper/UserDtoMapper.kt`,
`domain/usecase/auth/RegisterConsumerUseCase.kt`.

## Naming

- Ports: `UserRepository`, `AuthProvider`, `AuthSessionStore`.
- Adapters: `ApiUserRepository`, `Auth0AuthProvider`.
- Mappers: `<Entity>DtoMapper` or the existing mapper convention in `data/api/mapper/`.
- Outcomes: `<Action>Outcome` with typed `Success` and `Failure` variants.

## Validation

Run before review:

```bash
rg -n "import (com\.loresuelvo\.consumer\.(data|application|ui)|android\.|dagger|hilt|okhttp3|retrofit2|kotlinx\.serialization)" \
  app/src/main/java/com/loresuelvo/consumer/domain/
```

Expected result: no matches.

Then check UI does not import data directly:

```bash
rg -n "import com\.loresuelvo\.consumer\.data\." \
  app/src/main/java/com/loresuelvo/consumer/ui/
```

Expected result: no matches.

## Review checklist

- Is the dependency pointing inward toward the domain?
- Is the infrastructure detail hidden behind a domain port?
- Are DTO conversion and error translation at the data/use-case boundary?
- Is state immutable and exposed through `StateFlow` in UI?
- Are strings, logging, and Android context kept in their proper outer layer?

Source of truth: `AGENTS.md` sections “Architecture”, “Dependency rule”,
“Domain purity”, and “DTO rule”.
