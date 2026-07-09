# android-api-client-governance

Cómo agregar, modificar y testear integraciones HTTP en el proyecto. Cargar cuando se vaya a crear o tocar el `ApiClient`, agregar/modificar un endpoint, crear un DTO, un mapper, un interceptor o un `Authenticator`.

## Cuándo NO cargar

- Cambios en UI que no llaman a la API.
- Cambios en Auth0 (cargar `android-hilt-governance` + revisar `data/auth/Auth0AuthProvider.kt`).

## Regla de oro

> **El dominio nunca sabe que existe HTTP. Toda la complejidad de red vive en `data/api/`.**

El dominio define un **puerto** (interfaz). La infraestructura lo implementa con HTTP. La UI consume el dominio vía Hilt. Ver `domain/auth/AuthProvider.kt:3-6` (puerto) y `data/auth/Auth0AuthProvider.kt:16-37` (impl).

## Estructura esperada (cuando se implemente en Fase 1+)

```
data/api/
  ApiConfig.kt                    # BuildConfig.API_URL + timeouts + constantes
  ApiClient.kt                    # @Singleton wrapper sobre Retrofit
  AuthInterceptor.kt              # Inyecta Authorization: Bearer <token>
  RetryOn401Authenticator.kt      # OkHttp Authenticator para refresh
  dto/
    RegisterConsumerRequestDto.kt
    RegisterConsumerResponseDto.kt
    ApiErrorDto.kt
  mapper/
    UserDtoMapper.kt

domain/api/
  ApiError.kt                     # sealed class PURO, no imports de red
```

`domain/api/ApiError.kt` vive en `domain/` (no en `data/`) **solo** si es una jerarquía de tipos puros. Si necesita `HttpException` u `OkHttp`, va en `data/api/`.

## Reglas de DTOs

- DTOs **solo** en `data/api/dto/`. Anotados con `@Serializable` y `@SerialName` cuando difiere de camelCase.
- Campos snake_case en el JSON, camelCase en la clase Kotlin:
  ```kotlin
  @Serializable
  data class RegisterConsumerRequestDto(
      @SerialName("email") val email: String,
      @SerialName("name") val name: String,
      @SerialName("surname") val surname: String,
  )
  ```
- Si la API devuelve 10 campos, el DTO tiene los 10. El mapper descarta los que el dominio no usa.
- `null` explícito o no, según contrato. Si la API no garantiza el campo, marcarlo nullable.

## Reglas de mappers

- En `data/api/mapper/<Entity>Mapper.kt` con funciones de extensión `toDomain()` y `toDto()`.
- **No** inflar mappers con lógica de negocio. Solo traducción de tipos.
- Si la conversión requiere defaults o validaciones (ej: fecha a epoch), delegar al dominio, no al mapper.
- Tests del mapper: cubren el round-trip (DTO → domain → DTO) y al menos 3 casos de campos opcionales/null.

## Reglas de errores (`ApiError`)

`ApiError` es una `sealed class` en `domain/api/`. Subclases mínimas:

```kotlin
sealed class ApiError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    data class Network(val cause: Throwable) : ApiError("Network error", cause)
    data class Server(val code: Int, val message: String) : ApiError(message)
    data class Unauthorized(val message: String = "Unauthorized") : ApiError(message)
    data class Unknown(val cause: Throwable? = null) : ApiError("Unknown error", cause)
}
```

`ApiClient` mapea las respuestas a `ApiError`:

| HTTP | Cuerpo | Mapea a |
|---|---|---|
| 2xx | DTO válido | DTO parseado |
| 400/422 | `ApiErrorDto` con `code` y `message` | `ApiError.Server(code, message)` |
| 401 | cualquiera | `ApiError.Unauthorized` |
| 5xx | cualquiera | `ApiError.Server(code, messageGenérico)` |
| Sin red / timeout | excepción | `ApiError.Network(cause)` |
| Otro | excepción | `ApiError.Unknown(cause)` |

Los **use cases** traducen `ApiError` a subclases de su `XxxOutcome.Failure` (más específicas, ej: `UserRegistrationOutcome.Failure.Server(code, message)`).

## AuthInterceptor

- Lee el token de `AuthSessionStore` (no de `SessionStateHolder` directamente, para mantener la abstracción).
- Si no hay sesión, no agrega `Authorization`.
- **No** refresca el token. El refresh vive en `Authenticator` (ver abajo).
- Si la ruta es de Auth0 (`/authorize`, `/oauth/token`, etc.), permitir lista explícita sin header.

## RetryOn401Authenticator

- Solo se invoca cuando el server responde 401.
- Llama a `AuthProvider.refreshToken()` (o equivalente). Si OK, devuelve el `Request` reintentado con el nuevo token. Si no, devuelve `null` (no retry).
- **No** debe loopear: si el retry también da 401, propaga el 401 al caller.
- Política de refresh debe estar documentada con el equipo de API. Si no hay refresh, dejar el `Authenticator` retornando `null` y abrir issue.

## Timeouts

Centralizados en `ApiConfig.kt`:

```kotlin
object ApiConfig {
    const val CONNECT_TIMEOUT_SECONDS = 10L
    const val READ_TIMEOUT_SECONDS = 30L
    const val WRITE_TIMEOUT_SECONDS = 30L
    const val CALL_TIMEOUT_SECONDS = 60L
}
```

Configurar el `OkHttpClient` con esos valores. No hardcodear en `NetworkModule`.

## Módulo Hilt (`di/NetworkModule.kt`)

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        retryAuthenticator: RetryOn401Authenticator,
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(ApiConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(ApiConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(ApiConfig.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .authenticator(retryAuthenticator)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(ApiConfig.BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideApiClient(retrofit: Retrofit): ApiClient = ApiClient(retrofit)
}
```

## Tests

### Unit JVM (`src/test/.../data/api/`)

- `AuthInterceptorTest.kt`: con `MockK` para `AuthSessionStore` y `OkHttp` real + `MockResponse` para verificar el header.
- `RetryOn401AuthenticatorTest.kt`: cubre los 3 escenarios (refresh OK, refresh fail, segundo 401).
- `UserDtoMapperTest.kt`: round-trip + nulls.
- `ApiErrorMappingTest.kt`: tabla de verdad de `ApiClient` mapeando responses.

### Integration con `MockWebServer`

- `ApiClientIntegrationTest.kt`: con `@HiltAndroidTest` + `@UninstallModules(NetworkModule::class)` + `@TestInstallIn(..., replaces = [NetworkModule::class])` que provee un `OkHttpClient` apuntando a `MockWebServer.url("/")`.
- Cubre: 2xx, 4xx con body, 5xx, red caída.
- En cada test: `server.takeRequest()` para inspeccionar el `Authorization` header y el payload enviado.

### Verificación de que el dominio sigue puro

```bash
grep -RInE "import (okhttp3|retrofit2|kotlinx\.serialization|com\.loresuelvo\.consumer\.data)" \
  app/src/main/java/com/loresuelvo/consumer/domain/
# Debe devolver 0 líneas.
```

## Anti-patrones

- ❌ DTOs con campos `@SerializedName` (usar `@SerialName` de `kotlinx-serialization`).
- ❌ Mappers con `!!` para forzar null-safety. Tipar correctamente el dominio.
- ❌ `try { api.call() } catch (e: Exception) { ... }` en `ApiClient` o repositorios. Mapear a `ApiError` y propagar.
- ❌ `Log.d` con el body del request o response. Ni siquiera en debug.
- ❌ Tokens en logs, en `SharedPreferences` plano, en `Intent` extras, ni en URLs.
- ❌ Reintentar 4xx automáticamente. Solo 5xx y `IOException`.
- ❌ `baseUrl` cambiado por feature flag en runtime. Solo `BuildConfig.API_URL` (por flavor).

## Ejemplo concreto del proyecto (futuro, Fase 4)

- **Puerto**: `domain/auth/UserRepository.kt` (puro).
- **Adapter**: `data/api/ApiUserRepository.kt` (con `@Inject` + `@Singleton`).
- **Binding**: `di/RepositoryModule.kt`:
  ```kotlin
  @Module
  @InstallIn(SingletonComponent::class)
  abstract class RepositoryModule {
      @Binds
      @Singleton
      abstract fun bindUserRepository(impl: ApiUserRepository): UserRepository
  }
  ```

## Referencia rápida

- `AGENTS.md` → "Regla de DTOs" y "Regla de pureza del dominio".
- `skills/android-clean-architecture` para las reglas de capas.
- Webapp `infrastructure/api/base-client.ts` para el patrón equivalente en el backend TypeScript.
- `infrastructure/repositories/api-user-repository.ts` (webapp) para el contrato `POST /consumers`.
