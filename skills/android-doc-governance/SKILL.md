# android-doc-governance

Cómo mantener `AGENTS.md`, `CLAUDE.md`, `README.md` y las skills. Cargar cuando se vaya a agregar/modificar reglas, comandos, o skills.

## Cuándo NO cargar

- Cambios de código de producto (no tocan docs).
- Cambios en changelogs internos (no existen todavía; si se crean, definir en esta skill).

## Principios

- **`AGENTS.md` es la fuente canónica para agentes** (humanos AI o no). Si una regla está en otro lado, moverla a `AGENTS.md`.
- **`CLAUDE.md` es un puntero.** No duplica contenido, solo resume y enlaza a `AGENTS.md` y a skills. Si encontrás algo en `CLAUDE.md` que no está en `AGENTS.md`, movelo.
- **`README.md` es para humanos que recién arrancan.** Solo setup, comandos y troubleshooting. No reglas de arquitectura.
- **Cada skill es atómica**: una responsabilidad. Si una skill cubre dos cosas no relacionadas, dividirla.
- **Cada regla en `AGENTS.md` tiene al menos un ejemplo concreto del proyecto** (`path:line`). Regla sin ejemplo es regla que se ignora.

## Estructura esperada

```
AGENTS.md                                   # canónico para agentes
CLAUDE.md                                   # puntero/resumen para Claude y otros
README.md                                   # setup + comandos + troubleshooting
skills/
  android-clean-architecture/SKILL.md
  android-bdd-tdd-process/SKILL.md
  android-testing-gates/SKILL.md
  android-api-client-governance/SKILL.md
  android-hilt-governance/SKILL.md
  android-doc-governance/SKILL.md
  android-commit-governance/SKILL.md
```

## Cuándo actualizar `AGENTS.md`

- Cambia la arquitectura (nueva capa, nuevo módulo, cambio de DI).
- Cambia una regla crítica (pureza del dominio, manejo de errores, i18n, logging).
- Cambia un comando de validación (`make <target>`, `./gradlew <task>`).
- Se agrega o quita una skill del índice.
- Se descubre un nuevo anti-patrón con evidencia (path:line del proyecto donde pasó).
- Se actualiza el stack (versión de Kotlin, AGP, Compose, Hilt, etc.).

## Cuándo NO actualizar `AGENTS.md`

- Cambios en código que ya cumplen las reglas existentes.
- Refactors internos que no alteran convenciones.
- Cambios en `README.md` o en skills (esos usan su propio ciclo).

## Cuándo crear una skill nueva

- Aparece un patrón recurrente que amerita una guía reusable (ej: integración con Stripe, FCM, WorkManager, etc.).
- Hay una decisión técnica con varias opciones y queremos registrar el criterio (ej: "cómo elegir entre Retrofit y Ktor").
- Hay un anti-patrón que se repite en code reviews y queremos darle a los agentes una referencia única.

## Cuándo NO crear una skill

- El tema cabe en una skill existente.
- Es un caso único (no reusable).
- Es un comando o flag de un solo uso.

## Mantenimiento de skills

- **Primera línea**: cuándo cargar la skill.
- **Segunda línea**: cuándo NO cargar (típicamente: "Cambios que no tocan X").
- **Cuerpo**: principios, reglas concretas, ejemplos del proyecto, anti-patrones, referencia rápida.
- **Límite**: ≤ 200 líneas. Si pasa, dividir.
- **Convención de idioma**: español en secciones descriptivas, inglés en ejemplos de código, nombres de clases, comandos, flags.
- **Ejemplos del proyecto**: cada regla abstracta debe tener un ejemplo `path:line` concreto. Si no hay ejemplo, no es una regla útil.

## Cambios destructivos

- Borrar una skill: requiere PR con justificación + al menos un aprobador. Actualizar el índice en `AGENTS.md` y el `Mapa rápido de decisión`.
- Cambiar la estructura de capas: requiere Fase del plan maestro + actualizar `AGENTS.md`, `android-clean-architecture`, y al menos 2 skills relacionadas.

## Revisión periódica

- En cada release (Fase 10 del plan maestro), revisar:
  - `AGENTS.md` refleja la realidad del código.
  - Todas las skills tienen ejemplos `path:line` que siguen vigentes.
  - El índice de skills no incluye skills obsoletas.
  - `README.md` sigue siendo solo setup + comandos.

## Anti-patrones

- ❌ Documentación duplicada entre `AGENTS.md` y `README.md`. Decidir: `AGENTS.md` para reglas, `README.md` para humanos nuevos.
- ❌ Skills de 500 líneas con múltiples responsabilidades. Dividir.
- ❌ Reglas en `AGENTS.md` sin ejemplo concreto. Borrar o agregar ejemplo.
- ❌ "TODO" en `AGENTS.md` o skills. Si está pendiente, va en un issue, no en la doc.
- ❌ Cambios en `AGENTS.md` sin actualizar el índice de skills si se agrega/quita una.
- ❌ `CLAUDE.md` con contenido que no está en `AGENTS.md`. Mover.

## Referencia rápida

- `AGENTS.md` → secciones "Modo skills-first" e "Índice de skills locales" para el formato canónico.
- Webapp `loresuelvo-webapp/AGENTS.md` para el patrón de skills de la webapp.
- Webapp `loresuelvo-webapp/skills/` para ver el formato de skills existentes.
