package com.loresuelvo.consumer.instrumented.notifications

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.rule.GrantPermissionRule
import com.loresuelvo.consumer.data.auth.EncryptedAuthSessionStore
import com.loresuelvo.consumer.data.installation.EncryptedInstallationStateStore
import com.loresuelvo.consumer.data.notifications.MessageNotificationDecoder
import com.loresuelvo.consumer.data.notifications.ServiceNotificationDecoder
import com.loresuelvo.consumer.data.notifications.StoredNotificationEvents
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.RegisterConsumerAddress
import com.loresuelvo.consumer.domain.auth.User
import com.loresuelvo.consumer.domain.installation.InstallationBinding
import com.loresuelvo.consumer.domain.notifications.MessageNotificationOutcome
import com.loresuelvo.consumer.domain.notifications.NotificationClock
import com.loresuelvo.consumer.domain.notifications.ServiceNotificationOutcome
import com.loresuelvo.consumer.domain.usecase.notifications.AuthorizeMessageNotificationUseCase
import com.loresuelvo.consumer.domain.usecase.notifications.AuthorizeServiceNotificationUseCase
import com.loresuelvo.consumer.domain.usecase.notifications.ReceiveMessageNotificationUseCase
import com.loresuelvo.consumer.domain.usecase.notifications.ReceiveServiceNotificationUseCase
import com.loresuelvo.consumer.platform.notifications.AndroidMessageNotificationPublisher
import com.loresuelvo.consumer.platform.notifications.AndroidServiceNotificationPublisher
import com.loresuelvo.consumer.platform.notifications.ConsumerMessageReceiver
import com.loresuelvo.consumer.platform.notifications.ConsumerServiceNotificationReceiver
import com.loresuelvo.consumer.platform.notifications.VisibleConversationStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = 33)
@RunWith(AndroidJUnit4::class)
class DeduplicationAndExpirationInstrumentedTest {

    @get:Rule
    val permissions: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    private lateinit var context: Context
    private lateinit var manager: NotificationManager
    private lateinit var sessions: EncryptedAuthSessionStore
    private lateinit var installations: EncryptedInstallationStateStore
    private lateinit var binding: InstallationBinding
    private val controlledNow = 1_000_000L
    private val clock = NotificationClock { controlledNow }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        manager = context.getSystemService(NotificationManager::class.java)
        manager.cancelAll()

        val sessionPrefs = context.getSharedPreferences("test_dedup_sessions", Context.MODE_PRIVATE)
        val installPrefs = context.getSharedPreferences("test_dedup_installations", Context.MODE_PRIVATE)
        val eventPrefs = context.getSharedPreferences("test_dedup_events", Context.MODE_PRIVATE)
        sessionPrefs.edit().clear().commit()
        installPrefs.edit().clear().commit()
        eventPrefs.edit().clear().commit()

        sessions = EncryptedAuthSessionStore(sessionPrefs)
        sessions.saveSession(
            AuthSession(
                User(
                    displayName = "Verified Consumer",
                    firstName = "Verified",
                    lastName = "Consumer",
                    email = "consumer@example.test",
                    address = RegisterConsumerAddress("Street", "1"),
                    backendUserId = 17,
                ),
                "test-jwt",
            )
        )
        installations = EncryptedInstallationStateStore(installPrefs)
        binding = installations.prepare(17, "dedup-test-login")
        installations.confirm(binding)
    }

    @After
    fun tearDown() {
        manager.cancelAll()
    }

    @Test
    fun duplicate_message_is_not_republished_to_notification_manager() {
        val eventPrefs = context.getSharedPreferences("test_dedup_events", Context.MODE_PRIVATE)
        val events = StoredNotificationEvents(eventPrefs)
        val authorize = AuthorizeMessageNotificationUseCase(sessions, installations, clock)
        val publisher = AndroidMessageNotificationPublisher(context, authorize)
        val useCase = ReceiveMessageNotificationUseCase(
            authorize = authorize,
            visibility = VisibleConversationStore(),
            availability = publisher,
            events = events,
            publisher = publisher,
        )
        val receiver = ConsumerMessageReceiver(MessageNotificationDecoder(), useCase)

        val payload = mapOf(
            "version" to "1",
            "event_id" to "message:200:17",
            "type" to "conversation.message.created",
            "resource_type" to "conversation",
            "destination" to "conversation",
            "resource_id" to "42",
            "recipient_user_id" to "17",
            "recipient_app" to "consumer",
            "installation_id" to binding.identity.id,
            "binding_id" to binding.id,
            "title" to "Nuevo mensaje",
            "body" to "Tenés un nuevo mensaje en LoResuelvo.",
            "expires_at" to "2099-01-01T00:00:00Z",
        )

        val firstOutcome = receiver.receive(payload)
        assertEquals(MessageNotificationOutcome.Published, firstOutcome)
        val activeAfterFirst = manager.activeNotifications.filter { it.tag == "${binding.id}:message:200:17" }
        assertEquals(1, activeAfterFirst.size)

        val secondOutcome = receiver.receive(payload)
        assertEquals(MessageNotificationOutcome.Duplicate, secondOutcome)
        val activeAfterSecond = manager.activeNotifications.filter { it.tag == "${binding.id}:message:200:17" }
        assertEquals("Duplicate must not add another notification to NotificationManager", 1, activeAfterSecond.size)
    }

    @Test
    fun duplicate_service_notification_is_not_republished_to_notification_manager() {
        val eventPrefs = context.getSharedPreferences("test_dedup_events", Context.MODE_PRIVATE)
        val events = StoredNotificationEvents(eventPrefs)
        val authorize = AuthorizeServiceNotificationUseCase(sessions, installations, clock)
        val publisher = AndroidServiceNotificationPublisher(context, authorize)
        val useCase = ReceiveServiceNotificationUseCase(
            authorize = authorize,
            availability = publisher,
            events = events,
            publisher = publisher,
        )
        val receiver = ConsumerServiceNotificationReceiver(ServiceNotificationDecoder(), useCase)

        val payload = mapOf(
            "version" to "1",
            "event_id" to "notification:service_proposal_received:500",
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

        val firstOutcome = receiver.receive(payload)
        assertEquals(ServiceNotificationOutcome.Published, firstOutcome)
        val activeAfterFirst = manager.activeNotifications.filter {
            it.tag == "${binding.id}:notification:service_proposal_received:500"
        }
        assertEquals(1, activeAfterFirst.size)

        val secondOutcome = receiver.receive(payload)
        assertEquals(ServiceNotificationOutcome.Duplicate, secondOutcome)
        val activeAfterSecond = manager.activeNotifications.filter {
            it.tag == "${binding.id}:notification:service_proposal_received:500"
        }
        assertEquals("Duplicate must not add another notification to NotificationManager", 1, activeAfterSecond.size)
    }

    @Test
    fun expired_message_and_service_notifications_return_invalid_and_do_not_publish() {
        val eventPrefs = context.getSharedPreferences("test_dedup_events", Context.MODE_PRIVATE)
        val events = StoredNotificationEvents(eventPrefs)

        val authorizeMessage = AuthorizeMessageNotificationUseCase(sessions, installations, clock)
        val messagePublisher = AndroidMessageNotificationPublisher(context, authorizeMessage)
        val messageUseCase = ReceiveMessageNotificationUseCase(
            authorize = authorizeMessage,
            visibility = VisibleConversationStore(),
            availability = messagePublisher,
            events = events,
            publisher = messagePublisher,
        )
        val messageReceiver = ConsumerMessageReceiver(MessageNotificationDecoder(), messageUseCase)

        val expiredMessagePayload = mapOf(
            "version" to "1",
            "event_id" to "message:201:17",
            "type" to "conversation.message.created",
            "resource_type" to "conversation",
            "destination" to "conversation",
            "resource_id" to "42",
            "recipient_user_id" to "17",
            "recipient_app" to "consumer",
            "installation_id" to binding.identity.id,
            "binding_id" to binding.id,
            "title" to "Nuevo mensaje",
            "body" to "Tenés un nuevo mensaje en LoResuelvo.",
            "expires_at" to "1970-01-01T00:00:00Z",
        )
        val messageOutcome = messageReceiver.receive(expiredMessagePayload)
        assertEquals(MessageNotificationOutcome.Invalid, messageOutcome)

        val authorizeService = AuthorizeServiceNotificationUseCase(sessions, installations, clock)
        val servicePublisher = AndroidServiceNotificationPublisher(context, authorizeService)
        val serviceUseCase = ReceiveServiceNotificationUseCase(
            authorize = authorizeService,
            availability = servicePublisher,
            events = events,
            publisher = servicePublisher,
        )
        val serviceReceiver = ConsumerServiceNotificationReceiver(ServiceNotificationDecoder(), serviceUseCase)

        val expiredServicePayload = mapOf(
            "version" to "1",
            "event_id" to "notification:service_proposal_received:501",
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
            "expires_at" to "1970-01-01T00:00:00Z",
        )
        val serviceOutcome = serviceReceiver.receive(expiredServicePayload)
        assertEquals(ServiceNotificationOutcome.Invalid, serviceOutcome)

        assertTrue(
            "No notification should be published for expired events",
            manager.activeNotifications.isEmpty(),
        )
    }

    @Test
    fun deduplication_persists_across_recreated_receiver_and_store_instances() {
        val eventPrefs = context.getSharedPreferences("test_dedup_events", Context.MODE_PRIVATE)
        val firstStore = StoredNotificationEvents(eventPrefs)
        val authorize = AuthorizeMessageNotificationUseCase(sessions, installations, clock)
        val publisher = AndroidMessageNotificationPublisher(context, authorize)
        val firstUseCase = ReceiveMessageNotificationUseCase(
            authorize = authorize,
            visibility = VisibleConversationStore(),
            availability = publisher,
            events = firstStore,
            publisher = publisher,
        )
        val firstReceiver = ConsumerMessageReceiver(MessageNotificationDecoder(), firstUseCase)

        val payload = mapOf(
            "version" to "1",
            "event_id" to "message:300:17",
            "type" to "conversation.message.created",
            "resource_type" to "conversation",
            "destination" to "conversation",
            "resource_id" to "42",
            "recipient_user_id" to "17",
            "recipient_app" to "consumer",
            "installation_id" to binding.identity.id,
            "binding_id" to binding.id,
            "title" to "Nuevo mensaje",
            "body" to "Tenés un nuevo mensaje en LoResuelvo.",
            "expires_at" to "2099-01-01T00:00:00Z",
        )

        val firstOutcome = firstReceiver.receive(payload)
        assertEquals(MessageNotificationOutcome.Published, firstOutcome)

        // Create new store and receiver instance pointing to same preferences
        val secondStore = StoredNotificationEvents(eventPrefs)
        val secondUseCase = ReceiveMessageNotificationUseCase(
            authorize = authorize,
            visibility = VisibleConversationStore(),
            availability = publisher,
            events = secondStore,
            publisher = publisher,
        )
        val secondReceiver = ConsumerMessageReceiver(MessageNotificationDecoder(), secondUseCase)

        val secondOutcome = secondReceiver.receive(payload)
        assertEquals("Recreated receiver must recognize persisted event as duplicate", MessageNotificationOutcome.Duplicate, secondOutcome)
        val active = manager.activeNotifications.filter { it.tag == "${binding.id}:message:300:17" }
        assertEquals(1, active.size)
    }
}
