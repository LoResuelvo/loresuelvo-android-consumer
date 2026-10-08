package com.loresuelvo.consumer.instrumented.notifications

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import com.loresuelvo.consumer.MainActivity
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.data.auth.EncryptedAuthSessionStore
import com.loresuelvo.consumer.data.auth.SessionStoreModule
import com.loresuelvo.consumer.data.installation.EncryptedInstallationStateStore
import com.loresuelvo.consumer.di.RepositoryModule
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.auth.RegisterConsumerAddress
import com.loresuelvo.consumer.domain.auth.User
import com.loresuelvo.consumer.domain.auth.UserRepository
import com.loresuelvo.consumer.domain.calendar.CalendarConnectionRepository
import com.loresuelvo.consumer.domain.category.CategoryRepository
import com.loresuelvo.consumer.domain.conversation.ConversationRepository
import com.loresuelvo.consumer.domain.diagnosis.DiagnosisRepository
import com.loresuelvo.consumer.domain.installation.InstallationBinding
import com.loresuelvo.consumer.domain.jobrequest.JobRequestRepository
import com.loresuelvo.consumer.domain.notifications.ServiceNotification
import com.loresuelvo.consumer.domain.provider.ProviderRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import com.loresuelvo.consumer.domain.turno.Turno
import com.loresuelvo.consumer.domain.turno.TurnoCounterpart
import com.loresuelvo.consumer.domain.turno.TurnoStatus
import com.loresuelvo.consumer.domain.turno.TurnosRepository
import com.loresuelvo.consumer.domain.usecase.notifications.AuthorizeServiceNotificationUseCase
import com.loresuelvo.consumer.domain.workorder.CompletionReport
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetail
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailCounterpart
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailRepository
import com.loresuelvo.consumer.instrumented.diagnosis.FakeDiagnosisRepository
import com.loresuelvo.consumer.instrumented.support.WirePinHarness
import com.loresuelvo.consumer.platform.notifications.ServiceNotificationIntent
import com.loresuelvo.consumer.testdi.FakeCalendarConnectionRepository
import com.loresuelvo.consumer.testdi.FakeConversationRepository
import com.loresuelvo.consumer.testdi.FakeJobRequestRepository
import com.loresuelvo.consumer.testdi.FakeServiceProposalRepository
import com.loresuelvo.consumer.testdi.FakeTurnosRepository
import com.loresuelvo.consumer.testdi.FakeWorkOrderDetailRepository
import com.loresuelvo.consumer.ui.screens.home.HOME_SCREEN_TAG
import com.loresuelvo.consumer.ui.screens.workorderdetail.WORK_ORDER_ERROR_RETRY_TAG
import com.loresuelvo.consumer.ui.screens.workorderdetail.WORK_ORDER_ERROR_TAG
import com.loresuelvo.consumer.ui.screens.workorderdetail.WORK_ORDER_NOT_FOUND_TAG
import com.loresuelvo.consumer.ui.screens.workorderdetail.WORK_ORDER_READY_TAG
import com.loresuelvo.consumer.ui.screens.workorderdetail.WORK_ORDER_SCREEN_TAG
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
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = 33)
@HiltAndroidTest
@UninstallModules(RepositoryModule::class, SessionStoreModule::class)
@RunWith(AndroidJUnit4::class)
class ExactNavigationResolutionInstrumentedTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val permissions: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @get:Rule(order = 2)
    val compose = createEmptyComposeRule()

    private lateinit var scenario: ActivityScenario<MainActivity>
    private lateinit var scenarioStartIntent: Intent
    private val device: UiDevice by lazy {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    private val sessions: AuthSessionStore by lazy {
        EntryPointAccessors.fromApplication(
            ApplicationProvider.getApplicationContext<Application>(),
            ResolutionAuthSessionStoreEntryPoint::class.java,
        ).authSessionStore()
    }

    @Inject lateinit var installations: EncryptedInstallationStateStore
    @Inject lateinit var turnosRepository: FakeTurnosRepository
    @Inject lateinit var workOrdersRepository: FakeWorkOrderDetailRepository

    private lateinit var context: Context
    private lateinit var binding: InstallationBinding
    private lateinit var completionIntent: Intent

    @Before
    fun setup() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
        context.getSystemService(NotificationManager::class.java).cancelAll()

        sessions.saveSession(AuthSession(testUser(), "dummy-jwt"))
        binding = installations.prepare(17, "exact-nav-resolution-instrumented")
        installations.confirm(binding)

        turnosRepository.set(listOf(testTurno("88", TurnoStatus.AwaitingPayment, "Prestador actual")))
        workOrdersRepository.set("88", defaultWorkOrder())

        val completionNotification = ServiceNotification(
            version = "1",
            eventId = "notification:work_order_completion_reported:88",
            type = AuthorizeServiceNotificationUseCase.TYPE_COMPLETION,
            resourceType = AuthorizeServiceNotificationUseCase.DESTINATION_WORK_ORDER,
            destination = AuthorizeServiceNotificationUseCase.DESTINATION_WORK_ORDER,
            resourceId = "88",
            recipientId = 17,
            app = "consumer",
            installationId = binding.identity.id,
            bindingId = binding.id,
            title = "Servicio finalizado",
            body = "El prestador informó la finalización.",
            expiresAt = 2_000_000_000_000L,
        )
        completionIntent = ServiceNotificationIntent().create(context, completionNotification)
    }

    @After
    fun cleanup() {
        workOrdersRepository.failure = null
        if (::context.isInitialized) {
            context.getSystemService(NotificationManager::class.java).cancelAll()
        }
        if (::scenario.isInitialized) {
            scenario.onActivity { activity -> activity.setIntent(scenarioStartIntent) }
            scenario.close()
        }
        sessions.clearSession()
    }

    @Test
    fun network_failure_shows_error_with_retry_and_can_recover() {
        workOrdersRepository.failure = ServiceProposalsOutcome.Failure.Network(IOException("Connection failed"))
        scenario = launchScenario(completionIntent)
        compose.waitForIdle()

        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag(WORK_ORDER_ERROR_TAG).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag(WORK_ORDER_ERROR_TAG).assertIsDisplayed()
        compose.onNodeWithTag(WORK_ORDER_ERROR_RETRY_TAG).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.work_order_error_network)).assertIsDisplayed()

        // Recover on retry
        workOrdersRepository.failure = null
        compose.onNodeWithTag(WORK_ORDER_ERROR_RETRY_TAG).performClick()

        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag(WORK_ORDER_READY_TAG).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag(WORK_ORDER_READY_TAG).assertIsDisplayed()
        compose.onNodeWithText("Estado actual de finalización 88").assertIsDisplayed()

        pressBack()
        awaitHome()
    }

    @Test
    fun resource_not_available_shows_not_found_and_returns_home() {
        workOrdersRepository.set("88", null)
        scenario = launchScenario(completionIntent)
        compose.waitForIdle()

        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag(WORK_ORDER_NOT_FOUND_TAG).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag(WORK_ORDER_NOT_FOUND_TAG).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.work_order_not_found)).assertIsDisplayed()

        // Privacy: no private data of another account or invented data
        compose.onAllNodesWithTag(WORK_ORDER_READY_TAG).assertCountEquals(0)
        compose.onAllNodesWithText("Estado actual de finalización 88").assertCountEquals(0)

        pressBack()
        awaitHome()
    }

    @Test
    fun access_denied_shows_message_without_retry_and_returns_home() {
        workOrdersRepository.failure = ServiceProposalsOutcome.Failure.AccessDenied
        scenario = launchScenario(completionIntent)
        compose.waitForIdle()

        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag(WORK_ORDER_ERROR_TAG).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag(WORK_ORDER_ERROR_TAG).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.service_access_denied)).assertIsDisplayed()
        compose.onAllNodesWithTag(WORK_ORDER_ERROR_RETRY_TAG).assertCountEquals(0)

        // Privacy: no private data shown
        compose.onAllNodesWithTag(WORK_ORDER_READY_TAG).assertCountEquals(0)
        compose.onAllNodesWithText("Estado actual de finalización 88").assertCountEquals(0)

        pressBack()
        awaitHome()
    }

    @Test
    fun inactive_session_navigates_to_welcome_without_opening_order() {
        sessions.clearSession()
        scenario = launchScenario(completionIntent)
        compose.waitForIdle()

        compose.waitUntil(10_000) {
            compose.onAllNodesWithText(context.getString(R.string.welcome_title)).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(context.getString(R.string.welcome_title)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.welcome_register)).assertIsDisplayed()

        // Old order is not opened and no private data is shown
        compose.onAllNodesWithTag(WORK_ORDER_SCREEN_TAG).assertCountEquals(0)
        compose.onAllNodesWithTag(WORK_ORDER_READY_TAG).assertCountEquals(0)
        compose.onAllNodesWithText("Estado actual de finalización 88").assertCountEquals(0)
    }

    private fun launchScenario(intent: Intent): ActivityScenario<MainActivity> {
        val startIntent = Intent(intent).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        scenarioStartIntent = startIntent
        return ActivityScenario.launch(startIntent)
    }

    private fun pressBack() {
        scenario.onActivity { activity ->
            assertEquals(Lifecycle.State.RESUMED, activity.lifecycle.currentState)
        }
        assertEquals(context.packageName, device.currentPackageName)
        assertTrue(device.pressBack())
        compose.waitForIdle()
    }

    private fun awaitHome() {
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag(HOME_SCREEN_TAG).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag(HOME_SCREEN_TAG).assertIsDisplayed()
    }

    private fun testTurno(id: String, status: TurnoStatus, providerName: String) = Turno(
        id = id,
        serviceProposalId = "101",
        status = status,
        counterpart = TurnoCounterpart("20", providerName, "Apellido", "Plomería", null),
        description = "Turno actual $id",
        amountCents = 1800000L,
        scheduledOnEpochMillis = System.currentTimeMillis() + 86400000L,
    )

    private fun defaultWorkOrder() = WorkOrderDetail(
        proposalId = "101",
        provider = WorkOrderDetailCounterpart("20", "Prestador actual", "Apellido", "Plomería", null),
        description = "Estado actual de finalización 88",
        amountCents = 1800000L,
        scheduledOnEpochMillis = System.currentTimeMillis() + 86400000L,
        acceptedOnEpochMillis = System.currentTimeMillis(),
        paidOnEpochMillis = null,
        status = TurnoStatus.AwaitingPayment,
        completionReport = CompletionReport(
            id = "report-88",
            description = "Informe reportado actualmente",
            reportedOnEpochMillis = 2000L,
            images = emptyList(),
        ),
        review = null,
        estimatedDurationMinutes = 60,
    )

    @Module
    @InstallIn(SingletonComponent::class)
    object ResolutionTestSessionPrefsModule {
        @Provides
        @Singleton
        fun preferences(@ApplicationContext context: Context): SharedPreferences =
            context.getSharedPreferences("exact_nav_res_sessions", 0)

        @Provides
        @Singleton
        fun userRepository(): UserRepository = object : UserRepository {
            override suspend fun getCurrentUser() =
                com.loresuelvo.consumer.domain.auth.CurrentUserOutcome.Success(testUser())

            override suspend fun registerConsumer(data: com.loresuelvo.consumer.domain.auth.RegisterConsumerData) =
                com.loresuelvo.consumer.domain.auth.UserRegistrationOutcome.Success(testUser())
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ResolutionAuthSessionStoreEntryPoint {
        fun authSessionStore(): AuthSessionStore
    }

    @Module
    @InstallIn(SingletonComponent::class)
    abstract class ExactNavigationResolutionRepositoryModule {
        @Binds
        @Singleton
        abstract fun bindCalendarConnectionRepository(
            repository: FakeCalendarConnectionRepository,
        ): CalendarConnectionRepository

        @Binds
        @Singleton
        abstract fun bindAuthSessionStore(
            store: EncryptedAuthSessionStore,
        ): AuthSessionStore

        @Binds
        @Singleton
        abstract fun bindCategoryRepository(
            repository: WirePinHarness.StubCategoryRepository,
        ): CategoryRepository

        @Binds
        @Singleton
        abstract fun bindProviderRepository(
            repository: WirePinHarness.StubProviderRepository,
        ): ProviderRepository

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
        abstract fun bindTurnosRepository(
            repository: FakeTurnosRepository,
        ): TurnosRepository

        @Binds
        @Singleton
        abstract fun bindWorkOrderDetailRepository(
            repository: FakeWorkOrderDetailRepository,
        ): WorkOrderDetailRepository
    }

    companion object {
        fun testUser() = User(
            displayName = "Verified Consumer",
            firstName = "Verified",
            lastName = "Consumer",
            email = "consumer@example.test",
            address = RegisterConsumerAddress("Test street", "1"),
            backendUserId = 17,
        )
    }
}
