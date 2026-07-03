# LoResuelvo Android Consumer

Aplicación Android para consumidores de LoResuelvo.

## Requisitos

### Java

Verificar instalación:

```bash
java -version
```

Se requiere:

```plaintext
OpenJDK 17
```

---

### Android SDK

Se requiere:

```plaintext
Android SDK Platform 35
Android Build Tools 35
Android Platform Tools
Android Command Line Tools
```

---

## Configuración del Android SDK

Agregar a `~/.bashrc`:

```bash
export ANDROID_HOME=$HOME/Android/Sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME

export PATH=$ANDROID_HOME/platform-tools:$PATH
export PATH=$ANDROID_HOME/emulator:$PATH
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$PATH
```

Aplicar cambios:

```bash
source ~/.bashrc
```

Verificar:

```bash
sdkmanager --version
```

---

## Compilación

Construir la aplicación:

```bash
make build
```

o

```bash
make up
```

---

## Tests

### Unit Tests

```bash
make test
```

### Acceptance Tests

```bash
make e2e
```

### Ejecutar todo

```bash
make test-all-once
```

### Pipeline local

```bash
make ci
```

---

# Conectar dispositivo Android (ADB inalámbrico)

Para poder conectar los dispositivos de forma inalámbrica, tanto la computadora como el celular deben estar conectados a la misma red wifi.

## Activar modo desarrollador

En el dispositivo:

```plaintext
Ajustes
→ Acerca del teléfono
→ Información de software
→ Número de compilación
```

Presionar 7 veces.

---

## Activar depuración inalámbrica

```plaintext
Ajustes
→ Opciones de desarrollador
→ Depuración inalámbrica
```

Activar.

---

## Vincular dispositivo

Ingresar en:

```plaintext
Opciones de desarrollador
→ Depuración inalámbrica
→ Vincular dispositivo con código de vinculación
```

El dispositivo mostrará:

```plaintext
Dirección IP y puerto
Código de vinculación
```

---

## Vincular desde la computadora

Ejecutar:

```bash
adb pair <IP>:<PUERTO>
```

Ejemplo:

```bash
adb pair 192.168.1.100:37123
```

Ingresar el código de vinculación mostrado en el dispositivo.

Salida esperada:

```plaintext
Successfully paired to device
```

---

## Conectar al dispositivo

Volver a la pantalla de depuración inalámbrica.

Copiar la dirección mostrada en:

```plaintext
Dirección IP y puerto
```

Ejecutar:

```bash
adb connect <IP>:<PUERTO>
```

Ejemplo:

```bash
adb connect 192.168.1.100:41751
```

Salida esperada:

```plaintext
connected to 192.168.1.100:41751
```

---

## Verificar conexión

```bash
adb devices
```

Salida esperada:

```plaintext
List of devices attached
192.168.1.100:41751 device
```

---

## Instalar aplicación

```bash
./gradlew installDebug
```

---

## Desconectar dispositivo

```bash
adb disconnect
```

o

```bash
adb disconnect <IP>:<PUERTO>
```

# GitHub Actions -> WIP

Los tests se ejecutan automáticamente mediante GitHub Actions en cada:

```plaintext
Push
Pull Request
```

El pipeline ejecuta:

1. Build
2. Lint
3. Unit Tests
4. Acceptance Tests

Los Acceptance Tests se ejecutan sobre un emulador Android configurado automáticamente por GitHub Actions.

---

# Comandos disponibles

```bash
make help
```

Comandos:

```bash
make build
make up
make lint
make test
make e2e
make test-all-once
make ci
make clean
```