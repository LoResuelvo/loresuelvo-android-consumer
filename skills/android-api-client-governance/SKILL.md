# android-api-client-governance

Load this skill when adding or changing an HTTP endpoint, Retrofit API,
DTO, mapper, interceptor, authenticator, or network test.

## Boundary

The domain must not know HTTP, Retrofit, OkHttp, or serialization. Define a
domain port and implement it in `data/`. Keep Retrofit interfaces, DTOs, and
wire-format details inside `data/api/`.

Examples: `domain/auth/UserRepository.kt:1-8`,
`data/api/ApiUserRepository.kt:31-45`.

## DTOs and mappers

- DTOs live only in `data/api/dto/`.
- Use `@Serializable` and `@SerialName` for backend field names.
- Keep backend snake_case out of domain and UI types.
- Mappers live in `data/api/mapper/` and only translate data; they do not contain business rules.
- Preserve all fields required by the wire contract in DTOs, even if the domain uses fewer fields.
- Test nullable fields, defaults, and field-name mapping.

```kotlin
@Serializable
data class ExampleDto(
    @SerialName("profile_photo_url") val profilePhotoUrl: String?,
)
```

Examples: `data/api/dto/`, `data/api/mapper/UserDtoMapper.kt`,
`data/api/mapper/CategoryDtoMapper.kt`.

## Errors

Keep the shared `ApiError` hierarchy pure in `domain/api/`. Repositories map
transport and response failures to it; use cases translate it into their own
typed outcomes.

At minimum distinguish network/timeouts, unauthorized responses, server
responses with status and safe message, and unknown failures.

Never return generic error strings from a use case and never leak transport
exceptions into UI state.

Examples: `domain/api/ApiError.kt`,
`data/api/ApiErrorMapping.kt`, `domain/usecase/auth/RegisterConsumerUseCase.kt`.

## Authentication and retry

- `AuthInterceptor` reads the token through `AuthSessionStore`.
- If no non-blank token exists, forward the request unchanged.
- Never log tokens, payloads, or response bodies.
- Keep refresh/retry policy in `RetryOn401Authenticator`.
- Do not retry ordinary 4xx responses.
- A second 401 must terminate the retry path.

Examples: `data/api/AuthInterceptor.kt`,
`data/api/RetryOn401Authenticator.kt`.

## Network configuration

- Use `BuildConfig.API_URL` for the Retrofit base URL.
- Keep timeouts centralized in `data/api/ApiConfig.kt`.
- Provide REST and upload clients as Hilt singletons.
- The upload client must not attach the API bearer token to pre-signed storage URLs.

Example: `di/NetworkModule.kt:35-145`.

## Tests

Use JVM tests for repositories, mappers, interceptors, and HTTP behavior.
Use `MockWebServer` for request/response contracts. Assert method, path,
headers, body, status mapping, and failure behavior.

Examples:

- `data/api/AuthInterceptorTest.kt`
- `data/api/RetryOn401AuthenticatorTest.kt`
- `data/api/ApiUserRepositoryIntegrationTest.kt`
- `data/api/mapper/UserDtoMapperTest.kt`

Validate domain purity with:

```bash
rg -n "import (okhttp3|retrofit2|kotlinx\.serialization|com\.loresuelvo\.consumer\.data)" \
  app/src/main/java/com/loresuelvo/consumer/domain/
```

Expected result: no matches.

## Anti-patterns

- DTOs in `domain/` or `ui/`.
- `@SerializedName` instead of `@SerialName`.
- Business rules inside mappers.
- Generic `catch (Exception)` that hides the failure type.
- Runtime flags that replace the configured base URL.
- Logging request/response bodies or credentials.
