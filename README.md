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
   AUTH0_AUDIENCE=http://localhost:8080
   API_URL=http://10.0.2.2:8080
   ```
   `AUTH0_AUDIENCE` es el identificador lógico de la API registrado en Auth0; no tiene que ser una URL alcanzable. En un teléfono físico, `API_URL` sí debe apuntar a una dirección alcanzable de la PC, por ejemplo `http://192.168.1.41:8080`.

   Para `staging` y `prod`, usar `AUTH0_DOMAIN_STAGING`, `AUTH0_CLIENT_ID_STAGING`, `AUTH0_SCHEME_STAGING`, `AUTH0_AUDIENCE_STAGING`, `API_URL_STAGING`, etc.
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

### API local desde un teléfono físico

Con el teléfono conectado por ADB, la opción más simple evita configurar firewall o port forwarding en Windows:

```bash
adb reverse tcp:8080 tcp:8080
```

Luego usar en `local.properties`:

```properties
API_URL=http://127.0.0.1:8080
```

La regla `adb reverse` dura mientras siga activa la conexión ADB y debe repetirse después de reconectar o reiniciar el dispositivo. La API debe estar escuchando en el puerto `8080` de la máquina de desarrollo.

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

### `make up` o `make test` fallan con `ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH`

Falta el JDK en el entorno donde se corre el build. En WSL Ubuntu:

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >> ~/.bashrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc
source ~/.bashrc
java -version   # debe decir "openjdk version 17.x"
```

Si usás Windows nativo (PowerShell), instalá [Eclipse Temurin 17](https://adoptium.net/) o [`openjdk-17-jdk` desde Chocolatey](https://chocolatey.org/packages/openjdk17) y setea `JAVA_HOME` en las variables de entorno del sistema.

Verificá que el `JAVA_HOME` apunte a un directorio que contenga `bin/java` (`bin/java.exe` en Windows).

### `Build-tool 35.0.0 is missing AAPT` o `Installed Build Tools revision X is corrupted` corriendo desde WSL contra un SDK de Android instalado en Windows

El SDK de Android tiene binarios distintos por plataforma. WSL espera binarios Linux (sin `.exe`); el SDK de Windows tiene `.exe`. El error aparece porque Gradle busca `aapt` (Linux) pero solo existe `aapt.exe` (Windows). Tres opciones:

- **Más rápido (recomendado para setup actual)**: compilá desde **Git Bash / PowerShell en Windows**, no desde WSL. El SDK ya está en `C:\Users\ASUS\AppData\Local\Android\Sdk` y todo el toolchain funciona ahí.
- **Más limpio a largo plazo**: instalá un SDK Android Linux en WSL con `cmdline-tools` y `build-tools;35.0.0` (binarios Linux). ~5 GB de descarga.
- **Híbrido (no recomendado)**: usá Git Bash para `make up` / `make test` y WSL solo para la webapp. Cada shell necesita su `local.properties` con el formato de path correcto (Windows para Git Bash, `/mnt/c/...` para WSL).

### `:app:hiltAggregateDepsDevDebug FAILED` con `NoSuchMethodError: 'java.lang.String com.squareup.javapoet.ClassName.canonicalName()'`

Bug conocido de Hilt 2.51+ con KSP 2.0.21-1.0.28. El fix está aplicado en `build.gradle.kts` (raíz) cargando Hilt y KSP desde `buildscript { classpath(...) }` con un `force("com.squareup:javapoet:1.13.0")` en la classpath, y Hilt pinneado en `2.50`. Si reaparece tras un update, revisar:

- `gradle/libs.versions.toml`: `hilt = "2.50"` (no `2.51+` hasta que saquen fix).
- `build.gradle.kts` (raíz): `buildscript { configurations.all { resolutionStrategy { force("com.squareup:javapoet:1.13.0") } }; dependencies { classpath("com.google.dagger:hilt-android-gradle-plugin:2.50"); classpath("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.0.21-1.0.28") } }`.
- `app/build.gradle.kts`: `id("com.google.dagger.hilt.android")` y `id("com.google.devtools.ksp")` (no `alias`).

Ver `https://github.com/google/dagger/issues/3965` para el detalle.

### `kotlin-serialization` plugin no resuelve

Verificar que el plugin está declarado en el `plugins` block con la misma versión de Kotlin. Si choca con `kotlin-compose`, considerar usar `kotlinx-serialization` runtime con `Json {}` en vez del plugin (workaround documentado en `AGENTS.md`).

---

## GitHub Actions

> Pendiente (WIP). El pipeline correrá `make ci` (build + lint + unit + acceptance) en cada push y PR.

---

## Licencia

Ver [`LICENSE`](./LICENSE).
