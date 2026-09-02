# android-hilt-governance

Load this skill when adding or changing Hilt modules, bindings, scopes,
`@HiltViewModel`s, or `@HiltAndroidTest`s.

## Do not load

- UI-only changes with no dependency wiring.
- Auth0 or HTTP changes unless they also change DI.

## Production graph

- `LoresuelvoApp` uses `@HiltAndroidApp`.
- `MainActivity` uses `@AndroidEntryPoint` and only hosts Compose navigation.
- ViewModels use `@HiltViewModel` and constructor injection.
- Routes obtain ViewModels with `hiltViewModel()`.
- Production code must not use `viewModelFactory { initializer { ... } }`.
- New mutable global `object`s are forbidden.

Examples: `LoresuelvoApp.kt:20`, `MainActivity.kt:42-49`,
`ui/session/SessionViewModel.kt:31-36`.

## Modules and scopes

Use `SingletonComponent` for process-wide infrastructure: Retrofit, OkHttp,
JSON, repositories, and `EncryptedAuthSessionStore`.

Use `ViewModelComponent` only for dependencies that must live exactly as long
as one ViewModel. Do not scope network clients to a ViewModel.

Prefer:

- `@Binds` for interfaces whose implementation has an `@Inject` constructor.
- `@Provides` for third-party types or values that cannot be constructor-injected.
- `@Singleton` for shared infrastructure.

Examples: `di/RepositoryModule.kt:37-72`,
`data/auth/SessionStoreModule.kt:28-37`, `di/NetworkModule.kt:35-145`.

## ViewModels

```kotlin
@HiltViewModel
class ExampleViewModel @Inject constructor(
    private val useCase: ExampleUseCase,
) : ViewModel()
```

```kotlin
@Composable
fun ExampleRoute(
    viewModel: ExampleViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    ExampleScreen(state = state, onEvent = viewModel::onEvent)
}
```

Keep state and event handling in the ViewModel. Keep composables free of
manual dependency construction.

## Instrumented tests with Hilt

Every instrumented test that launches `MainActivity` must declare Hilt first:

```kotlin
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }
}
```

The custom runner must return `HiltTestApplication`. It is configured in
`app/build.gradle.kts` and implemented by
`app/src/androidTest/java/com/loresuelvo/consumer/HiltTestRunner.kt`.

Use `@TestInstallIn(..., replaces = [...])` for suite-wide fakes. Use
`@UninstallModules(...)` plus a nested test module for a class-local override.

Unit JVM tests in `src/test/` do not use Hilt; construct ViewModels with
fakes or mocks.

## Singleton session access in tests

When a test must mutate the session observed by production ViewModels, resolve
the binding from the application `SingletonComponent` with an `@EntryPoint`.
Do not inject `AuthSessionStore` directly into the test: interface bindings can
produce a different instance from the one observed by the Activity.

```kotlin
private val sessionStore: AuthSessionStore by lazy {
    EntryPointAccessors.fromApplication(
        ApplicationProvider.getApplicationContext<Application>(),
        AuthSessionStoreEntryPoint::class.java,
    ).authSessionStore()
}
```

See `instrumented/auth/CompleteProfileScreenInstrumentedTest.kt:61-70`.

## Anti-patterns

- Injecting `Context` into a ViewModel when `@ApplicationContext` or an adapter is sufficient.
- Creating repositories, Retrofit, or OkHttp clients manually in UI code.
- Using `@Provides` where `@Binds` is sufficient.
- Using `@ActivityScoped` for process-wide state.
- Omitting `HiltAndroidRule` or declaring it after the Compose rule.
- Replacing the singleton session store with a locally constructed store in an instrumented test.
