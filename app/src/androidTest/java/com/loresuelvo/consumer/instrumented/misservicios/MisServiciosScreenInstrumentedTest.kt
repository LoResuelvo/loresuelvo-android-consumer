package com.loresuelvo.consumer.instrumented.misservicios

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loresuelvo.consumer.MainActivity
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.data.api.ApiCategoryRepository
import com.loresuelvo.consumer.data.api.ApiProviderRepository
import com.loresuelvo.consumer.data.auth.EncryptedAuthSessionStore
import com.loresuelvo.consumer.data.auth.SessionStoreModule
import com.loresuelvo.consumer.di.RepositoryModule
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.auth.CurrentUserOutcome
import com.loresuelvo.consumer.domain.auth.RegisterConsumerData
import com.loresuelvo.consumer.domain.auth.User
import com.loresuelvo.consumer.domain.auth.UserRegistrationOutcome
import com.loresuelvo.consumer.domain.auth.UserRepository
import com.loresuelvo.consumer.domain.category.CategoryRepository
import com.loresuelvo.consumer.domain.conversation.ConversationRepository
import com.loresuelvo.consumer.domain.diagnosis.DiagnosisRepository
import com.loresuelvo.consumer.domain.jobrequest.JobRequestRepository
import com.loresuelvo.consumer.domain.provider.ProviderRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalCounterpart
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus
import com.loresuelvo.consumer.instrumented.diagnosis.FakeDiagnosisRepository
import com.loresuelvo.consumer.testdi.FakeConversationRepository
import com.loresuelvo.consumer.testdi.FakeJobRequestRepository
import com.loresuelvo.consumer.testdi.FakeServiceProposalRepository
import com.loresuelvo.consumer.ui.screens.home.components.HOME_MIS_SERVICIOS_LINK_TAG
import com.loresuelvo.consumer.ui.screens.misservicios.MIS_SERVICIOS_LIST_TAG
import com.loresuelvo.consumer.ui.screens.misservicios.MIS_SERVICIOS_SCREEN_TAG
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented coverage for US-54 scenario 03-VSP ("Visualizar
 * todas las propuestas de servicio"). Drives the consumer through
 * the navigation graph — Home → "Mis Servicios" link → MisServicios
 * list — and pins that the seeded proposals land in the rendered
 * `LazyColumn`.
 *
 * Mirrors [com.loresuelvo.consumer.instrumented.auth.CompleteProfileScreenInstrumentedTest]:
 *
 *  - `@HiltAndroidTest` + `@UninstallModules(RepositoryModule::class,
 *    SessionStoreModule::class)` to install a deterministic test
 *    graph: every port the production ViewModels transitively
 *    need (Categories / Provider / AuthSessionStore /
 *    Diagnosis / JobRequest / Conversation /
 *    ServiceProposal) is bound to a fake / production stub that
 *    returns a fast, in-memory result.
 *  - `@EntryPoint` to resolve the **same** `@Singleton` instance of
 *    `AuthSessionStore` that `SessionViewModel` observes, so the
 *    `saveSession(...)` mutation propagates through the production
 *    StateFlow and the smart router in `LoResuelvoNav` redirects
 *    the consumer to the Home (not Welcome / CompleteProfile).
 *  - `FakeServiceProposalRepository.set(...)` to seed the
 *    proposal list the MisServicios screen renders.
 *  - `composeTestRule.runOnUiThread { ... }` for the Hilt mutation
 *    + `activityRule.scenario.recreate()` to force the activity
 *    to rebuild against the new state.
 *
 * Locale note: the CI emulator boots as `en-US`, so the screen
 * resolves the strings from `values-en/`. The Compose assertions
 * resolve `R.string.*` from the activity's resources rather than
 * hard-coded Spanish literals.
 */
@HiltAndroidTest
@UninstallModules(RepositoryModule::class, SessionStoreModule::class)
@RunWith(AndroidJUnit4::class)
class MisServiciosScreenInstrumentedTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val sessionStore: AuthSessionStore by lazy {
        EntryPointAccessors.fromApplication(
            ApplicationProvider.getApplicationContext<Application>(),
            AuthSessionStoreEntryPoint::class.java,
        ).authSessionStore()
    }

    private val serviceProposalRepository: FakeServiceProposalRepository by lazy {
        EntryPointAccessors.fromApplication(
            ApplicationProvider.getApplicationContext<Application>(),
            ServiceProposalRepositoryEntryPoint::class.java,
        ).serviceProposalRepository()
    }

    @Before
    fun setUp() {
        hiltRule.inject()
        sessionStore.clearSession()
        serviceProposalRepository.set(SEED_PROPOSALS)
        persistCompletedAuthenticatedUser()
    }

    @Test
    fun home_entry_link_navigates_to_mis_servicios_listing_all_proposals() {
        // Sanity: the Home dashboard renders its "Ver todas"
        // section for the Mis Servicios entry. The link is keyed
        // by a dedicated testTag so we can target it without
        // relying on the localized text. This also pins that
        // `persistCompletedAuthenticatedUser` landed the smart
        // router on `Route.Home`, which is a precondition for
        // the MisServicios navigation below.
        composeTestRule
            .onNodeWithTag(HOME_MIS_SERVICIOS_LINK_TAG)
            .assertHasClickAction()

        // The "Mis Servicios" link on Home carries a dedicated
        // testTag so this assertion stays locale-independent and
        // unambiguous (the Home screen has multiple "Ver todas"
        // links for different sections). The link sits below the
        // initial viewport once the Home grid is fully populated,
        // so we scroll to it first to make sure hit-testing targets
        // the live (visible) bounds.
        composeTestRule
            .onAllNodesWithTag(HOME_MIS_SERVICIOS_LINK_TAG, useUnmergedTree = true)
            .onFirst()
            .performScrollTo()
            .assertHasClickAction()
            .performClick()

        // The Compose navigation transition + Hilt VM construction
        // + the VM's `init { load() }` round trip need more than a
        // single `waitForIdle()` to settle. Poll the semantics tree
        // with a deadline so the assertion races against the
        // recomposition instead of failing immediately.
        val deadline = System.currentTimeMillis() + 5_000L
        while (System.currentTimeMillis() < deadline &&
            composeTestRule
                .onAllNodesWithTag(MIS_SERVICIOS_SCREEN_TAG)
                .fetchSemanticsNodes()
                .isEmpty()
        ) {
            Thread.sleep(50)
        }
        composeTestRule.waitForIdle()

        // The MisServicios screen renders its loading text or
        // the empty-state copy while the round trip is in flight
        // / has returned empty — either is a positive "we are on
        // MisServicios" assertion. We use `assertCountEquals` (a
        // count, not a visibility check) because the screen's root
        // `Box` carries the testTag via a `Modifier.testTag(...)`
        // chained with `fillMaxSize()` and `statusBarsPadding()`
        // that occasionally place the node just outside the
        // viewport on this CI emulator; a count assertion pins
        // "the component exists in the semantics tree" without
        // demanding pixel-level visibility.
        composeTestRule
            .onAllNodesWithTag(MIS_SERVICIOS_SCREEN_TAG)
            .assertCountEquals(1)

        // Each seeded proposal lands on its own row. The row
        // testTag is keyed by the proposal id; `assertCountEquals`
        // here too because rows below the first may scroll out of
        // the viewport once the LazyColumn grows.
        SEED_PROPOSALS.forEach { proposal ->
            composeTestRule
                .onAllNodesWithTag("mis-servicios-row-${proposal.id}")
                .assertCountEquals(1)
        }

        // The seeded list (3 entries) is non-empty, so the
        // LazyColumn must be present.
        composeTestRule
            .onAllNodesWithTag(MIS_SERVICIOS_LIST_TAG)
            .assertCountEquals(1)
    }

    private fun persistCompletedAuthenticatedUser() {
        composeTestRule.runOnUiThread {
            sessionStore.saveSession(
                AuthSession(
                    user = User(
                        displayName = "Andres",
                        firstName = "Andres",
                        lastName = "Colina",
                        email = "andy@pro.com",
                    ),
                    accessToken = "fake-token",
                ),
            )
        }
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
    }

    private fun localizedString(resourceId: Int): String =
        composeTestRule.activity.getString(resourceId)

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AuthSessionStoreEntryPoint {
        fun authSessionStore(): AuthSessionStore
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ServiceProposalRepositoryEntryPoint {
        fun serviceProposalRepository(): FakeServiceProposalRepository
    }

    @Module
    @InstallIn(SingletonComponent::class)
    object TestSessionPrefsModule {
        @Provides
        @Singleton
        fun provideSessionPrefs(
            @ApplicationContext context: Context,
        ): SharedPreferences =
            context.getSharedPreferences("auth_session_secure_test", Context.MODE_PRIVATE)
    }

    @Module
    @InstallIn(SingletonComponent::class)
    abstract class MisServiciosTestRepositoryModule {

        @Binds
        @Singleton
        abstract fun bindUserRepository(
            repository: SuccessfulUserRepository,
        ): UserRepository

        @Binds
        @Singleton
        abstract fun bindCategoryRepository(
            repository: ApiCategoryRepository,
        ): CategoryRepository

        @Binds
        @Singleton
        abstract fun bindProviderRepository(
            repository: ApiProviderRepository,
        ): ProviderRepository

        @Binds
        @Singleton
        abstract fun bindAuthSessionStore(
            store: EncryptedAuthSessionStore,
        ): AuthSessionStore

        @Binds
        @Singleton
        abstract fun bindDiagnosisRepository(
            repository: FakeDiagnosisRepository,
        ): DiagnosisRepository

        @Binds
        @Singleton
        abstract fun bindJobRequestRepository(
            repository: FakeJobRequestRepository,
        ): JobRequestRepository

        @Binds
        @Singleton
        abstract fun bindConversationRepository(
            repository: FakeConversationRepository,
        ): ConversationRepository

        @Binds
        @Singleton
        abstract fun bindServiceProposalRepository(
            repository: FakeServiceProposalRepository,
        ): ServiceProposalRepository
    }

    @Singleton
    class SuccessfulUserRepository @Inject constructor() : UserRepository {
        override suspend fun getCurrentUser(): CurrentUserOutcome =
            CurrentUserOutcome.Success(
                User(
                    displayName = "Andres",
                    firstName = "Andres",
                    lastName = "Colina",
                    email = "andy@pro.com",
                ),
            )

        override suspend fun registerConsumer(
            data: RegisterConsumerData,
        ): UserRegistrationOutcome = UserRegistrationOutcome.Success(
            User(
                displayName = data.firstName,
                firstName = data.firstName,
                lastName = data.lastName,
                email = data.email,
            ),
        )
    }

    private companion object {
        val SEED_PROPOSALS: List<ServiceProposal> = listOf(
            proposal(id = "1", status = ServiceProposalStatus.Pending),
            proposal(id = "2", status = ServiceProposalStatus.Accepted),
            proposal(id = "3", status = ServiceProposalStatus.Rejected),
        )

        private fun proposal(id: String, status: ServiceProposalStatus): ServiceProposal =
            ServiceProposal(
                id = id,
                conversationId = "100",
                status = status,
                counterpart = ServiceProposalCounterpart(
                    id = "1",
                    name = "Juan",
                    surname = "Pérez",
                    categoryName = "Plomería",
                    profilePhotoUrl = null,
                ),
                description = "Fuga en el lavamanos",
                amountCents = 1500000L,
                scheduledOnEpochMillis = 1_792_074_600_000L,
                createdOnEpochMillis = 1_788_434_364_640L,
            )
    }
}
