# android-bdd-tdd-process

Ciclo BDD (Gherkin primero) y TDD (RED → GREEN → REFACTOR) aplicado a este proyecto. Cargar cuando se vaya a crear una nueva feature, refactorizar comportamiento, o agregar/modificar un flujo de usuario.

## Cuándo NO cargar

- Cambios puramente cosméticos (colores, strings sin cambio semántico).
- Cambios de tooling (Gradle, CI, scripts).
- Refactors que no alteran comportamiento (mover archivos, renombrar) sin tests existentes.

## Principio

> **Test primero, código después. Comportamiento primero, impl después.**

Cada cambio de comportamiento sigue este orden estricto:

1. **Spec BDD**: escribir o actualizar el `.feature` en `app/src/androidTest/assets/features/<dominio>/<caso>.feature` con el escenario nuevo. Idioma: español.
2. **Step definitions**: implementar los `@Given/@When/@Then` mínimos para que el scenario falle por la razón correcta (RED).
3. **Unit test del comportamiento**: escribir el test JVM (`src/test/`) que cubre la rama feliz + al menos 2 ramas de error. RED local.
4. **Implementación mínima**: solo lo necesario para pasar el test. GREEN.
5. **Refactor**: extraer, renombrar, mejorar nombres. GREEN se mantiene.
6. **Acceptance test** (cuando aplique): el `composable` se prueba con `compose.ui.test` (`createComposeRule()` o `createAndroidComposeRule<MainActivity>()`).
7. **Validación**: `make test` + `make e2e` + `make build` verde.

## Estructura de un `.feature`

```gherkin
# language: es
Característica: Registrar cuenta nueva de consumidor
  Como consumidor
  Quiero registrarme en LoResuelvo
  Para contratar servicios residenciales

  Escenario: 01-RCN Registro exitoso
    Dado que el usuario no está autenticado
    Y que Auth0 devuelve credenciales válidas
    Y que el backend acepta POST /consumers con 201
    Cuando completo el formulario con nombre "Andres" y apellido "Colina"
    Y presiono "Continuar"
    Entonces la UI navega al Home
    Y el backend recibió un POST /consumers con email y nombres correctos

  Escenario: 02-RCN Falla de red
    Dado que el backend no responde
    Cuando completo el formulario y presiono "Continuar"
    Entonces veo el mensaje de error de red
    Y NO navego al Home
```

Convención de IDs: `<NN>-<PREFIJO> <descripción>`. El prefijo identifica el feature (RCN = register consumer, CPC = complete profile consumer, etc.).

## Steps (reglas)

- Cada step es una función con **un solo** `@Given/@When/@Then`. Nada de helpers multi-acción.
- Steps agrupados por archivo por área: `WelcomeSteps.kt`, `CompleteProfileSteps.kt`, `BackendSteps.kt` (assertions sobre `MockWebServer.takeRequest()`), `SessionSteps.kt`, `NavigationSteps.kt`.
- Idioma del step en español. Idioma del nombre de clase y métodos en inglés.
- Steps **no** comparten estado mutable. Toda la data va por el `ScenarioContext` (un objeto inyectado vía PicoContainer o equivalente de Cucumber) o por parámetros del step.
- `Hooks.kt` configura Hilt (`@HiltAndroidTest`), reinicia `MockWebServer`, reemplaza `RepositoryModule` con fakes.

## TDD loop para código de producto

```bash
# 1. Escribir el test (RED)
# 2. Correr el test solo y confirmar que falla por la razón correcta
./gradlew :app:testDevDebugUnitTest --tests "*CompleteProfileViewModelTest.register_consumer_successfully*"

# 3. Escribir la implementación mínima
# 4. Correr el test, confirmar que pasa (GREEN)
./gradlew :app:testDevDebugUnitTest --tests "*CompleteProfileViewModelTest*"

# 5. Refactor con test en verde
# 6. Correr toda la suite
./gradlew :app:testDevDebugUnitTest
```

## Tests por capa

| Capa | Test type | Carpeta | Herramientas |
|---|---|---|---|
| Dominio (puertos, use cases, entidades) | Unit JVM | `src/test/.../domain/` | JUnit4 + MockK (solo para verificar que **no** se llama a un puerto; los use cases reales usan fakes) |
| Adapters (ApiUserRepository, Auth0) | Unit JVM | `src/test/.../data/` | JUnit4 + MockK para clientes, Robolectric para Context |
| Integración HTTP | Unit JVM con Robolectric | `src/test/.../data/api/` | MockWebServer + OkHttp real |
| ViewModels | Unit JVM | `src/test/.../ui/auth/`, `ui/session/` | JUnit4 + MockK + Turbine + `runTest` + `Dispatchers.setMain` |
| Composables (UI) | Unit JVM con Robolectric o `androidTest/` | `src/test/.../ui/auth/` o `src/androidTest/.../acceptance/auth/` | `compose.ui.test.junit4` + `createComposeRule()` |
| BDD E2E | Instrumented | `src/androidTest/.../bdd/` + `app/src/androidTest/assets/features/` | Cucumber JVM + `@HiltAndroidTest` + MockWebServer opcional |
| Acceptance flujo | Instrumented | `src/androidTest/.../acceptance/` | `createAndroidComposeRule<MainActivity>()` o Espresso |

## Fakes vs Mocks

- **Fakes** (`FakeUserRepository`, `FakeAuthProvider`): clases con respuestas programables. Preferidos para tests de use cases y ViewModels.
- **Mocks** (MockK `mockk<UserRepository>()`): solo cuando verificás interacciones (`verify { ... }`) o cuando necesitás simular excepciones específicas.
- **No usar `mockkStatic`** ni `mockkObject(SessionStateHolder)` en tests JVM. Refactor: extraer a interfaz testeable.

## Naming de tests

- Tests unitarios: `<ClassName>Test.kt`.
- Tests de integración: `<ClassName>IntegrationTest.kt`.
- Tests de aceptación: `<ClassName>AcceptanceTest.kt`.
- Métodos: `should_<comportamiento>_when_<condición>` en inglés. Ej: `should_navigate_to_home_when_registration_succeeds`, `should_show_error_when_backend_returns_400`.
- Un test por comportamiento, no por método. Si un método tiene 3 ramas, son 3 tests mínimo.

## Cobertura mínima

- Use cases: 100% de ramas.
- Repositorios y ViewModels: ≥ 90% líneas, 100% ramas en métodos públicos.
- Composables: cubrir estados `loading`, `empty`, `error`, `success` cuando apliquen.
- BDD: cubrir el happy path + al menos 2 caminos de error por feature.

## Anti-patrones

- ❌ Tests que dependen del orden de ejecución. Cada test debe poder correr en cualquier orden.
- ❌ Tests que duermen (`Thread.sleep`). Usar `runTest`, `advanceUntilIdle`, o `Turbine` para esperar.
- ❌ Tests con `runBlocking` dentro de callbacks de UI. Usar `runTest` o mover la lógica al ViewModel testeable.
- ❌ Tests de "render" que no validan comportamiento (ej: `setContent { WelcomeScreen() }` y assertar que un texto está). Cubrir al menos el callback que dispara el VM.
- ❌ Cobertura como meta sin calidad. 100% con tests basura es peor que 80% con tests que encuentren bugs.

## Ejemplo concreto del proyecto

- `src/test/.../ExampleUnitTest.kt` (placeholder) → reemplazar con tests reales.
- `app/src/androidTest/.../acceptance/auth/CompleteProfileScreenAcceptanceTest.kt` — ejemplo de acceptance con `createAndroidComposeRule<MainActivity>()`.
- `app/src/androidTest/.../integration/auth/Auth0AuthProviderTest.kt:30-72` — ejemplo de integration test con fake launcher y `async/yield`. **Frágil**: en código nuevo, reemplazar con `runTest` + Turbine o un `CountDownLatch` no es la solución.

## Referencia rápida

- `AGENTS.md` → "Comandos de validación" para los comandos de Gradle.
- `AGENTS.md` → "Regla de errores en use cases" para outcomes tipados.
- Webapp `loresuelvo-webapp/AGENTS.md` sección "Modo BDD con Gherkin/Cucumber" para inspiración del estilo Gherkin.
