package com.loresuelvo.consumer.bdd.notifications

import android.content.SharedPreferences
import com.loresuelvo.consumer.data.auth.EncryptedAuthSessionStore
import com.loresuelvo.consumer.data.installation.EncryptedInstallationStateStore
import com.loresuelvo.consumer.data.notifications.MessageNotificationDecoder
import com.loresuelvo.consumer.data.notifications.ServiceNotificationDecoder
import com.loresuelvo.consumer.data.notifications.StoredNotificationEvents
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.User
import com.loresuelvo.consumer.domain.installation.InstallationBinding
import com.loresuelvo.consumer.domain.notifications.MessageNotification
import com.loresuelvo.consumer.domain.notifications.MessageNotificationOutcome
import com.loresuelvo.consumer.domain.notifications.NotificationAvailability
import com.loresuelvo.consumer.domain.notifications.NotificationClock
import com.loresuelvo.consumer.domain.notifications.ServiceNotification
import com.loresuelvo.consumer.domain.notifications.ServiceNotificationAvailability
import com.loresuelvo.consumer.domain.notifications.ServiceNotificationOutcome
import com.loresuelvo.consumer.domain.usecase.notifications.AuthorizeMessageNotificationUseCase
import com.loresuelvo.consumer.domain.usecase.notifications.AuthorizeServiceNotificationUseCase
import com.loresuelvo.consumer.domain.usecase.notifications.ReceiveMessageNotificationUseCase
import com.loresuelvo.consumer.domain.usecase.notifications.ReceiveServiceNotificationUseCase
import com.loresuelvo.consumer.platform.notifications.ConsumerMessageReceiver
import com.loresuelvo.consumer.platform.notifications.ConsumerServiceNotificationReceiver
import com.loresuelvo.consumer.platform.notifications.VisibleConversationStore
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class ConsumerDeduplicationNotificationsWorld {
    private val sessionPreferences = fakePreferences()
    private val installationPreferences = fakePreferences()

    private lateinit var sessions: EncryptedAuthSessionStore
    private lateinit var installations: EncryptedInstallationStateStore
    private lateinit var binding: InstallationBinding
    private lateinit var events: StoredNotificationEvents

    private var controlledNowEpochMillis = 1_000_000L
    private var publishedMessages = 0
    private var publishedServices = 0
    private var lastMessageOutcome: MessageNotificationOutcome? = null
    private var lastServiceOutcome: ServiceNotificationOutcome? = null
    private var lastNoticeCategory: String = ""

    fun start() {
        controlledNowEpochMillis = 1_000_000L
        publishedMessages = 0
        publishedServices = 0
        lastMessageOutcome = null
        lastServiceOutcome = null
        lastNoticeCategory = ""

        sessions = EncryptedAuthSessionStore(sessionPreferences)
        sessions.saveSession(
            AuthSession(
                User("Active Consumer", email = "consumer@example.test", backendUserId = 17),
                "active-jwt",
            )
        )
        installations = EncryptedInstallationStateStore(installationPreferences)
        binding = installations.prepare(17, "dedup-login")
        installations.confirm(binding)

        events = StoredNotificationEvents(installationPreferences)
    }

    fun receiveNotice(notice: String) {
        lastNoticeCategory = notice
        val clock = NotificationClock { controlledNowEpochMillis }
        val authorizeMessage = AuthorizeMessageNotificationUseCase(sessions, installations, clock)
        val messageUseCase = ReceiveMessageNotificationUseCase(
            authorize = authorizeMessage,
            visibility = VisibleConversationStore(sessions),
            availability = NotificationAvailability { true },
            events = events,
            publisher = { publishedMessages++; true },
        )
        val messageReceiver = ConsumerMessageReceiver(MessageNotificationDecoder(), messageUseCase)

        when (notice) {
            "un aviso que ya recibí" -> {
                val payload = mapOf(
                    "version" to "1",
                    "event_id" to "message:100:17",
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
                val firstOutcome = messageReceiver.receive(payload)
                assertEquals("First delivery must be published", MessageNotificationOutcome.Published, firstOutcome)
                assertEquals("Published count must be 1 after initial reception", 1, publishedMessages)

                // Second delivery of the identical payload
                lastMessageOutcome = messageReceiver.receive(payload)
            }
            "un aviso cuyo plazo para mostrarse venció" -> {
                val expiredPayload = mapOf(
                    "version" to "1",
                    "event_id" to "message:101:17",
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
                lastMessageOutcome = messageReceiver.receive(expiredPayload)
            }
            else -> error("Unknown notice kind: $notice")
        }
    }

    fun assertNoNewNotificationNorResound() {
        when (lastNoticeCategory) {
            "un aviso que ya recibí" -> {
                assertEquals(
                    "Repeated notification must be flagged as duplicate",
                    MessageNotificationOutcome.Duplicate,
                    lastMessageOutcome,
                )
                assertEquals(
                    "No additional notification must be published for duplicate",
                    1,
                    publishedMessages,
                )
            }
            "un aviso cuyo plazo para mostrarse venció" -> {
                assertEquals(
                    "Expired notification must be rejected as invalid",
                    MessageNotificationOutcome.Invalid,
                    lastMessageOutcome,
                )
                assertEquals(
                    "No notification must be published for expired notice",
                    0,
                    publishedMessages,
                )
            }
            else -> error("Unknown notice kind: $lastNoticeCategory")
        }
    }

    private fun fakePreferences(): SharedPreferences {
        val values = mutableMapOf<String, Any?>()
        val prefs = mockk<SharedPreferences>()
        val editor = mockk<SharedPreferences.Editor>()
        every { prefs.getString(any(), any()) } answers { values[firstArg<String>()] as? String ?: secondArg() }
        every { prefs.getInt(any(), any()) } answers { values[firstArg<String>()] as? Int ?: secondArg() }
        every { prefs.getLong(any(), any()) } answers { values[firstArg<String>()] as? Long ?: secondArg() }
        every { prefs.getBoolean(any(), any()) } answers { values[firstArg<String>()] as? Boolean ?: secondArg() }
        every { prefs.contains(any()) } answers { values.containsKey(firstArg<String>()) }
        every { prefs.edit() } returns editor
        every { editor.putString(any(), any()) } answers { values[firstArg<String>()] = secondArg<String?>(); editor }
        every { editor.putInt(any(), any()) } answers { values[firstArg<String>()] = secondArg<Int>(); editor }
        every { editor.putLong(any(), any()) } answers { values[firstArg<String>()] = secondArg<Long>(); editor }
        every { editor.putBoolean(any(), any()) } answers { values[firstArg<String>()] = secondArg<Boolean>(); editor }
        every { editor.remove(any()) } answers { values.remove(firstArg<String>()); editor }
        every { editor.clear() } answers { values.clear(); editor }
        every { editor.apply() } returns Unit
        every { editor.commit() } returns true
        return prefs
    }
}
