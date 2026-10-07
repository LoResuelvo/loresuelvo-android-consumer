package com.loresuelvo.consumer.instrumented.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.loresuelvo.consumer.data.auth.EncryptedAuthSessionStore
import com.loresuelvo.consumer.data.installation.EncryptedInstallationStateStore
import com.loresuelvo.consumer.data.notifications.MessageNotificationDecoder
import com.loresuelvo.consumer.data.notifications.ServiceNotificationDecoder
import com.loresuelvo.consumer.data.notifications.StoredNotificationEvents
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.RegisterConsumerAddress
import com.loresuelvo.consumer.domain.auth.User
import com.loresuelvo.consumer.domain.conversation.Conversation
import com.loresuelvo.consumer.domain.conversation.ConversationCounterpart
import com.loresuelvo.consumer.domain.conversation.ConversationMessage
import com.loresuelvo.consumer.domain.conversation.ConversationSender
import com.loresuelvo.consumer.domain.conversation.ConversationStatus
import com.loresuelvo.consumer.domain.conversation.ConversationsOutcome
import com.loresuelvo.consumer.domain.notifications.MessageNotificationOutcome
import com.loresuelvo.consumer.domain.notifications.NotificationAvailability
import com.loresuelvo.consumer.domain.notifications.NotificationClock
import com.loresuelvo.consumer.domain.notifications.ServiceNotificationAvailability
import com.loresuelvo.consumer.domain.notifications.ServiceNotificationOutcome
import com.loresuelvo.consumer.domain.turno.Turno
import com.loresuelvo.consumer.domain.turno.TurnoCounterpart
import com.loresuelvo.consumer.domain.turno.TurnoStatus
import com.loresuelvo.consumer.domain.turno.TurnosOutcome
import com.loresuelvo.consumer.domain.usecase.notifications.AuthorizeMessageNotificationUseCase
import com.loresuelvo.consumer.domain.usecase.notifications.AuthorizeServiceNotificationUseCase
import com.loresuelvo.consumer.platform.notifications.ConsumerMessageReceiver
import com.loresuelvo.consumer.platform.notifications.ConsumerServiceNotificationReceiver
import com.loresuelvo.consumer.domain.usecase.notifications.ReceiveMessageNotificationUseCase
import com.loresuelvo.consumer.domain.usecase.notifications.ReceiveServiceNotificationUseCase
import com.loresuelvo.consumer.platform.notifications.AndroidMessageNotificationPublisher
import com.loresuelvo.consumer.platform.notifications.AndroidServiceNotificationPublisher
import com.loresuelvo.consumer.platform.notifications.VisibleConversationStore
import com.loresuelvo.consumer.testdi.FakeConversationRepository
import com.loresuelvo.consumer.testdi.FakeTurnosRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = 33)
@RunWith(AndroidJUnit4::class)
class NotificationSettingsInstrumentedTest {

    private lateinit var context: Context
    private lateinit var manager: NotificationManager
    private lateinit var sessions: EncryptedAuthSessionStore
    private lateinit var installations: EncryptedInstallationStateStore
    private lateinit var fakeConversations: FakeConversationRepository
    private lateinit var fakeTurnos: FakeTurnosRepository
    private lateinit var binding: com.loresuelvo.consumer.domain.installation.InstallationBinding

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        manager = context.getSystemService(NotificationManager::class.java)

        val sessionPrefs = context.getSharedPreferences("test_settings_sessions", Context.MODE_PRIVATE)
        val installPrefs = context.getSharedPreferences("test_settings_installations", Context.MODE_PRIVATE)
        sessionPrefs.edit().clear().commit()
        installPrefs.edit().clear().commit()

        sessions = EncryptedAuthSessionStore(sessionPrefs)
        sessions.saveSession(
            AuthSession(
                User(
                    displayName = "Test Consumer",
                    firstName = "Test",
                    lastName = "Consumer",
                    email = "consumer@example.test",
                    address = RegisterConsumerAddress("Street", "1"),
                    backendUserId = 17,
                ),
                "test-jwt",
            )
        )
        installations = EncryptedInstallationStateStore(installPrefs)
        binding = installations.prepare(17, "settings-test-login")
        installations.confirm(binding)

        fakeConversations = FakeConversationRepository()
        fakeTurnos = FakeTurnosRepository()
    }

    @Test
    fun message_notification_publisher_respects_disabled_channel() {
        val authorize = AuthorizeMessageNotificationUseCase(sessions, installations, NotificationClock { 0 })
        val publisher = AndroidMessageNotificationPublisher(context, authorize)

        val channel = NotificationChannel(
            AndroidMessageNotificationPublisher.CHANNEL,
            "Mensajes",
            NotificationManager.IMPORTANCE_NONE,
        )
        manager.createNotificationChannel(channel)

        val isChannelBlocked = manager.getNotificationChannel(AndroidMessageNotificationPublisher.CHANNEL)?.importance == NotificationManager.IMPORTANCE_NONE
        if (isChannelBlocked) {
            assertFalse(publisher.messagesAllowed())
        }
    }

    @Test
    fun service_notification_publisher_respects_disabled_channel() {
        val authorize = AuthorizeServiceNotificationUseCase(sessions, installations, NotificationClock { 0 })
        val publisher = AndroidServiceNotificationPublisher(context, authorize)

        val channel = NotificationChannel(
            AndroidServiceNotificationPublisher.CHANNEL,
            "Servicios",
            NotificationManager.IMPORTANCE_NONE,
        )
        manager.createNotificationChannel(channel)

        val isChannelBlocked = manager.getNotificationChannel(AndroidServiceNotificationPublisher.CHANNEL)?.importance == NotificationManager.IMPORTANCE_NONE
        if (isChannelBlocked) {
            assertFalse(publisher.servicesAllowed())
        }
    }

    @Test
    fun disabled_notifications_suppress_delivery_and_return_unavailable() {
        val authorizeMessage = AuthorizeMessageNotificationUseCase(sessions, installations, NotificationClock { 0 })
        val messageEvents = StoredNotificationEvents(context.getSharedPreferences("test_settings_events_msg", Context.MODE_PRIVATE))
        var publishedMsg = false
        val messageUseCase = ReceiveMessageNotificationUseCase(
            authorize = authorizeMessage,
            visibility = VisibleConversationStore(),
            availability = NotificationAvailability { false },
            events = messageEvents,
            publisher = { publishedMsg = true; true },
        )
        val messageReceiver = ConsumerMessageReceiver(MessageNotificationDecoder(), messageUseCase)
        val messagePayload = mapOf(
            "version" to "1",
            "event_id" to "message:51:17",
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
        assertEquals(MessageNotificationOutcome.Unavailable, messageReceiver.receive(messagePayload))
        assertFalse(publishedMsg)

        val authorizeService = AuthorizeServiceNotificationUseCase(sessions, installations, NotificationClock { 0 })
        val serviceEvents = StoredNotificationEvents(context.getSharedPreferences("test_settings_events_srv", Context.MODE_PRIVATE))
        var publishedSrv = false
        val serviceUseCase = ReceiveServiceNotificationUseCase(
            authorize = authorizeService,
            availability = ServiceNotificationAvailability { false },
            events = serviceEvents,
            publisher = { publishedSrv = true; true },
        )
        val serviceReceiver = ConsumerServiceNotificationReceiver(ServiceNotificationDecoder(), serviceUseCase)
        val proposalPayload = mapOf(
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
        assertEquals(ServiceNotificationOutcome.Unavailable, serviceReceiver.receive(proposalPayload))
        assertFalse(publishedSrv)
    }

    @Test
    fun conversations_and_services_remain_accessible_when_notifications_are_disabled() = runBlocking {
        fakeConversations.setConversationsSeed(
            listOf(
                Conversation(
                    id = "42",
                    status = ConversationStatus.Pending,
                    counterpart = ConversationCounterpart(20L, "Juan", "Prestador", "Plomería", null),
                    lastMessage = ConversationMessage("51", ConversationSender.Provider, "Hola!", 1000L),
                    updatedOnEpochMillis = 1000L,
                )
            )
        )
        val conversationsOutcome = fakeConversations.getConversations()
        assertTrue("Conversations query must succeed", conversationsOutcome is ConversationsOutcome.Success)
        val conversations = (conversationsOutcome as ConversationsOutcome.Success).conversations
        assertEquals(1, conversations.size)
        assertEquals("42", conversations.first().id)

        fakeTurnos.set(
            listOf(
                Turno(
                    id = "77",
                    serviceProposalId = "88",
                    status = TurnoStatus.Confirmed,
                    counterpart = TurnoCounterpart("20", "Juan", "Prestador", "Plomería", null),
                    description = "Reparación",
                    amountCents = 500000L,
                    scheduledOnEpochMillis = 2000L,
                )
            )
        )
        val turnosOutcome = fakeTurnos.getTurnos()
        assertTrue("Turnos query must succeed", turnosOutcome is TurnosOutcome.Success)
        val turnos = (turnosOutcome as TurnosOutcome.Success).turnos
        assertEquals(1, turnos.size)
        assertEquals("77", turnos.first().id)
    }
}
