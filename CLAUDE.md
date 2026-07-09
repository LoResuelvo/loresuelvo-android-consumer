# CLAUDE.md — LoResuelvo Android Consumer

Fuente canónica de contexto para Claude y otros agentes: **`AGENTS.md`** (este directorio).

## TL;DR

- App Android exclusiva para **consumidores** de LoResuelvo.
- Stack: Kotlin + Compose + Navigation + Hilt + StateFlow/UDF.
- Arquitectura: Clean Architecture liviana (domain puro, data adapters, ui Compose).
- Patrones: Observer, Adapter, Factory, DI.
- Regla clave: TDD/BDD primero, cero espaghetti, código y tests en inglés, UI en español.
- Regla clave: `MainActivity` minimal (≤15 líneas). Toda la composición vive en `LoResuelvoNav`.

## Cómo trabajar acá

1. **Leer primero**: `AGENTS.md`.
2. **Cargar skills** desde `skills/<skill>/SKILL.md` solo cuando aplique a la tarea.
3. **Seguir el plan maestro por fases** documentado en el chat de la sesión raíz (Fase 0 → Fase 10). No saltar fases; cada fase tiene pre-requisitos.
4. **TDD/BDD siempre**: test antes del impl. `.feature` antes de los steps. RED → GREEN → REFACTOR.
5. **Cero `Log.*` directo, cero literales en español, cero `viewModelFactory` en producción, cero `object` global nuevo.** Todo eso se valida con grep en code review.
6. **Validar antes de PR**: `make lint && make test && make build`. Si cambia BDD, también `make e2e`.

## Convenciones rápidas

- **Código y tests**: inglés.
- **UI y steps de BDD**: español.
- **Commits**: Conventional Commits en inglés (`feat:`, `fix:`, `refactor:`, `test:`, `chore:`, `docs:`, `build:`, `ci:`).
- **PRs**: atómicos por fase. Título en inglés, descripción con bullet list de archivos tocados y comandos de validación.
- **DTOs**: snake_case, solo en `data/api/dto/`. Dominio siempre camelCase.
- **Outcomes de use cases**: `sealed interface XxxOutcome` con `Success`/`Failure` tipados.
- **Errores**: nunca tragados. Traducidos a `Failure` tipado o propagados.

## Si tenés dudas

- Arquitectura: `AGENTS.md` → sección "Arquitectura y capas" + skill `android-clean-architecture`.
- Tests: `AGENTS.md` → sección "Comandos de validación" + skill `android-bdd-tdd-process` y `android-testing-gates`.
- Hilt: skill `android-hilt-governance`.
- API/HTTP: skill `android-api-client-governance`.

## NO hacer

- ❌ No agregar dependencias sin discutir versiones en `AGENTS.md` y `libs.versions.toml`.
- ❌ No introducir `object` global mutable nuevo.
- ❌ No usar `viewModelFactory { initializer { ... } }` en producción.
- ❌ No hardcodear literales en español en `app/src/main/java/`.
- ❌ No loguear tokens ni payloads.
- ❌ No mergear a `main` sin `make ci` verde.
