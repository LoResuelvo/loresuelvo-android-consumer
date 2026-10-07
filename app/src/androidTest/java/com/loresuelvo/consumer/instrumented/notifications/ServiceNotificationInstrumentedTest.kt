package com.loresuelvo.consumer.instrumented.notifications

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.rule.GrantPermissionRule
import com.loresuelvo.consumer.MainActivity
import com.loresuelvo.consumer.data.auth.SessionStoreModule
import com.loresuelvo.consumer.data.installation.EncryptedInstallationStateStore
import com.loresuelvo.consumer.data.notifications.ServiceNotificationDecoder
import com.loresuelvo.consumer.data.api.mapper.toDomain
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
import com.loresuelvo.consumer.domain.jobrequest.JobRequestRepository
import com.loresuelvo.consumer.domain.notifications.ServiceNotificationOutcome
import com.loresuelvo.consumer.domain.provider.ProviderRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalCounterpart
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus
import com.loresuelvo.consumer.domain.turno.TurnoStatus
import com.loresuelvo.consumer.domain.turno.TurnosRepository
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetail
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailCounterpart
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailRepository
import com.loresuelvo.consumer.instrumented.diagnosis.FakeDiagnosisRepository
import com.loresuelvo.consumer.instrumented.support.WirePinHarness
import com.loresuelvo.consumer.platform.notifications.ConsumerServiceNotificationReceiver
import com.loresuelvo.consumer.platform.notifications.ServiceNotificationIntent
import com.loresuelvo.consumer.testdi.FakeCalendarConnectionRepository
import com.loresuelvo.consumer.testdi.FakeConversationRepository
import com.loresuelvo.consumer.testdi.FakeJobRequestRepository
import com.loresuelvo.consumer.testdi.FakeServiceProposalRepository
import com.loresuelvo.consumer.testdi.FakeTurnosRepository
import com.loresuelvo.consumer.testdi.FakeWorkOrderDetailRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = 33)
@HiltAndroidTest
@UninstallModules(RepositoryModule::class, SessionStoreModule::class)
@RunWith(AndroidJUnit4::class)
class ServiceNotificationInstrumentedTest {
    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val permissions = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
    @get:Rule(order = 2) val compose = createEmptyComposeRule()
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Inject lateinit var sessions: AuthSessionStore
    @Inject lateinit var installations: EncryptedInstallationStateStore
    @Inject lateinit var receiver: ConsumerServiceNotificationReceiver
    @Inject lateinit var proposalsRepository: FakeServiceProposalRepository
    @Inject lateinit var workOrdersRepository: FakeWorkOrderDetailRepository

    private lateinit var context: Context
    private lateinit var proposalPayload: Map<String, String>
    private lateinit var workOrderPayload: Map<String, String>

    @Before fun setup() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
        sessions.saveSession(AuthSession(testUser(), "dummy-jwt"))
        val binding = installations.prepare(17, "instrumented-service-login")
        installations.confirm(binding)

        proposalsRepository.set(listOf(
            ServiceProposal(
                id = "88",
                conversationId = "42",
                status = ServiceProposalStatus.Pending,
                counterpart = ServiceProposalCounterpart("20", "Juan", "Carlos", "Plomero", null),
                description = "Reparación de cañería",
                amountCents = 1500000,
                scheduledOnEpochMillis = System.currentTimeMillis() + 86400000,
                createdOnEpochMillis = System.currentTimeMillis(),
            )
        ))

        proposalPayload = mapOf(
            "version" to "1",
            "event_id" to "notification:service_proposal_received:101",
            "type" to "service_proposal_received",
            "resource_type" to "service_proposal",
            "destination" to "service_proposal",
            "resource_id" to "88",
            "recipient_user_id" to "17",
            "recipient_app" to "consumer",
            "installation_id" to binding.identity.id,
            "binding_id" to binding.id,
            "title" to "Nueva propuesta",
            "body" to "Recibiste una propuesta de servicio.",
            "expires_at" to "2099-01-01T00:00:00Z",
        )

        workOrderPayload = mapOf(
            "version" to "1",
            "event_id" to "notification:work_order_close_to_scheduled_time:102",
            "type" to "work_order_close_to_scheduled_time",
            "resource_type" to "work_order",
            "destination" to "work_order",
            "resource_id" to "77",
            "recipient_user_id" to "17",
            "recipient_app" to "consumer",
            "installation_id" to binding.identity.id,
            "binding_id" to binding.id,
            "title" to "Turno próximo",
            "body" to "Tenés un servicio programado dentro de las próximas 24 horas.",
            "expires_at" to "2099-01-01T00:00:00Z",
        )

        val initialNotification = ServiceNotificationDecoder().decode(proposalPayload)!!.toDomain().copy(expiresAt = 0)
        scenario = ActivityScenario.launch(ServiceNotificationIntent().create(context, initialNotification))
        compose.waitForIdle()
    }

    @After fun cleanup() {
        context.getSystemService(NotificationManager::class.java).cancelAll()
        if (::scenario.isInitialized) scenario.close()
        sessions.clearSession()
    }

    @Test fun tapping_proposal_notification_opens_mis_servicios() {
        assertEquals(ServiceNotificationOutcome.Published, receiver.receive(proposalPayload))
        val notification = context.getSystemService(NotificationManager::class.java).activeNotifications.single().notification
        assertTrue(notification.contentIntent.isImmutable)
        scenario.onActivity { activity ->
            activity.startIntentSender(notification.contentIntent.intentSender, null, 0, 0, 0)
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Juan Carlos").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Juan Carlos").assertIsDisplayed()
    }

    @Test fun provider_events_are_rejected_and_never_publish() {
        val providerAccepted = proposalPayload + mapOf(
            "event_id" to "notification:service_proposal_accepted:104",
            "type" to "service_proposal_accepted",
            "resource_type" to "work_order",
            "destination" to "work_order",
            "title" to "Propuesta aceptada",
            "body" to "Se confirmó una contratación.",
        )
        val providerPayment = proposalPayload + mapOf(
            "event_id" to "notification:work_order_final_payment_approved:105",
            "type" to "work_order_final_payment_approved",
            "resource_type" to "work_order",
            "destination" to "work_order",
            "title" to "Pago final confirmado",
            "body" to "Se aprobó el pago del saldo de tu servicio.",
        )
        assertEquals(ServiceNotificationOutcome.Invalid, receiver.receive(providerAccepted))
        assertEquals(ServiceNotificationOutcome.Invalid, receiver.receive(providerPayment))
        assertEquals(0, context.getSystemService(NotificationManager::class.java).activeNotifications.size)
    }

    @Module @InstallIn(SingletonComponent::class)
    object TestServiceSessionPrefsModule {
        @Provides @Singleton fun preferences(@ApplicationContext context: Context): SharedPreferences =
            context.getSharedPreferences("service_instrumented_sessions", 0)
        @Provides @Singleton fun userRepository(): UserRepository = object : UserRepository {
            override suspend fun getCurrentUser() = com.loresuelvo.consumer.domain.auth.CurrentUserOutcome.Success(testUser())
            override suspend fun registerConsumer(data: com.loresuelvo.consumer.domain.auth.RegisterConsumerData) =
                com.loresuelvo.consumer.domain.auth.UserRegistrationOutcome.Success(testUser())
        }
    }

    companion object {
        fun testUser() = User("Verified Consumer", "Verified", "Consumer", "consumer@example.test",
            RegisterConsumerAddress("Test street", "1"), backendUserId = 17)
    }

    @Module
    @InstallIn(SingletonComponent::class)
    abstract class ServiceTestRepositoryModule {
        @Binds @Singleton abstract fun bindCalendarConnectionRepository(repository: FakeCalendarConnectionRepository): CalendarConnectionRepository
        @Binds @Singleton abstract fun bindAuthSessionStore(store: com.loresuelvo.consumer.data.auth.EncryptedAuthSessionStore): AuthSessionStore
        @Binds @Singleton abstract fun bindCategoryRepository(repository: WirePinHarness.StubCategoryRepository): CategoryRepository
        @Binds @Singleton abstract fun bindProviderRepository(repository: WirePinHarness.StubProviderRepository): ProviderRepository
        @Binds @Singleton abstract fun bindDiagnosisRepository(repository: FakeDiagnosisRepository): DiagnosisRepository
        @Binds @Singleton abstract fun bindJobRequestRepository(repository: FakeJobRequestRepository): JobRequestRepository
        @Binds @Singleton abstract fun bindConversationRepository(repository: FakeConversationRepository): ConversationRepository
        @Binds @Singleton abstract fun bindServiceProposalRepository(repository: FakeServiceProposalRepository): ServiceProposalRepository
        @Binds @Singleton abstract fun bindTurnosRepository(repository: FakeTurnosRepository): TurnosRepository
        @Binds @Singleton abstract fun bindWorkOrderDetailRepository(repository: FakeWorkOrderDetailRepository): WorkOrderDetailRepository
    }
}
