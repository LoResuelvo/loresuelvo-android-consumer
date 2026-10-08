package com.loresuelvo.consumer.instrumented.notifications

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.rule.GrantPermissionRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import dagger.hilt.EntryPoint
import dagger.hilt.android.EntryPointAccessors
import com.loresuelvo.consumer.MainActivity
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
import com.loresuelvo.consumer.domain.conversation.ConversationCounterpart
import com.loresuelvo.consumer.domain.conversation.ConversationDetail
import com.loresuelvo.consumer.domain.conversation.ConversationMessage
import com.loresuelvo.consumer.domain.conversation.ConversationRepository
import com.loresuelvo.consumer.domain.conversation.ConversationSender
import com.loresuelvo.consumer.domain.conversation.ConversationStatus
import com.loresuelvo.consumer.domain.diagnosis.DiagnosisRepository
import com.loresuelvo.consumer.domain.installation.InstallationBinding
import com.loresuelvo.consumer.domain.jobrequest.JobRequestRepository
import com.loresuelvo.consumer.domain.notifications.MessageNotification
import com.loresuelvo.consumer.domain.notifications.ServiceNotification
import com.loresuelvo.consumer.domain.provider.ProviderRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalCounterpart
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus
import com.loresuelvo.consumer.domain.workorder.CompletionReport
import com.loresuelvo.consumer.domain.turno.Turno
import com.loresuelvo.consumer.domain.turno.TurnoCounterpart
import com.loresuelvo.consumer.domain.turno.TurnoStatus
import com.loresuelvo.consumer.domain.turno.TurnosRepository
import com.loresuelvo.consumer.domain.usecase.notifications.AuthorizeServiceNotificationUseCase
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetail
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailCounterpart
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailRepository
import com.loresuelvo.consumer.instrumented.diagnosis.FakeDiagnosisRepository
import com.loresuelvo.consumer.instrumented.support.WirePinHarness
import com.loresuelvo.consumer.platform.notifications.MessageNotificationIntent
import com.loresuelvo.consumer.platform.notifications.ServiceNotificationIntent
import com.loresuelvo.consumer.testdi.FakeCalendarConnectionRepository
import com.loresuelvo.consumer.testdi.FakeJobRequestRepository
import com.loresuelvo.consumer.testdi.FakeServiceProposalRepository
import com.loresuelvo.consumer.testdi.FakeTurnosRepository
import com.loresuelvo.consumer.testdi.FakeWorkOrderDetailRepository
import com.loresuelvo.consumer.ui.screens.chat.CONVERSATION_SCREEN_TAG
import com.loresuelvo.consumer.ui.screens.home.HOME_SCREEN_TAG
import com.loresuelvo.consumer.ui.screens.misservicios.MIS_SERVICIOS_SCREEN_TAG
import com.loresuelvo.consumer.ui.screens.paymentresult.PAYMENT_RESULT_SCREEN_TAG
import com.loresuelvo.consumer.ui.screens.proposals.PROPOSAL_DETAIL_READY_TAG
import com.loresuelvo.consumer.ui.screens.workorderdetail.WORK_ORDER_DESCRIPTION_TAG
import com.loresuelvo.consumer.ui.screens.workorderdetail.WORK_ORDER_EVIDENCE_DESCRIPTION_TAG
import com.loresuelvo.consumer.ui.screens.workorderdetail.WORK_ORDER_PAY_NOW_TAG
import com.loresuelvo.consumer.ui.screens.workorderdetail.WORK_ORDER_PROVIDER_TAG
import com.loresuelvo.consumer.ui.screens.workorderdetail.WORK_ORDER_READY_TAG
import com.loresuelvo.consumer.ui.screens.workorderdetail.WORK_ORDER_SCREEN_TAG
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import javax.inject.Singleton

@SdkSuppress(minSdkVersion = 33)
@HiltAndroidTest
@UninstallModules(RepositoryModule::class, SessionStoreModule::class)
@RunWith(AndroidJUnit4::class)
class ExactNavigationInstrumentedTest {

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
            AuthSessionStoreEntryPoint::class.java,
        ).authSessionStore()
    }
    @Inject lateinit var installations: EncryptedInstallationStateStore
    @Inject lateinit var conversations: TrackingConversationRepository
    @Inject lateinit var proposalsRepository: FakeServiceProposalRepository
    @Inject lateinit var turnosRepository: FakeTurnosRepository
    @Inject lateinit var workOrdersRepository: FakeWorkOrderDetailRepository

    private lateinit var context: Context
    private lateinit var binding: InstallationBinding
    private lateinit var messageIntent: Intent
    private lateinit var proposalIntent: Intent
    private lateinit var reminderIntent: Intent
    private lateinit var completionIntent: Intent

    @Before
    fun setup() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
        context.getSystemService(NotificationManager::class.java).cancelAll()

        sessions.saveSession(AuthSession(testUser(), "dummy-jwt"))
        binding = installations.prepare(17, "exact-nav-instrumented")
        installations.confirm(binding)

        conversations.fake.setDetailSeed(
            ConversationDetail(
                id = "42",
                status = ConversationStatus.Pending,
                counterpart = ConversationCounterpart(20L, "Current", "Provider", "Repairs", null),
                messages = listOf(
                    ConversationMessage("51", ConversationSender.Provider, "Mensaje anterior al aviso", 1000L),
                ),
                updatedOnEpochMillis = 1000L,
            )
        )

        proposalsRepository.set(
            listOf(
                ServiceProposal(
                    id = "88",
                    conversationId = "42",
                    status = ServiceProposalStatus.Pending,
                    counterpart = ServiceProposalCounterpart("20", "Pedro", "Propuesta", "Plomero", null),
                    description = "Descripción anterior de propuesta 88",
                    amountCents = 1500000L,
                    scheduledOnEpochMillis = System.currentTimeMillis() + 86400000L,
                    createdOnEpochMillis = System.currentTimeMillis(),
                )
            )
        )

        turnosRepository.set(listOf(testTurno("88", TurnoStatus.Confirmed, "Carlos")))
        workOrdersRepository.set(
            "88",
            WorkOrderDetail(
                proposalId = "101",
                provider = WorkOrderDetailCounterpart("20", "Carlos", "Plomero", "Plomería", null),
                description = "Estado anterior de la orden 88",
                amountCents = 2500000L,
                scheduledOnEpochMillis = System.currentTimeMillis() + 86400000L,
                acceptedOnEpochMillis = System.currentTimeMillis(),
                paidOnEpochMillis = null,
                status = TurnoStatus.Confirmed,
                completionReport = null,
                review = null,
                estimatedDurationMinutes = 60,
            )
        )

        val messageNotification = MessageNotification(
            version = "1",
            eventId = "message:42:17",
            type = "conversation.message.created",
            resourceType = "conversation",
            destination = "conversation",
            conversationId = 42,
            recipientId = 17,
            app = "consumer",
            installationId = binding.identity.id,
            bindingId = binding.id,
            title = "Nuevo mensaje",
            body = "Tenés un nuevo mensaje en LoResuelvo.",
            expiresAt = 2_000_000_000_000L,
        )
        messageIntent = MessageNotificationIntent().create(context, messageNotification)

        val proposalNotification = ServiceNotification(
            version = "1",
            eventId = "notification:service_proposal_received:88",
            type = AuthorizeServiceNotificationUseCase.TYPE_PROPOSAL_RECEIVED,
            resourceType = AuthorizeServiceNotificationUseCase.DESTINATION_PROPOSAL,
            destination = AuthorizeServiceNotificationUseCase.DESTINATION_PROPOSAL,
            resourceId = "88",
            recipientId = 17,
            app = "consumer",
            installationId = binding.identity.id,
            bindingId = binding.id,
            title = "Nueva propuesta",
            body = "Recibiste una propuesta de servicio.",
            expiresAt = 2_000_000_000_000L,
        )
        proposalIntent = ServiceNotificationIntent().create(context, proposalNotification)

        val reminderNotification = ServiceNotification(
            version = "1",
            eventId = "notification:work_order_close_to_scheduled_time:88",
            type = AuthorizeServiceNotificationUseCase.TYPE_REMINDER,
            resourceType = AuthorizeServiceNotificationUseCase.DESTINATION_WORK_ORDER,
            destination = AuthorizeServiceNotificationUseCase.DESTINATION_WORK_ORDER,
            resourceId = "88",
            recipientId = 17,
            app = "consumer",
            installationId = binding.identity.id,
            bindingId = binding.id,
            title = "Turno próximo",
            body = "Tenés un servicio programado dentro de las próximas 24 horas.",
            expiresAt = 2_000_000_000_000L,
        )
        reminderIntent = ServiceNotificationIntent().create(context, reminderNotification)

        completionIntent = ServiceNotificationIntent().create(
            context,
            reminderNotification.copy(
                eventId = "notification:work_order_completion_reported:88",
                type = AuthorizeServiceNotificationUseCase.TYPE_COMPLETION,
                title = "Servicio finalizado",
                body = "El prestador informó la finalización.",
            ),
        )
    }

    @After
    fun cleanup() {
        context.getSystemService(NotificationManager::class.java).cancelAll()
        if (::scenario.isInitialized) {
            scenario.onActivity { activity -> activity.setIntent(scenarioStartIntent) }
            scenario.close()
        }
        sessions.clearSession()
    }

    @Test
    fun cold_message_notice_opens_current_conversation() = exerciseCold(messageIntent)

    @Test
    fun warm_message_pending_intent_opens_current_conversation() = exerciseWarm(messageIntent)

    @Test
    fun cold_proposal_notice_opens_exact_current_proposal() = exerciseCold(proposalIntent)

    @Test
    fun warm_proposal_pending_intent_opens_exact_current_proposal() = exerciseWarm(proposalIntent)

    @Test
    fun cold_reminder_notice_opens_current_work_order() = exerciseCold(reminderIntent)

    @Test
    fun warm_reminder_pending_intent_opens_current_work_order() = exerciseWarm(reminderIntent)

    @Test
    fun cold_completion_notice_opens_current_completion_report() = exerciseCold(completionIntent)

    @Test
    fun warm_completion_pending_intent_opens_current_completion_report() = exerciseWarm(completionIntent)

    @Test
    fun payment_return_after_notification_still_opens_payment_result() {
        scenario = launchScenario(messageIntent, expired = true)
        compose.waitForIdle()
        refreshCurrentAccountData(messageIntent)
        deliverPendingIntent(messageIntent)
        awaitFreshDestination(messageIntent)

        val paymentReturn = Intent(
            Intent.ACTION_VIEW,
            Uri.Builder()
                .scheme("https")
                .authority(com.loresuelvo.consumer.BuildConfig.PAYMENT_RETURN_HOST)
                .appendPath("payments")
                .appendPath("success")
                .appendQueryParameter("external_reference", "payment-88")
                .build(),
        ).setClass(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        scenario.onActivity { activity -> activity.startActivity(paymentReturn) }

        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag(PAYMENT_RESULT_SCREEN_TAG).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag(PAYMENT_RESULT_SCREEN_TAG).assertIsDisplayed()
    }

    private fun exerciseCold(intent: Intent) {
        refreshCurrentAccountData(intent)
        scenario = launchScenario(intent)
        compose.waitForIdle()
        assertFreshDestinationAndBack(intent)
    }

    private fun exerciseWarm(intent: Intent) {
        scenario = launchScenario(intent, expired = true)
        compose.waitForIdle()
        refreshCurrentAccountData(intent)
        deliverPendingIntent(intent)
        compose.waitForIdle()
        assertFreshDestinationAndBack(intent)
    }

    private fun assertFreshDestinationAndBack(intent: Intent) {
        awaitFreshDestination(intent)
        deliverPendingIntent(intent)
        compose.waitForIdle()
        awaitFreshDestination(intent)

        if (intent.getStringExtra("type") == AuthorizeServiceNotificationUseCase.TYPE_PROPOSAL_RECEIVED) {
            pressBack()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithTag(PROPOSAL_DETAIL_READY_TAG).fetchSemanticsNodes().isEmpty() &&
                    compose.onAllNodesWithTag(MIS_SERVICIOS_SCREEN_TAG).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag(MIS_SERVICIOS_SCREEN_TAG).assertIsDisplayed()
            pressBack()
        } else {
            pressBack()
        }

        awaitHome()
    }

    private fun awaitFreshDestination(intent: Intent) {
        when (intent.getStringExtra("type")) {
            "conversation.message.created" -> {
                compose.waitUntil(10_000) {
                    compose.onAllNodesWithText("Mensaje actualizado de mi cuenta").fetchSemanticsNodes().isNotEmpty()
                }
                compose.onNodeWithTag(CONVERSATION_SCREEN_TAG).assertIsDisplayed()
                compose.onNodeWithText("Mensaje actualizado de mi cuenta").assertIsDisplayed()
                assertEquals("42", conversations.requestedId)
                compose.onAllNodesWithTag(CONVERSATION_SCREEN_TAG).assertCountEquals(1)
            }
            AuthorizeServiceNotificationUseCase.TYPE_PROPOSAL_RECEIVED -> {
                compose.waitUntil(10_000) {
                    compose.onAllNodesWithTag(PROPOSAL_DETAIL_READY_TAG).fetchSemanticsNodes().isNotEmpty()
                }
                compose.onNodeWithTag(PROPOSAL_DETAIL_READY_TAG).assertIsDisplayed()
                compose.onNodeWithText("Detalle actual de la propuesta 88").assertIsDisplayed()
                compose.onAllNodes(
                    hasText("Detalle señuelo de la propuesta 99")
                        .and(hasAnyAncestor(hasTestTag(PROPOSAL_DETAIL_READY_TAG))),
                ).assertCountEquals(0)
                compose.onAllNodesWithTag(PROPOSAL_DETAIL_READY_TAG).assertCountEquals(1)
            }
            AuthorizeServiceNotificationUseCase.TYPE_REMINDER -> {
                compose.waitUntil(10_000) {
                    compose.onAllNodesWithTag(WORK_ORDER_READY_TAG).fetchSemanticsNodes().isNotEmpty()
                }
                compose.onNodeWithTag(WORK_ORDER_SCREEN_TAG).assertIsDisplayed()
                compose.onNodeWithTag(WORK_ORDER_DESCRIPTION_TAG).assertIsDisplayed()
                compose.onNodeWithTag(WORK_ORDER_PROVIDER_TAG).assertIsDisplayed()
                compose.onNodeWithText("Estado actual del turno 88").assertIsDisplayed()
                compose.onNodeWithText("Prestador actual Apellido").assertIsDisplayed()
                assertEquals("88", workOrdersRepository.lastRequestedWorkOrderId)
                compose.onAllNodesWithTag(WORK_ORDER_SCREEN_TAG).assertCountEquals(1)
            }
            AuthorizeServiceNotificationUseCase.TYPE_COMPLETION -> {
                compose.waitUntil(10_000) {
                    compose.onAllNodesWithTag(WORK_ORDER_EVIDENCE_DESCRIPTION_TAG).fetchSemanticsNodes().isNotEmpty() &&
                        compose.onAllNodesWithTag(WORK_ORDER_PAY_NOW_TAG).fetchSemanticsNodes().isNotEmpty()
                }
                compose.onNodeWithTag(WORK_ORDER_SCREEN_TAG).assertIsDisplayed()
                compose.onNodeWithTag(WORK_ORDER_PAY_NOW_TAG).assertIsDisplayed()
                compose.onNodeWithTag(WORK_ORDER_EVIDENCE_DESCRIPTION_TAG)
                    .performScrollTo()
                    .assertIsDisplayed()
                compose.onNodeWithText("Informe reportado actualmente").assertIsDisplayed()
                assertEquals("88", workOrdersRepository.lastRequestedWorkOrderId)
                compose.onAllNodesWithTag(WORK_ORDER_SCREEN_TAG).assertCountEquals(1)
            }
            else -> error("Unknown notification type: ${intent.getStringExtra("type")}")
        }
    }

    private fun refreshCurrentAccountData(intent: Intent) {
        conversations.fake.setDetailSeed(
            ConversationDetail(
                id = "42",
                status = ConversationStatus.Pending,
                counterpart = ConversationCounterpart(20L, "Actual", "Prestador", "Plomería", null),
                messages = listOf(
                    ConversationMessage("52", ConversationSender.Provider, "Mensaje actualizado de mi cuenta", 2000L),
                ),
                updatedOnEpochMillis = 2000L,
            ),
        )
        proposalsRepository.set(
            listOf(
                ServiceProposal(
                    id = "99",
                    conversationId = "90",
                    status = ServiceProposalStatus.Pending,
                    counterpart = ServiceProposalCounterpart("21", "Otra", "Prestadora", "Electricidad", null),
                    description = "Detalle señuelo de la propuesta 99",
                    amountCents = 900000L,
                    scheduledOnEpochMillis = System.currentTimeMillis() + 86400000L,
                    createdOnEpochMillis = System.currentTimeMillis(),
                ),
                ServiceProposal(
                    id = "88",
                    conversationId = "42",
                    status = ServiceProposalStatus.Pending,
                    counterpart = ServiceProposalCounterpart("20", "Actualizada", "Prestadora", "Plomería", null),
                    description = "Detalle actual de la propuesta 88",
                    amountCents = 1800000L,
                    scheduledOnEpochMillis = System.currentTimeMillis() + 172800000L,
                    createdOnEpochMillis = System.currentTimeMillis(),
                ),
            ),
        )

        val type = intent.getStringExtra("type")
        val status = when (type) {
            AuthorizeServiceNotificationUseCase.TYPE_COMPLETION -> TurnoStatus.AwaitingPayment
            else -> TurnoStatus.Confirmed
        }
        val completionReport = if (type == AuthorizeServiceNotificationUseCase.TYPE_COMPLETION) {
            CompletionReport(
                id = "report-88",
                description = "Informe reportado actualmente",
                reportedOnEpochMillis = 2000L,
                images = emptyList(),
            )
        } else {
            null
        }
        turnosRepository.set(
            listOf(
                testTurno("88", status, "Prestador actual"),
                testTurno("99", TurnoStatus.Confirmed, "Prestador señuelo"),
            ),
        )
        workOrdersRepository.set(
            "88",
            WorkOrderDetail(
                proposalId = "101",
                provider = WorkOrderDetailCounterpart("20", "Prestador actual", "Apellido", "Plomería", null),
                description = if (type == AuthorizeServiceNotificationUseCase.TYPE_REMINDER) {
                    "Estado actual del turno 88"
                } else {
                    "Estado actual de finalización 88"
                },
                amountCents = 1800000L,
                scheduledOnEpochMillis = System.currentTimeMillis() + 86400000L,
                acceptedOnEpochMillis = System.currentTimeMillis(),
                paidOnEpochMillis = if (status == TurnoStatus.Paid) System.currentTimeMillis() else null,
                status = status,
                completionReport = completionReport,
                review = null,
                estimatedDurationMinutes = 60,
            ),
        )
    }

    private fun scenarioLaunchIntent(intent: Intent, expired: Boolean = false): Intent =
        Intent(intent).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            if (expired) putExtra("expires_at", 0L)
        }

    private fun launchScenario(intent: Intent, expired: Boolean = false): ActivityScenario<MainActivity> =
        scenarioLaunchIntent(intent, expired).let { startIntent ->
            scenarioStartIntent = startIntent
            ActivityScenario.launch(startIntent)
        }

    private fun deliverPendingIntent(intent: Intent) {
        val pendingIntent = PendingIntent.getActivity(
            context,
            requireNotNull(intent.dataString).hashCode(),
            Intent(intent),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        pendingIntent.send()
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
        serviceProposalId = if (id == "88") "101" else "202",
        status = status,
        counterpart = TurnoCounterpart("20", providerName, "Apellido", "Plomería", null),
        description = "Turno actual $id",
        amountCents = 1800000L,
        scheduledOnEpochMillis = System.currentTimeMillis() + 86400000L,
    )

    @Module
    @InstallIn(SingletonComponent::class)
    object TestSessionPrefsModule {
        @Provides
        @Singleton
        fun preferences(@ApplicationContext context: Context): SharedPreferences =
            context.getSharedPreferences("exact_nav_sessions", 0)

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
    interface AuthSessionStoreEntryPoint {
        fun authSessionStore(): AuthSessionStore
    }

    @Module
    @InstallIn(SingletonComponent::class)
    abstract class ExactNavigationRepositoryModule {
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
            repository: TrackingConversationRepository,
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
