# android-commit-governance

Cómo escribir commits y PRs coherentes, atómicos y trazables. Cargar cuando se está por commitear cambios o abrir un PR.

## Cuándo NO cargar

- Hotfix de un solo carácter (commit trivial). Aun así, mensaje en Conventional Commits.

## Formato de commits: Conventional Commits

```
<type>(<scope>): <short summary>

<body — opcional>

<footer — opcional, ej: refs/closes issue>
```

### Tipos permitidos

| Tipo | Cuándo |
|---|---|
| `feat` | Nueva feature visible para el usuario o nuevo módulo público. |
| `fix` | Bug fix. |
| `refactor` | Cambio interno sin alterar comportamiento. |
| `test` | Solo tests. |
| `chore` | Tareas menores (renames, mover archivos, typos). |
| `docs` | Solo docs (`AGENTS.md`, `README.md`, skills, comentarios). |
| `build` | Cambios en `build.gradle.kts`, `libs.versions.toml`, Gradle wrapper. |
| `ci` | Cambios en `.github/workflows/`, scripts de CI. |
| `perf` | Mejora de performance. |
| `style` | Cambios de formato sin alterar lógica (whitepace, imports order). |

### Scope

Opcional pero recomendado. El scope es el módulo o área afectada. Ejemplos:
- `feat(registration): add ApiUserRepository and RegisterConsumerUseCase`
- `test(complete-profile): add ViewModel test for missing last name`
- `refactor(session): migrate SessionStateHolder to Hilt @Singleton`
- `chore(rename): fix typo SecondayButton → SecondaryButton`
- `build(gradle): add hilt, retrofit, mockk to libs.versions.toml`
- `docs(agents): add AGENTS.md and skills per Fase 0`

### Summary

- ≤ 72 caracteres.
- Imperativo, presente: "add", "fix", "migrate", "remove". NO "added", "fixes", "migrating".
- Sin punto final.
- Minúscula después del scope.

### Body

Opcional. Si lo usás:
- Explicar el **por qué**, no el **qué**.
- Wrapping a 72 columnas.
- Separar del summary con línea en blanco.

### Footer

- `Refs #123` para issues.
- `Closes #123` para cerrar.
- `BREAKING CHANGE: <descripción>` si hay cambio incompatible (ej: borrar un puerto, renombrar un módulo).

## Atomicidad

- **Un commit = un cambio lógico.** Si estás modificando 3 cosas no relacionadas, 3 commits.
- **Un PR = una fase del plan maestro** (o un subconjunto coherente). No PRs de "limpié todo lo que vi".
- Si un commit toca `domain/`, `data/` y `ui/`, está bien **si** es para una sola feature (ej: el use case + su adapter + su VM). Está mal si es "refactor + feature + fix" en el mismo commit.

## Orden de commits en un PR

Recomendado, no obligatorio:

1. `build:` cambios de catálogo y plugins (sin código).
2. `docs:` `AGENTS.md`, skills, `README.md`.
3. `test:` test RED (falla por la razón correcta).
4. `feat:` o `refactor:` implementación mínima que vuelve verde.
5. `refactor:` mejoras de nombres, extracción, etc. (con tests en verde).
6. `docs:` si la feature amerita nota en `AGENTS.md`.
7. `chore:` limpieza final (typofix, archivos movidos, etc.).

## Mensajes de PR

Título: igual estilo que commit summary.

Descripción en inglés, con secciones:

```markdown
## What
<1-3 bullets: qué cambia y por qué>

## How
<1-3 bullets: cómo se implementa>

## Validation
- [ ] `make lint` verde
- [ ] `make test` verde
- [ ] `make build` verde
- [ ] `make e2e` verde (si cambió BDD o flujo crítico)

## Files touched
- app/src/main/java/.../RegisterConsumerUseCase.kt (new)
- app/src/main/java/.../CompleteProfileViewModel.kt (modified)
- app/src/androidTest/assets/features/onboarding/register_consumer.feature (new)
- app/src/test/.../RegisterConsumerUseCaseTest.kt (new)

## Risks
<1-2 bullets: qué puede romperse, qué no se cubrió, qué queda para otra fase>
```

## Ejemplos buenos

```
feat(registration): add RegisterConsumerUseCase

Closes #42. The use case orchestrates AuthSessionStore → UserRepository
and translates ApiError into UserRegistrationOutcome.Failure. Used by
CompleteProfileViewModel to drive the registration flow.

- Adds domain/usecase/auth/RegisterConsumerUseCase.kt
- Adds domain/usecase/auth/RegisterConsumerCommand.kt
- Adds 6 unit tests covering all Failure branches
```

```
refactor(session): migrate SessionStateHolder to Hilt @Singleton

SessionStateHolder was a global object with mutable state. This commit
migrates it to @Singleton @Inject so it can be tested in isolation and
replaced in tests via @TestInstallIn. MainActivity wiring is unchanged.

- data/auth/SessionStateHolder.kt: object → @Singleton class
- data/auth/EncryptedAuthSessionStore.kt: receives SessionStateHolder via @Inject
- ui/session/SessionViewModel.kt: @HiltViewModel + @Inject
- LoresuelvoApp.kt: confirmed @HiltAndroidApp
```

## Ejemplos malos

❌ `arreglé cosas`
❌ `WIP`
❌ `fix bug`
❌ `feat: Updated login screen and also fixed some other things and added a new feature and renamed some files`
❌ `feat(login): add login screen.` (con punto final, mayúscula después del scope)

## Anti-patrones

- ❌ Commit sin `type:`. Conventional Commits **siempre**.
- ❌ `git commit --amend` después de pushear. Si se equivocó, fix forward.
- ❌ Commits con archivos debug (`Log.d("test")`, `TODO` olvidados, `print()`). Limpiar antes de commitear.
- ❌ Commitear secretos, tokens, `local.properties`. Validar con `git status` antes de commitear.
- ❌ Mezclar refactor + feature en un commit. Separar.
- ❌ `git commit -m "..."` con un cuerpo de 500 líneas explicando el diff. El diff está en el código; el mensaje explica el **por qué**.

## Referencia rápida

- [Conventional Commits spec](https://www.conventionalcommits.org/) para detalle del formato.
- `AGENTS.md` → "Idioma y estilo" para convención de idioma.
- Webapp `loresuelvo-webapp/skills/frontend-commit-governance/SKILL.md` para el equivalente en el repo de la webapp.
