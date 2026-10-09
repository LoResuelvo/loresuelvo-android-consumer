package com.loresuelvo.consumer.bdd.notifications

import android.content.SharedPreferences
import com.loresuelvo.consumer.data.auth.EncryptedAuthSessionStore
import com.loresuelvo.consumer.data.installation.EncryptedInstallationStateStore
import com.loresuelvo.consumer.data.notifications.MessageNotificationDecoder
import com.loresuelvo.consumer.data.notifications.ServiceNotificationDecoder
import com.loresuelvo.consumer.data.notifications.StoredNotificationEvents
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.User
import com.loresuelvo.consumer.domain.conversation.Conversation
import com.loresuelvo.consumer.domain.conversation.ConversationCounterpart
import com.loresuelvo.consumer.domain.conversation.ConversationRepository
import com.loresuelvo.consumer.domain.conversation.ConversationStatus
import com.loresuelvo.consumer.domain.conversation.ConversationsOutcome
import com.loresuelvo.consumer.domain.installation.InstallationBinding
import com.loresuelvo.consumer.domain.notifications.MessageNotification
import com.loresuelvo.consumer.domain.notifications.MessageNotificationOutcome
import com.loresuelvo.consumer.domain.notifications.MessageNotificationPublisher
import com.loresuelvo.consumer.domain.notifications.NotificationAvailability
import com.loresuelvo.consumer.domain.notifications.NotificationClock
import com.loresuelvo.consumer.domain.notifications.ServiceNotification
import com.loresuelvo.consumer.domain.notifications.ServiceNotificationAvailability
import com.loresuelvo.consumer.domain.notifications.ServiceNotificationOutcome
import com.loresuelvo.consumer.domain.notifications.ServiceNotificationPublisher
import com.loresuelvo.consumer.domain.turno.Turno
import com.loresuelvo.consumer.domain.turno.TurnoCounterpart
import com.loresuelvo.consumer.domain.turno.TurnoStatus
import com.loresuelvo.consumer.domain.turno.TurnosOutcome
import com.loresuelvo.consumer.domain.turno.TurnosRepository
import com.loresuelvo.consumer.domain.usecase.notifications.AuthorizeMessageNotificationUseCase
import com.loresuelvo.consumer.domain.usecase.notifications.AuthorizeServiceNotificationUseCase
import com.loresuelvo.consumer.domain.usecase.notifications.ReceiveMessageNotificationUseCase
import com.loresuelvo.consumer.domain.usecase.notifications.ReceiveServiceNotificationUseCase
import com.loresuelvo.consumer.platform.notifications.ConsumerMessageReceiver
import com.loresuelvo.consumer.platform.notifications.ConsumerServiceNotificationReceiver
import com.loresuelvo.consumer.platform.notifications.VisibleConversationStore
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class ConsumerSettingsNotificationsWorld {
    private val sessionPreferences = preferences()
    private val installationPreferences = preferences()
    private lateinit var sessions: EncryptedAuthSessionStore
    private lateinit var installations: EncryptedInstallationStateStore
    private lateinit var binding: InstallationBinding
    private lateinit var conversationRepository: ConversationRepository
    private lateinit var turnosRepository: TurnosRepository

    private var appNotificationsEnabled = true
    private var messageChannelEnabled = true
    private var serviceChannelEnabled = true
    private var permissionGranted = true

    private var lastNoticeType: String = ""
    private var lastMessageOutcome: MessageNotificationOutcome? = null
    private var lastServiceOutcome: ServiceNotificationOutcome? = null
    private val publishedMessages = mutableListOf<MessageNotification>()
    private val publishedServices = mutableListOf<ServiceNotification>()

    fun start() {
        appNotificationsEnabled = true
        messageChannelEnabled = true
        serviceChannelEnabled = true
        permissionGranted = true
        lastNoticeType = ""
        lastMessageOutcome = null
        lastServiceOutcome = null
        publishedMessages.clear()
        publishedServices.clear()

        sessions = EncryptedAuthSessionStore(sessionPreferences)
        sessions.saveSession(
            AuthSession(
                User("Active consumer", email = "consumer@example.test", backendUserId = 17),
                "active-jwt",
            )
        )
        installations = EncryptedInstallationStateStore(installationPreferences)
        binding = installations.prepare(17, "login")
        installations.confirm(binding)

        val conversation = Conversation(
            id = "42",
            status = ConversationStatus.Pending,
            counterpart = ConversationCounterpart(20L, "Juan", "Prestador", "Plomería", null),
            lastMessage = null,
            updatedOnEpochMillis = 1000L,
        )
        conversationRepository = mockk {
            coEvery { getConversations() } returns ConversationsOutcome.Success(listOf(conversation))
        }

        val turno = Turno(
            id = "77",
            serviceProposalId = "88",
            status = TurnoStatus.Confirmed,
            counterpart = TurnoCounterpart("20", "Juan", "Prestador", "Plomería", null),
            description = "Reparación",
            amountCents = 500000L,
            scheduledOnEpochMillis = 2000L,
        )
        turnosRepository = mockk {
            coEvery { getTurnos() } returns TurnosOutcome.Success(listOf(turno))
        }
    }

    fun configureAdjustment(adjustment: String) {
        when (adjustment) {
            "rechacé el permiso de avisos" -> {
                permissionGranted = false
                appNotificationsEnabled = false
            }
            "deshabilité el canal de mensajes" -> {
                messageChannelEnabled = false
            }
            "deshabilité el canal de servicios" -> {
                serviceChannelEnabled = false
            }
            "deshabilité las notificaciones de la app" -> {
                appNotificationsEnabled = false
            }
            else -> error("Unknown notification adjustment: $adjustment")
        }
    }

    fun receiveValidNotice(noticeType: String) {
        lastNoticeType = noticeType
        when (noticeType) {
            "nuevo mensaje" -> {
                val payload = mapOf(
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
                val authorize = AuthorizeMessageNotificationUseCase(sessions, installations, NotificationClock { 0 })
                val availability = NotificationAvailability {
                    permissionGranted && appNotificationsEnabled && messageChannelEnabled
                }
                val publisher = MessageNotificationPublisher {
                    publishedMessages += it
                    true
                }
                val receive = ReceiveMessageNotificationUseCase(
                    authorize,
                    VisibleConversationStore(sessions),
                    availability,
                    StoredNotificationEvents(installationPreferences),
                    publisher,
                )
                val receiver = ConsumerMessageReceiver(MessageNotificationDecoder(), receive)
                lastMessageOutcome = receiver.receive(payload)
            }
            "propuesta recibida" -> {
                val payload = mapOf(
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
                val authorize = AuthorizeServiceNotificationUseCase(sessions, installations, NotificationClock { 0 })
                val availability = ServiceNotificationAvailability {
                    permissionGranted && appNotificationsEnabled && serviceChannelEnabled
                }
                val publisher = ServiceNotificationPublisher {
                    publishedServices += it
                    true
                }
                val receive = ReceiveServiceNotificationUseCase(
                    authorize,
                    availability,
                    StoredNotificationEvents(installationPreferences),
                    publisher,
                )
                val receiver = ConsumerServiceNotificationReceiver(ServiceNotificationDecoder(), receive)
                lastServiceOutcome = receiver.receive(payload)
            }
            "servicio finalizado" -> {
                val payload = mapOf(
                    "version" to "1",
                    "event_id" to "notification:work_order_completion_reported:103",
                    "type" to "work_order_completion_reported",
                    "resource_type" to "work_order",
                    "destination" to "work_order",
                    "resource_id" to "77",
                    "recipient_user_id" to "17",
                    "recipient_app" to "consumer",
                    "installation_id" to binding.identity.id,
                    "binding_id" to binding.id,
                    "title" to "Trabajo finalizado",
                    "body" to "El prestador informó la finalización. Revisá el detalle del servicio.",
                    "expires_at" to "2099-01-01T00:00:00Z",
                )
                val authorize = AuthorizeServiceNotificationUseCase(sessions, installations, NotificationClock { 0 })
                val availability = ServiceNotificationAvailability {
                    permissionGranted && appNotificationsEnabled && serviceChannelEnabled
                }
                val publisher = ServiceNotificationPublisher {
                    publishedServices += it
                    true
                }
                val receive = ReceiveServiceNotificationUseCase(
                    authorize,
                    availability,
                    StoredNotificationEvents(installationPreferences),
                    publisher,
                )
                val receiver = ConsumerServiceNotificationReceiver(ServiceNotificationDecoder(), receive)
                lastServiceOutcome = receiver.receive(payload)
            }
            else -> error("Unknown notice type: $noticeType")
        }
    }

    fun assertNoNotificationPublished() {
        assertTrue("No message notifications should be published", publishedMessages.isEmpty())
        assertTrue("No service notifications should be published", publishedServices.isEmpty())
        if (lastMessageOutcome != null) {
            assertEquals(MessageNotificationOutcome.Unavailable, lastMessageOutcome)
        }
        if (lastServiceOutcome != null) {
            assertEquals(ServiceNotificationOutcome.Unavailable, lastServiceOutcome)
        }
    }

    fun assertCanConsultNoveltyInsideApp() = runBlocking {
        when (lastNoticeType) {
            "nuevo mensaje" -> {
                val outcome = conversationRepository.getConversations()
                assertTrue("Conversations must be accessible", outcome is ConversationsOutcome.Success)
                val conversations = (outcome as ConversationsOutcome.Success).conversations
                assertTrue("Conversation with new message must be queryable", conversations.any { it.id == "42" })
            }
            "propuesta recibida", "servicio finalizado" -> {
                val outcome = turnosRepository.getTurnos()
                assertTrue("Services/turnos must be accessible", outcome is TurnosOutcome.Success)
                val turnos = (outcome as TurnosOutcome.Success).turnos
                assertTrue("Turnos must be queryable", turnos.isNotEmpty())
            }
            else -> error("Unknown notice type: $lastNoticeType")
        }
    }

    private fun preferences(): SharedPreferences {
        val values = mutableMapOf<String, Any?>()
        val preferences = mockk<SharedPreferences>()
        val editor = mockk<SharedPreferences.Editor>()
        every { preferences.getString(any(), any()) } answers { values[firstArg<String>()] as? String ?: secondArg() }
        every { preferences.getInt(any(), any()) } answers { values[firstArg<String>()] as? Int ?: secondArg() }
        every { preferences.getLong(any(), any()) } answers { values[firstArg<String>()] as? Long ?: secondArg() }
        every { preferences.getBoolean(any(), any()) } answers { values[firstArg<String>()] as? Boolean ?: secondArg() }
        every { preferences.contains(any()) } answers { values.containsKey(firstArg<String>()) }
        every { preferences.edit() } returns editor
        every { editor.putString(any(), any()) } answers { values[firstArg()] = secondArg<String?>(); editor }
        every { editor.putInt(any(), any()) } answers { values[firstArg()] = secondArg<Int>(); editor }
        every { editor.putLong(any(), any()) } answers { values[firstArg()] = secondArg<Long>(); editor }
        every { editor.putBoolean(any(), any()) } answers { values[firstArg()] = secondArg<Boolean>(); editor }
        every { editor.remove(any()) } answers { values.remove(firstArg<String>()); editor }
        every { editor.clear() } answers { values.clear(); editor }
        every { editor.apply() } returns Unit
        every { editor.commit() } returns true
        return preferences
    }
}
