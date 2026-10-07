package com.loresuelvo.consumer.instrumented.notifications

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import com.loresuelvo.consumer.platform.notifications.MessageNotificationIntent
import com.loresuelvo.consumer.data.notifications.MessageNotificationDecoder
import com.loresuelvo.consumer.data.api.mapper.toDomain
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loresuelvo.consumer.MainActivity
import com.loresuelvo.consumer.data.auth.SessionStoreModule
import com.loresuelvo.consumer.di.RepositoryModule
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.auth.RegisterConsumerAddress
import com.loresuelvo.consumer.domain.auth.User
import com.loresuelvo.consumer.domain.auth.UserRepository
import com.loresuelvo.consumer.domain.category.CategoryRepository
import com.loresuelvo.consumer.domain.calendar.CalendarConnectionRepository
import com.loresuelvo.consumer.domain.conversation.ConversationRepository
import com.loresuelvo.consumer.domain.diagnosis.DiagnosisRepository
import com.loresuelvo.consumer.domain.jobrequest.JobRequestRepository
import com.loresuelvo.consumer.domain.provider.ProviderRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalRepository
import com.loresuelvo.consumer.domain.turno.TurnosRepository
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailRepository
import com.loresuelvo.consumer.instrumented.diagnosis.FakeDiagnosisRepository
import com.loresuelvo.consumer.instrumented.support.WirePinHarness
import com.loresuelvo.consumer.testdi.FakeCalendarConnectionRepository
import com.loresuelvo.consumer.testdi.FakeConversationRepository
import com.loresuelvo.consumer.testdi.FakeJobRequestRepository
import com.loresuelvo.consumer.testdi.FakeServiceProposalRepository
import com.loresuelvo.consumer.testdi.FakeTurnosRepository
import com.loresuelvo.consumer.testdi.FakeWorkOrderDetailRepository
import dagger.Binds
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import dagger.Module
import dagger.Provides
import javax.inject.Singleton
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import androidx.test.filters.SdkSuppress
import android.Manifest
import android.app.NotificationManager
import androidx.test.rule.GrantPermissionRule
import com.loresuelvo.consumer.data.installation.EncryptedInstallationStateStore
import com.loresuelvo.consumer.platform.notifications.ConsumerMessageReceiver
import com.loresuelvo.consumer.platform.notifications.ConsumerFirebaseMessagingService
import com.loresuelvo.consumer.domain.conversation.*
import javax.inject.Inject
@SdkSuppress(minSdkVersion = 33)
@HiltAndroidTest
@UninstallModules(RepositoryModule::class, SessionStoreModule::class)
@RunWith(AndroidJUnit4::class)
class MessageNotificationInstrumentedTest {
    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val permissions = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
    @get:Rule(order = 2) val compose = createEmptyComposeRule()
    private lateinit var scenario: ActivityScenario<MainActivity>
    @Inject lateinit var sessions: AuthSessionStore
    @Inject lateinit var installations: EncryptedInstallationStateStore
    @Inject lateinit var receiver: ConsumerMessageReceiver
    @Inject lateinit var conversations: TrackingConversationRepository
    private lateinit var context: Context
    private lateinit var payload: Map<String, String>

    @Before fun setup() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
        sessions.saveSession(AuthSession(testUser(), "dummy-jwt"))
        val binding = installations.prepare(17, "instrumented-login")
        installations.confirm(binding)
        conversations.fake.setDetailSeed(ConversationDetail(
            id = "42", status = ConversationStatus.Pending,
            counterpart = ConversationCounterpart(20, "Current", "Provider", "Repairs", null),
            messages = listOf(ConversationMessage("51", ConversationSender.Provider, "Current API message", 1000)),
            updatedOnEpochMillis = 1000,
        ))
        payload = mapOf("version" to "1", "event_id" to "message:51:17", "type" to "conversation.message.created",
            "resource_type" to "conversation", "destination" to "conversation", "resource_id" to "42",
            "recipient_user_id" to "17", "recipient_app" to "consumer", "installation_id" to binding.identity.id,
            "binding_id" to binding.id, "title" to "New message", "body" to "You have a new message in LoResuelvo.",
            "expires_at" to "2099-01-01T00:00:00Z")
        val initialNotification = MessageNotificationDecoder().decode(payload)!!.toDomain().copy(expiresAt = 0)
        scenario = ActivityScenario.launch(MessageNotificationIntent().create(context, initialNotification))
        compose.waitForIdle()
    }

    @After fun cleanup() {
        context.getSystemService(NotificationManager::class.java).cancelAll()
        if (::scenario.isInitialized) scenario.close()
        sessions.clearSession()
    }

    @Test fun tapping_system_message_opens_current_conversation_through_main_activity() {
        org.junit.Assert.assertEquals(com.loresuelvo.consumer.domain.notifications.MessageNotificationOutcome.Published, receiver.receive(payload))
        val notification = context.getSystemService(NotificationManager::class.java).activeNotifications.single().notification
        org.junit.Assert.assertTrue(notification.contentIntent.isImmutable)
        scenario.onActivity { activity ->
            activity.startIntentSender(notification.contentIntent.intentSender, null, 0, 0, 0)
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Current API message").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Current API message").assertIsDisplayed()
        org.junit.Assert.assertEquals("42", conversations.requestedId)
        scenario.recreate()
        compose.waitForIdle()
        compose.onNodeWithText("Current API message").assertIsDisplayed()
    }

    @Test fun messaging_service_is_private_and_notification_permission_declared() {
        val info = context.packageManager.getServiceInfo(android.content.ComponentName(context, ConsumerFirebaseMessagingService::class.java), 0)
        org.junit.Assert.assertFalse(info.exported)
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_PERMISSIONS)
        org.junit.Assert.assertTrue(packageInfo.requestedPermissions?.contains(Manifest.permission.POST_NOTIFICATIONS) == true)
    }

    @Module @InstallIn(SingletonComponent::class)
    object TestSessionPrefsModule {
        @Provides @Singleton fun preferences(@ApplicationContext context: Context): SharedPreferences =
            context.getSharedPreferences("message_instrumented_sessions", 0)
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
    abstract class MessageTestRepositoryModule {

        @Binds
        @Singleton
        abstract fun bindCalendarConnectionRepository(
            repository: FakeCalendarConnectionRepository,
        ): CalendarConnectionRepository


        @Binds
        @Singleton
        abstract fun bindAuthSessionStore(
            store: com.loresuelvo.consumer.data.auth.EncryptedAuthSessionStore,
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

}

@Singleton
class TrackingConversationRepository @Inject constructor(val fake: FakeConversationRepository) : ConversationRepository by fake {
    @Volatile var requestedId: String? = null
    override suspend fun getConversationById(conversationId: String): ConversationDetailOutcome {
        requestedId = conversationId
        return fake.getConversationById(conversationId)
    }
}
