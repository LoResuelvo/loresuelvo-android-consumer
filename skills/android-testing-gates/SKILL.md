# android-testing-gates

Validaciones que se corren antes de PR y antes de merge. Cargar cuando se está por cerrar un PR, hacer release, o merge a `main`.

## Cuándo NO cargar

- Iteración local (usar `android-bdd-tdd-process`).
- Cambios puramente de documentación.

## Comandos

```bash
# Mínimo antes de PR
make lint
make test

# Antes de merge a main
make ci
# = make build + make lint + make test-all-once
#   = make build + make lint + make test + make e2e

# Focalizado durante TDD
./gradlew :app:testDevDebugUnitTest --tests "*CompleteProfileViewModelTest*"
./gradlew :app:connectedDevDebugAndroidTest --tests "*CompleteProfileAcceptanceTest*"
```

`make help` lista todos los targets.

## Política fail-fast

1. Si `make lint` falla, **no** correr `make test`. Corregir lint primero.
2. Si `make test` falla, corregir y re-ejecutar solo `make test`. No re-correr `make build`.
3. Si `make build` falla, **no** mergear.
4. Si `make e2e` falla, **no** mergear. Los E2E son contrato de producto.

## Qué valida cada comando

| Comando | Qué cubre | Velocidad |
|---|---|---|
| `make lint` | Android Lint (composables, recursos, manifest, etc.) | ~30s |
| `make test` | Unit tests JVM (JUnit4 + MockK + Turbine + Robolectric) | <30s objetivo |
| `make e2e` | Acceptance con Compose-test + BDD Cucumber (requiere emulador o device) | 2-10 min |
| `make build` | `assemble<Flavor>Debug` (compilación + recursos + R8 si release) | ~1-3 min |

## Antes de PR (checklist)

- [ ] `make lint` verde.
- [ ] `make test` verde. Todos los tests nuevos pasan localmente antes de pushear.
- [ ] `make build` verde.
- [ ] Si agregaste un `.feature` o modificaste steps, `make e2e` verde.
- [ ] Sin `Log.*` directo en código nuevo (`grep -RIn "Log\.[dwe]" app/src/main/java/`).
- [ ] Sin literales en español en código nuevo (`grep -RIn '"[A-ZÁÉÍÓÚÑ][a-záéíóúñ ]\+[a-záéíóúñ]"' app/src/main/java/`).
- [ ] Sin `viewModelFactory { initializer { ... } }` nuevo en producción.
- [ ] Sin `object` global mutable nuevo.
- [ ] DTOs nuevos solo en `data/api/dto/`. Dominio sin snake_case.
- [ ] `AGENTS.md` y skills actualizados si cambió arquitectura, convención o comandos.

## Antes de merge a main (checklist adicional)

- [ ] `make ci` verde (build + lint + test + e2e).
- [ ] Al menos un revisor aprobó (Joseph si toca contrato API, par del equipo Android si es solo refactor).
- [ ] PR con descripción en inglés: archivos tocados, comandos de validación ejecutados, riesgos residuales.

## Errores comunes y qué hacer

### `Lint` reporta `HardcodedText` en composables

Eso es el linter detectando un literal en español en código. Mover a `strings.xml` (es + en) y reemplazar por `stringResource(R.string.<key>)`.

### `Lint` reporta `UnusedResources`

Recursos `strings.xml` definidos pero no usados. Borrarlos (los huérfanos se acumulan y agrandan el APK).

### `make test` falla con `Hilt ... must be set`

Falta `@HiltAndroidTest` + `HiltTestApplication`. Configurar `testInstrumentationRunner` en `app/build.gradle.kts` y un `HiltTestRunner` custom.

### `make e2e` falla con "device not found"

`adb devices` debe devolver al menos un device. Conectar vía ADB inalámbrico (ver `README.md` sección "ADB inalámbrico") o arrancar un emulador con `emulator -avd <name>`.

### `MockWebServer` falla con `port already in use`

`Hooks.kt` debe cerrar el `MockWebServer` en `@After` con `server.shutdown()`. Si no, el puerto queda tomado entre tests.

## Cobertura (cuando se agregue CI)

- Mínimo: jacoco + reporte HTML en `build/reports/jacoco/`.
- Gate: ≥ 90% líneas en `data/` + `domain/usecase/`, ≥ 80% en `ui/`.
- Hoy (sin jacoco configurado todavía) la métrica informal es: cada use case tiene tests por cada subclase de `Failure`, cada ViewModel tiene tests por cada estado de `UiState`.

## Anti-patrones

- ❌ "Pasa local, pusheo y arreglo en CI". Los tests deben pasar local antes de pushear.
- ❌ Mergear con `make e2e` rojo aunque los demás estén verdes.
- ❌ Saltar `make build` porque "ya sé que compila". `make build` valida recursos, R8 (si release), KSP/KAPT, plugins nuevos.
- ❌ Confundir "tests pasan" con "calidad". Revisar coverage y nombres de tests, no solo el verde.
