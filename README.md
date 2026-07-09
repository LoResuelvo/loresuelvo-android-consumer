# LoResuelvo Android Consumer

Aplicación Android para **consumidores** de LoResuelvo. Construida con Kotlin + Jetpack Compose + Hilt.

> Para reglas de arquitectura, convenciones, comandos y skills, ver [`AGENTS.md`](./AGENTS.md). Este README es solo para humanos que arrancan.

---

## Requisitos

- **JDK 17** (`java -version`).
- **Android SDK Platform 35** + Build Tools 35 + Platform Tools + Command Line Tools.
- Variable `ANDROID_HOME` apuntando al SDK (ver abajo).
- **WSL/Linux/macOS** con `make` y `bash` (los scripts usan GNU make).

### Configurar el SDK

Agregar a `~/.bashrc` (o `~/.zshrc`):

```bash
export ANDROID_HOME=$HOME/Android/Sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
export PATH=$ANDROID_HOME/platform-tools:$PATH
export PATH=$ANDROID_HOME/emulator:$PATH
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$PATH
```

Aplicar y verificar:

```bash
source ~/.bashrc
sdkmanager --version
```

---

## Setup inicial

1. Clonar el repo.
2. Crear `local.properties` (NO commitear) en la raíz con tus credenciales de Auth0 por flavor:
   ```properties
   AUTH0_DOMAIN=loresuelvo-dev.auth0.com
   AUTH0_CLIENT_ID=tu_client_id_dev
   AUTH0_SCHEME=com.loresuelvo.consumer
   API_URL=http://10.0.2.2:8080
   ```
   Para `staging` y `prod`, usar `AUTH0_DOMAIN_STAGING`, `AUTH0_CLIENT_ID_STAGING`, `AUTH0_SCHEME_STAGING`, `API_URL_STAGING`, etc.
3. `./gradlew :app:assembleDevDebug` para verificar que compila.

---

## Comandos

Todos los targets aceptan `FLAVOR=Dev|Staging|Prod` (default: `Dev`).

| Comando | Qué hace |
|---|---|
| `make help` | Lista los targets disponibles. |
| `make build` | `./gradlew assemble<Flavor>Debug` |
| `make lint` | `./gradlew lint<Flavor>Debug` |
| `make test` | `./gradlew test<Flavor>DebugUnitTest` (JVM, rápido) |
| `make e2e` | Acceptance tests con Compose-test / Espresso (requiere emulador o device). |
| `make test-all-once` | `make test` + `make e2e`. |
| `make ci` | `make build` + `make lint` + `make test-all-once`. Usar antes de merge. |
| `make clean` | `./gradlew clean`. |
| `make devices` | `adb devices`. |

Para invocar `./gradlew` directamente con un test focalizado:

```bash
./gradlew :app:testDevDebugUnitTest --tests "*CompleteProfileViewModelTest*"
```

---

## ADB inalámbrico

Con la PC y el celular en la misma red Wi-Fi:

1. Activar **modo desarrollador**: `Ajustes → Acerca del teléfono → Número de compilación` (tocar 7 veces).
2. Activar **Depuración inalámbrica**: `Ajustes → Opciones de desarrollador → Depuración inalámbrica`.
3. **Vincular dispositivo**: en el celular, ir a `Vincular dispositivo con código de vinculación`. Anotar IP, puerto y código.
4. En la PC:
   ```bash
   adb pair <IP>:<PUERTO>
   # Pegar el código cuando lo pida
   adb connect <IP>:<PUERTO>   # puerto que aparece en la pantalla principal de Depuración inalámbrica
   adb devices
   ```
5. Instalar: `./gradlew installDevDebug` o `make build && adb install -r app/build/outputs/apk/dev/debug/app-dev-debug.apk`.

Desconectar: `adb disconnect` (o `adb disconnect <IP>:<PUERTO>`).

---

## Troubleshooting

### `SDK location not found`

Crear `local.properties` en la raíz con `sdk.dir=/path/to/Android/Sdk` (o setear `ANDROID_HOME`).

### `Build was configured to prefer settings repositories over project repositories`

Asegurarse de que `settings.gradle.kts` use `pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }` y que las deps estén en `dependencyResolutionManagement`.

### Tests E2E fallan con "Auth0 launches external activity"

Por diseño: el test `redirects_to_auth0_signup` está marcado con `@Ignore` porque depende de una actividad externa. Para testear el flujo real se usan mocks (`FakeAuthProvider`, `FakeUserRepository`) inyectados vía Hilt en tests con `@HiltAndroidTest`.

### Hilt: "Hilt AuthSessionStore must be set"

Suele indicar que falta `@HiltAndroidApp` en `LoresuelvoApp` o que no registraste `MainActivity` con `@AndroidEntryPoint`. Ver `MainActivity.kt` y `LoresuelvoApp.kt`.

### Encoding raro en strings (`Sesi��n`)

Archivo guardado en CP1252 en vez de UTF-8. Forzar `org.gradle.jvmargs=-Dfile.encoding=UTF-8` en `gradle.properties` y re-escribir el archivo con tu editor en UTF-8 sin BOM.

### `kotlin-serialization` plugin no resuelve

Verificar que el plugin está declarado en el `plugins` block con la misma versión de Kotlin. Si choca con `kotlin-compose`, considerar usar `kotlinx-serialization` runtime con `Json {}` en vez del plugin (workaround documentado en `AGENTS.md`).

---

## GitHub Actions

> Pendiente (WIP). El pipeline correrá `make ci` (build + lint + unit + acceptance) en cada push y PR.

---

## Licencia

Ver [`LICENSE`](./LICENSE).
