# android-hilt-governance

Cómo configurar y usar Hilt en el proyecto, y cómo testear con Hilt. Cargar cuando se vaya a crear/modificar un módulo, un `@HiltViewModel`, un scope, un binding, o un test con `@HiltAndroidTest`.

## Cuándo NO cargar

- Cambios que no tocan DI.
- Cambios en Auth0 (cargar `skills/android-api-client-governance` o revisar `data/auth/Auth0AuthProvider.kt`).

## Pieza clave: topología

```kotlin
// LoresuelvoApp.kt
@HiltAndroidApp
class LoresuelvoApp : Application()

// MainActivity.kt — debe quedar así:
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LoResuelvoNav() }
    }
}

// AndroidManifest.xml — <application android:name=".LoresuelvoApp" ...>
```

**Regla**: `MainActivity` ≤ 15 líneas. Si crece, falla el code review.

## Componentes y scopes

| Componente | Scope | Cuándo usarlo |
|---|---|---|
| `SingletonComponent` | `@Singleton` | Repos, `ApiClient`, `OkHttpClient`, `Retrofit`, `Json`, `SessionStateHolder`, `EncryptedAuthSessionStore` |
| `ActivityRetainedComponent` | `@ActivityRetainedScoped` | (rara vez) Estado que sobrevive a recreaciones de Activity |
| `ViewModelComponent` | `@ViewModelScoped` | Use cases que solo viven con un VM específico |
| `ActivityComponent` | `@ActivityScoped` | (rara vez) Estado atado al ciclo de vida de Activity |

Regla práctica: **`@Singleton` por defecto**. Bajar a `@ViewModelScoped` solo si el use case es pesado o mutable y se quiere destruir con el VM. Documentar en `AGENTS.md` la decisión.

## Módulos

### `@Module @InstallIn(SingletonComponent::class)` para red, storage, etc.

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides @Singleton
    fun provideOkHttpClient(/* ... */): OkHttpClient = /* ... */

    @Provides @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit = /* ... */

    @Provides @Singleton
    fun provideApiClient(retrofit: Retrofit): ApiClient = ApiClient(retrofit)
}
```

### `@Module @InstallIn(SingletonComponent::class)` abstract + `@Binds` para repos

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: ApiUserRepository): UserRepository

    @Binds
    @Singleton
    abstract fun bindAuthSessionStore(impl: EncryptedAuthSessionStore): AuthSessionStore
}
```

**Regla**: `@Binds` para interfaces (más eficiente que `@Provides`). `@Provides` para terceros sin constructor anotado (OkHttp, Retrofit, etc.).

### `@Module @InstallIn(ViewModelComponent::class)` para use cases scoped a un VM

```kotlin
@Module
@InstallIn(ViewModelComponent::class)
object UseCaseModule {
    @Provides
    @ViewModelScoped
    fun provideRegisterConsumerUseCase(
        userRepository: UserRepository,
        authSessionStore: AuthSessionStore,
    ): RegisterConsumerUseCase = RegisterConsumerUseCase(userRepository, authSessionStore)
}
```

Alternativa: si el use case es `@Inject constructor(...)`, no necesita módulo. Hilt lo provee automáticamente. Usar módulo solo cuando:
- El use case no puede tener `@Inject` (depende de algo que no se puede inyectar).
- Necesitás decidir el scope manualmente.

## ViewModels

```kotlin
@HiltViewModel
class CompleteProfileViewModel @Inject constructor(
    private val registerConsumerUseCase: RegisterConsumerUseCase,
    private val sessionStore: AuthSessionStore,
) : ViewModel() {
    // ...
}
```

En el composable:

```kotlin
@Composable
fun CompleteProfileRoute(
    onNavigateToHome: () -> Unit,
    viewModel: CompleteProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    CompleteProfileScreen(
        firstName = state.firstName,
        lastName = state.lastName,
        loading = state.loading,
        error = state.error,
        onEvent = { event -> when (event) { NavigateToHome -> onNavigateToHome() } },
        onFirstNameChange = viewModel::onFirstNameChange,
        onLastNameChange = viewModel::onLastNameChange,
        onContinueClick = viewModel::onContinueClick,
    )
}
```

**Regla**: `hiltViewModel()` en el composable, no en el `ViewModelFactory`. Nunca `viewModelFactory { initializer { ... } }` en producción.

## `SavedStateHandle` (cuando aplique)

Si el VM necesita leer argumentos de navegación, usar `SavedStateHandle`:

```kotlin
@HiltViewModel
class SearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val categoryId: String = savedStateHandle.get<String>("categoryId") ?: ""
    // ...
}
```

## Entry points (evitar)

`@EntryPoint` para acceder a dependencias fuera del grafo estándar (servicios, broadcast receivers). **Evitar**. Si lo necesitás, replantear: probablemente algo está mal modelado.

## Tests con Hilt

### Unit JVM (`src/test/`) — sin Hilt

Los unit tests de ViewModels y use cases **no** usan Hilt. Usan `MockK` para inyectar fakes:

```kotlin
class CompleteProfileViewModelTest {
    private val registerConsumerUseCase = mockk<RegisterConsumerUseCase>()
    private val sessionStore = mockk<AuthSessionStore>()

    @Test
    fun should_navigate_to_home_when_registration_succeeds() = runTest {
        coEvery { registerConsumerUseCase(any()) } returns UserRegistrationOutcome.Success(User(...))
        every { sessionStore.saveSession(any()) } just runs

        val viewModel = CompleteProfileViewModel(registerConsumerUseCase, sessionStore)
        viewModel.onContinueClick()
        advanceUntilIdle()

        // asserts con Turbine
    }
}
```

**Razón**: los unit tests deben ser rápidos (<5s total) y no requerir emulador ni Hilt setup.

### Integration con Hilt (`src/androidTest/`) — `@HiltAndroidTest`

```kotlin
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ApiClientHiltTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var apiClient: ApiClient

    @Before
    fun setUp() { hiltRule.inject() }

    @Test
    fun should_call_consumers_endpoint() = runTest {
        // ...
    }
}
```

### Reemplazar módulos en tests

Para reemplazar `NetworkModule` con uno que apunte a `MockWebServer`:

```kotlin
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [NetworkModule::class],
)
object TestNetworkModule {
    @Provides @Singleton
    fun provideMockWebServer(): MockWebServer = MockWebServer().apply { start() }

    @Provides @Singleton
    fun provideOkHttpClient(server: MockWebServer): OkHttpClient =
        OkHttpClient.Builder().baseUrl(server.url("/")).build()

    // ...
}
```

En el test:

```kotlin
@HiltAndroidTest
@UninstallModules(NetworkModule::class)
class ApiClientIntegrationTest { /* ... */ }
```

### `HiltTestApplication` (requerido para instrumented con Hilt)

`app/build.gradle.kts`:

```kotlin
android {
    defaultConfig {
        testInstrumentationRunner = "com.loresuelvo.consumer.HiltTestRunner"
    }
}

dependencies {
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.51.1")
    kspAndroidTest("com.google.dagger:hilt-android-compiler:2.51.1")
}
```

`HiltTestRunner.kt`:

```kotlin
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, name: String?, context: Context?): Application {
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}
```

## Anti-patrones

- ❌ `viewModelFactory { initializer { ... } }` en producción. Usar `hiltViewModel()`.
- ❌ `object` global mutable nuevo. Usar `@Singleton @Inject` (con `@Provides` si no se puede).
- ❌ Pasar `Context` a ViewModels o use cases. Si hace falta, `@ApplicationContext`.
- ❌ `@Inject` en `data class` que no es un contrato. Inyectar solo lo que necesita ser reemplazable.
- ❌ `Provides` con `new` cuando se puede usar `@Binds`. `@Binds` es más eficiente.
- ❌ `@ActivityScoped` por defecto. El ciclo de vida de Activity es frágil. `@Singleton` o `@ViewModelScoped`.
- ❌ Módulos con scope equivocado. `NetworkModule` en `ViewModelComponent` recrea OkHttp con cada VM. **Siempre** en `SingletonComponent`.

## Ejemplo concreto del proyecto (estado actual + migración)

**Actual**:
- `MainActivity.kt:42-49`: `viewModelFactory { initializer { ... } }`. **Reemplazar** con `hiltViewModel()` en cada `composable("route") { ... }` del NavHost (Fase 8 del plan maestro).
- `data/auth/SessionStateHolder.kt:8-15`: `object SessionStateHolder { ... }` global mutable. **Migrar** a `@Singleton class @Inject constructor()` (Fase 8).

**Esperado (Fase 1+)**:
- `LoresuelvoApp.kt` con `@HiltAndroidApp`.
- `MainActivity.kt` minimal.
- `LoResuelvoNav.kt` con `NavHost` + `composable("route") { val vm: XxxViewModel = hiltViewModel() }`.

## Referencia rápida

- `AGENTS.md` → "DI (Hilt)".
- `skills/android-clean-architecture` para reglas de capas.
- `skills/android-testing-gates` para la política de `make test` + `make e2e`.
