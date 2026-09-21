package com.loresuelvo.consumer.instrumented.turnos

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loresuelvo.consumer.MainActivity
import com.loresuelvo.consumer.data.api.ApiCategoryRepository
import com.loresuelvo.consumer.data.api.ApiProviderRepository
import com.loresuelvo.consumer.data.api.ApiWorkOrderRepository
import com.loresuelvo.consumer.data.auth.EncryptedAuthSessionStore
import com.loresuelvo.consumer.data.auth.SessionStoreModule
import com.loresuelvo.consumer.di.RepositoryModule
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.auth.CurrentUserOutcome
import com.loresuelvo.consumer.domain.auth.RegisterConsumerAddress
import com.loresuelvo.consumer.domain.auth.User
import com.loresuelvo.consumer.domain.auth.RegisterConsumerData
import com.loresuelvo.consumer.domain.auth.UserRegistrationOutcome
import com.loresuelvo.consumer.domain.auth.UserRepository
import com.loresuelvo.consumer.domain.category.CategoryRepository
import com.loresuelvo.consumer.domain.conversation.ConversationRepository
import com.loresuelvo.consumer.domain.diagnosis.DiagnosisRepository
import com.loresuelvo.consumer.domain.jobrequest.JobRequestRepository
import com.loresuelvo.consumer.domain.provider.ProviderRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalRepository
import com.loresuelvo.consumer.domain.turno.Turno
import com.loresuelvo.consumer.domain.turno.TurnoCounterpart
import com.loresuelvo.consumer.domain.turno.TurnoStatus
import com.loresuelvo.consumer.domain.turno.TurnosRepository
import com.loresuelvo.consumer.domain.workorder.WorkOrderRepository
import com.loresuelvo.consumer.instrumented.diagnosis.FakeDiagnosisRepository
import com.loresuelvo.consumer.testdi.FakeConversationRepository
import com.loresuelvo.consumer.testdi.FakeJobRequestRepository
import com.loresuelvo.consumer.testdi.FakeServiceProposalRepository
import com.loresuelvo.consumer.testdi.FakeTurnosRepository
import com.loresuelvo.consumer.ui.components.turnocard.TURNO_CARD_TAG_PREFIX
import com.loresuelvo.consumer.ui.screens.home.HOME_TURNOS_LINK_TAG
import com.loresuelvo.consumer.ui.screens.turnos.TURNOS_SCREEN_TAG
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
 * Instrumented coverage for the US-55 visualize-turns feature
 * scenario 01-VT ("Acceder a Mis Turnos desde el Home") +
 * 02-VT ("Visualizar mis turnos registrados").
 *
 * Drives the consumer through the navigation graph — Home →
 * "Mis Turnos" link → Turnos list — and pins that the seeded
 * turnos land in the rendered `LazyColumn`.
 *
 * Mirrors
 * [com.loresuelvo.consumer.instrumented.misservicios.MisServiciosScreenInstrumentedTest]:
 *
 *  - `@HiltAndroidTest` + `@UninstallModules(RepositoryModule::class,
 *    SessionStoreModule::class)` to install a deterministic test
 *    graph: every port the production ViewModels transitively
 *    need (Categories / Provider / AuthSessionStore /
 *    Diagnosis / JobRequest / Conversation /
     *    ServiceProposal / WorkOrderDetail / Turnos) is bound to a fake /
 *    production stub that returns a fast, in-memory result.
 *  - `@EntryPoint` to resolve the **same** `@Singleton` instance of
 *    `AuthSessionStore` that `SessionViewModel` observes, so the
 *    `saveSession(...)` mutation propagates through the production
 *    StateFlow and the smart router in `LoResuelvoNav` redirects
 *    the consumer to the Home (not Welcome / CompleteProfile).
 *  - `FakeTurnosRepository.set(...)` to seed the turno list the
 *    Mis Turnos screen renders.
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
class MisTurnosScreenInstrumentedTest {

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

    private val turnosRepository: FakeTurnosRepository by lazy {
        EntryPointAccessors.fromApplication(
            ApplicationProvider.getApplicationContext<Application>(),
            TurnosRepositoryEntryPoint::class.java,
        ).turnosRepository()
    }

    @Before
    fun setUp() {
        hiltRule.inject()
        sessionStore.clearSession()
        turnosRepository.set(SEED_TURNOS)
        persistCompletedAuthenticatedUser()
    }

    @Test
    fun home_entry_link_navigates_to_mis_turnos_listing_all_seeded_turnos() {
        // Sanity: the Home dashboard renders its "Ver todas"
        // section for the Mis Turnos entry. The link is keyed
        // by a dedicated testTag so we can target it without
        // relying on the localized text. This also pins that
        // `persistCompletedAuthenticatedUser` landed the smart
        // router on `Route.Home`, which is a precondition for
        // the Mis Turnos navigation below.
        composeTestRule
            .onNodeWithTag(HOME_TURNOS_LINK_TAG)
            .assertHasClickAction()

        // The "Mis Turnos" link on Home carries a dedicated
        // testTag so this assertion stays locale-independent
        // and unambiguous (the Home screen has multiple
        // "Ver todas" links for different sections). The link
        // sits below the initial viewport once the Home grid is
        // fully populated, so we scroll to it first to make sure
        // hit-testing targets the live (visible) bounds.
        composeTestRule
            .onAllNodesWithTag(HOME_TURNOS_LINK_TAG, useUnmergedTree = true)
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
                .onAllNodesWithTag(TURNOS_SCREEN_TAG)
                .fetchSemanticsNodes()
                .isEmpty()
        ) {
            Thread.sleep(50)
        }
        composeTestRule.waitForIdle()

        // The Turnos screen root carries the testTag regardless
        // of which branch is rendered (Loading / Ready / Empty).
        composeTestRule
            .onAllNodesWithTag(TURNOS_SCREEN_TAG)
            .assertCountEquals(1)

        // Each seeded turno lands on its own `TurnoCard`. The
        // card testTag is keyed by the turno id; `assertCountEquals`
        // here too because cards below the first may scroll out
        // of the viewport once the LazyColumn grows.
        SEED_TURNOS.forEach { turno ->
            composeTestRule
                .onAllNodesWithTag(TURNO_CARD_TAG_PREFIX + turno.id)
                .assertCountEquals(1)
        }
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
                    
                        address = RegisterConsumerAddress(
                            street = "Calle Falsa",
                            streetNumber = "123",
                            floor = "2",
                            unit = "A",
                        ),
                    ),
                    accessToken = "fake-token",
                ),
            )
        }
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AuthSessionStoreEntryPoint {
        fun authSessionStore(): AuthSessionStore
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface TurnosRepositoryEntryPoint {
        fun turnosRepository(): FakeTurnosRepository
    }

    @Module
    @InstallIn(SingletonComponent::class)
    object TurnosTestSessionPrefsModule {
        @Provides
        @Singleton
        fun provideSessionPrefs(
            @ApplicationContext context: Context,
        ): SharedPreferences =
            context.getSharedPreferences(
                "auth_session_secure_turnos_test",
                Context.MODE_PRIVATE,
            )
    }

    @Module
    @InstallIn(SingletonComponent::class)
    abstract class MisTurnosTestRepositoryModule {

        @Binds
        @Singleton
        abstract fun bindUserRepository(
            repository: MisTurnosSuccessfulUserRepository,
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

        @Binds
        @Singleton
        abstract fun bindWorkOrderRepository(
            repository: ApiWorkOrderRepository,
        ): WorkOrderRepository

        @Binds
        @Singleton
        abstract fun bindTurnosRepository(
            repository: FakeTurnosRepository,
        ): TurnosRepository
    }

    @Singleton
    class MisTurnosSuccessfulUserRepository @Inject constructor() : UserRepository {
        override suspend fun registerConsumer(data: RegisterConsumerData): UserRegistrationOutcome = UserRegistrationOutcome.Failure.Network(IllegalStateException("not used in this test"))

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

    private companion object {
        // Three representative turnos covering different
        // counterpart ids so the LazyColumn assertion can verify
        // each card by id. All `Confirmed` because that is the
        // only `TurnoStatus` the wire maps today; per-status
        // coverage lives in `TurnoCardTest` (JVM).
        val SEED_TURNOS: List<Turno> = listOf(
            turno(id = "1", counterpartName = "Juan", counterpartSurname = "Gómez"),
            turno(id = "2", counterpartName = "Ana", counterpartSurname = "Pérez"),
            turno(id = "3", counterpartName = "Luis", counterpartSurname = "Suárez"),
        )

        private fun turno(
            id: String,
            counterpartName: String,
            counterpartSurname: String,
        ): Turno = Turno(
            id = id,
            serviceProposalId = "p-$id",
            status = TurnoStatus.Confirmed,
            counterpart = TurnoCounterpart(
                id = "$id-c",
                name = counterpartName,
                surname = counterpartSurname,
                categoryName = "Plomería",
                profilePhotoUrl = null,
            ),
            description = "Reparación de cañería",
            amountCents = 1_500_000L,
            scheduledOnEpochMillis = 1_792_074_600_000L,
        )
    }
}
