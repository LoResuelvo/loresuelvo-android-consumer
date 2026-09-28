package com.loresuelvo.consumer.instrumented.support

import android.app.Application
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.test.core.app.ApplicationProvider
import com.loresuelvo.consumer.MainActivity
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.auth.CurrentUserOutcome
import com.loresuelvo.consumer.domain.auth.RegisterConsumerAddress
import com.loresuelvo.consumer.domain.auth.User
import com.loresuelvo.consumer.domain.auth.UserRegistrationOutcome
import com.loresuelvo.consumer.domain.auth.UserRepository
import com.loresuelvo.consumer.domain.category.CategoriesOutcome
import com.loresuelvo.consumer.domain.category.CategoryRepository
import com.loresuelvo.consumer.domain.provider.ProviderRepository
import com.loresuelvo.consumer.domain.provider.ProvidersOutcome
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared utility helpers for the US-30-style **wire-pin instrumented
 * suite** — the regression tests that pin the route → VM callback
 * contract in production.
 *
 * **Why this is a plain Kotlin `object` and not a Hilt module.**
 *
 * KSP generates one `@HiltAndroidTest`-rooted component per
 * instrumented test class, but those tests share the production
 * `SingletonComponent` for transitive dependencies. Putting an
 * `@InstallIn(SingletonComponent::class)` module here would bind
 * `AuthSessionStore` / `UserRepository` / etc. across the whole
 * test component graph — colliding with the bindings every other
 * instrumented test (`WelcomeScreenInstrumentedTest`,
 * `MisServiciosScreenInstrumentedTest`, …) declares in its own
 * peer module. Dagger would refuse to compile with
 * `[Dagger/DuplicateBindings]` errors. By staying out of the
 * `@InstallIn(SingletonComponent::class)` namespace, this
 * helper only exposes plain Kotlin entry points + reference
 * `@Singleton` classes that each test wires via its own
 * `@Module @Binds` declarations.
 *
 * Concretely, the helper provides:
 *  1. [sessionStore] — `@EntryPoint`-resolved access to the
 *     same `@Singleton` [AuthSessionStore] the production
 *     `SessionViewModel` observes.
 *  2. [persistAuthSession] / [clearAuthSession] — drive the
 *     smart router via `saveSession(...)` / `clearSession()`
 *     through the entry-point-accessed store.
 *  3. [waitForTag] — bounded-deadline polling for a `testTag`
 *     to land in the semantics tree. Every wire-pin test
 *     races against `viewModelScope.launch → load(...) →
 *     uiState.update` recomposition; this helper avoids
 *     non-deterministic `Thread.sleep(...)` waits.
 *  4. [SuccessfulFakeUserRepository], [StubCategoryRepository],
 *     [StubProviderRepository] — re-usable `@Singleton` classes
 *     the test declares in its own `@Binds` module. They have
 *     no port-binding side effects here; they only carry the
 *     shape the test's `@Module` references.
 *  5. [AuthSessionStoreEntryPoint] — `@EntryPoint` interface the
 *     test's `EntryPointAccessors.fromApplication(...,
 *     AuthSessionStoreEntryPoint::class.java).authSessionStore()`
 *     dispatches through.
 *
 * **Pattern for a new wire-pin test** (see
 * [com.loresuelvo.consumer.instrumented.workorderdetail.WorkOrderDetailRouteInstrumentedTest]
 * for the canonical usage):
 *
 * ```kotlin
 * @HiltAndroidTest
 * @UninstallModules(RepositoryModule::class, SessionStoreModule::class)
 * class FooRouteInstrumentedTest {
 *     @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
 *     @get:Rule(order = 1) val composeTestRule =
 *         createAndroidComposeRule<MainActivity>()
 *
 *     private val sessionStore: AuthSessionStore by lazy {
 *         EntryPointAccessors.fromApplication(
 *             ApplicationProvider.getApplicationContext<Application>(),
 *             WirePinHarness.AuthSessionStoreEntryPoint::class.java,
 *         ).authSessionStore()
 *     }
 *     private val fooRepo: FakeFooRepository by lazy { ... }
 *
 *     @Before
 *     fun setUp() {
 *         WirePinHarness.harnessSetup(hiltRule, composeTestRule)
 *         fooRepo.set(seed)
 *         WirePinHarness.persistAuthSession(composeTestRule)
 *         composeTestRule.activityRule.scenario.recreate()
 *         composeTestRule.waitForIdle()
 *     }
 *
 *     @Test
 *     fun tapping_foo_reaches_the_repo_through_the_production_route() {
 *         composeTestRule.activity.setContent { FooRoute(...) }
 *         composeTestRule.waitForIdle()
 *         WirePinHarness.waitForTag(composeTestRule, FOO_TAG)
 *         composeTestRule.onNodeWithTag(FOO_TAG).performClick()
 *         // assertions on fooRepo.lastCall
 *     }
 * }
 *
 * @Module @InstallIn(SingletonComponent::class)
 * abstract class FooRouteTestBindings {
 *     @Binds @Singleton abstract fun bindFooRepo(
 *         fake: FakeFooRepository,
 *     ): FooRepository
 *     // ... route-specific bindings ...
 * }
 * ```
 */
object WirePinHarness {

    /**
     * Default consumer used by [persistAuthSession]. Override
     * via the optional [user] parameter when a test needs a
     * specific identity (e.g. a different email / display name).
     */
    fun defaultUser(): User = User(
        displayName = "Andres",
        firstName = "Andres",
        lastName = "Colina",
        email = "andy@pro.com",
        address = RegisterConsumerAddress(
            street = "Calle Falsa",
            streetNumber = "123",
            floor = "2",
            unit = "A",
        ),
    )

    /**
     * Injects Hilt into the test component graph. Call this once
     * from the test's `@Before fun setUp()` before any
     * `@EntryPoint`-resolution or `recreate()`.
     */
    fun harnessSetup(
        hiltRule: dagger.hilt.android.testing.HiltAndroidRule,
        @Suppress("UNUSED_PARAMETER") composeTestRule: androidx.compose.ui.test.junit4.ComposeContentTestRule,
    ) {
        hiltRule.inject()
    }

    /**
     * Hooks for tests that need to persist the consumer session
     * through the SAME `@Singleton` [AuthSessionStore] the
     * production `SessionViewModel] observes, so the smart
     * router flips into `Route.Home` after `scenario.recreate()`.
     */
    @Suppress("unused")
    fun persistAuthSession(
        composeTestRule: androidx.compose.ui.test.junit4.ComposeContentTestRule,
        user: User = defaultUser(),
    ) {
        composeTestRule.runOnUiThread {
            sessionStore(composeTestRule).saveSession(
                AuthSession(user = user, accessToken = "fake-token"),
            )
        }
    }

    /**
     * Counterpart of [persistAuthSession] for tests that need
     * the smart router back on `Route.Welcome`.
     */
    @Suppress("unused")
    fun clearAuthSession(
        composeTestRule: androidx.compose.ui.test.junit4.ComposeContentTestRule,
    ) {
        composeTestRule.runOnUiThread {
            sessionStore(composeTestRule).clearSession()
        }
    }

    /**
     * Resolves the `@Singleton` [AuthSessionStore] through the
     * Hilt test component graph. Tests typically wrap this in a
     * `by lazy { ... }` property so it resolves only on first
     * use — see the example in the class KDoc.
     */
    fun sessionStore(composeTestRule: androidx.compose.ui.test.junit4.ComposeContentTestRule): AuthSessionStore =
        EntryPointAccessors.fromApplication(
            ApplicationProvider.getApplicationContext<Application>(),
            AuthSessionStoreEntryPoint::class.java,
        ).authSessionStore()

    /**
     * Polls a [testTag] in the semantics tree on the calling
     * thread until it appears or [timeoutMillis] elapses. The
     * test thread sleeps ~50 ms between attempts so VMs that
     * need a recomposition round-trip have a window to settle
     * without ever hitting `Thread.sleep(...)` in the test body.
     *
     * Returns `true` if the tag was found within the deadline,
     * `false` otherwise — tests can use the boolean to drive a
     * more diagnostic `assertTrue("…", waitForTag(...))`.
     */
    fun waitForTag(
        composeTestRule: androidx.compose.ui.test.junit4.ComposeContentTestRule,
        testTag: String,
        timeoutMillis: Long = 5_000L,
    ): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            if (composeTestRule
                    .onAllNodesWithTag(testTag)
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            ) {
                return true
            }
            Thread.sleep(50)
        }
        return false
    }

    /**
     * Always-successful [UserRepository] stub used by every
     * test that lifts `MainActivity` through `LoResuelvoNav` —
     * the production `LoResuelvoNav` calls `getCurrentUser()`
     * to decide between `Route.Welcome` / `Route.Home` /
     * `Route.CompleteProfile`. Tests reference this class from
     * their own `@Module @Binds`; the stub is `@Singleton`
     * scoped so multiple `@Binds` declarations in the same
     * test refer to the same instance.
     */
    @Singleton
    class SuccessfulFakeUserRepository @Inject constructor() : UserRepository {
        override suspend fun registerConsumer(
            data: com.loresuelvo.consumer.domain.auth.RegisterConsumerData,
        ): UserRegistrationOutcome =
            UserRegistrationOutcome.Failure.Network(
                IllegalStateException("not exercised by wire-pin tests"),
            )

        override suspend fun getCurrentUser(): CurrentUserOutcome =
            CurrentUserOutcome.Success(
                User(
                    displayName = "Andres",
                    firstName = "Andres",
                    lastName = "Colina",
                    email = "andy@pro.com",
                    address = RegisterConsumerAddress(
                        street = "Calle Falsa",
                        streetNumber = "123",
                        floor = "2",
                        unit = "A",
                    ),
                ),
            )
    }

    /**
     * Minimal in-memory [CategoryRepository] stub. The route
     * under test never drills into categories; this exists only
     * to satisfy Hilt's transitive graph. Tests reference this
     * from their own `@Binds` in the same way they would a
     * production singleton.
     */
    @Singleton
    class StubCategoryRepository @Inject constructor() : CategoryRepository {
        override suspend fun getCategories(): CategoriesOutcome =
            CategoriesOutcome.Success(emptyList())
    }

    /**
     * Mirror of [StubCategoryRepository] for the provider port.
     */
    @Singleton
    class StubProviderRepository @Inject constructor() : ProviderRepository {
        override suspend fun getProvidersByCategory(categoryId: Int): ProvidersOutcome =
            ProvidersOutcome.Success(emptyList())
    }

    /**
     * The `EntryPoint` a wire-pin test uses to fish the
     * `@Singleton` `AuthSessionStore` out of the test
     * component graph. The test declares it on its own class
     * (passing it to `EntryPointAccessors.fromApplication(...)`)
     * because the entry-point interface lookup is keyed by the
     * declaring class' identifier.
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AuthSessionStoreEntryPoint {
        fun authSessionStore(): AuthSessionStore
    }
}
