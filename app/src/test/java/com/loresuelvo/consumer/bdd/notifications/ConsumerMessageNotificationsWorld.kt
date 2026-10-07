package com.loresuelvo.consumer.bdd.notifications

import android.content.SharedPreferences
import com.google.firebase.messaging.RemoteMessage
import com.loresuelvo.consumer.data.auth.EncryptedAuthSessionStore
import com.loresuelvo.consumer.data.installation.EncryptedInstallationStateStore
import com.loresuelvo.consumer.data.notifications.MessageNotificationDecoder
import com.loresuelvo.consumer.data.notifications.StoredNotificationEvents
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.User
import com.loresuelvo.consumer.domain.notifications.*
import com.loresuelvo.consumer.domain.usecase.notifications.*
import com.loresuelvo.consumer.platform.notifications.*
import io.mockk.every
import io.mockk.mockk
import javax.inject.Provider
import org.junit.Assert.*

class ConsumerMessageNotificationsWorld {
    private val published = mutableListOf<MessageNotification>()
    private val sessionPreferences = preferences()
    private val installationPreferences = preferences()
    private val visibility = VisibleConversationStore()
    private lateinit var sessions: EncryptedAuthSessionStore
    private lateinit var installations: EncryptedInstallationStateStore
    private lateinit var service: ConsumerFirebaseMessagingService
    private lateinit var payload: Map<String, String>
    private lateinit var authorize: AuthorizeMessageNotificationUseCase

    fun start() {
        sessions = EncryptedAuthSessionStore(sessionPreferences)
        sessions.saveSession(AuthSession(User("Private consumer", email = "private@example.test", backendUserId = 17), "private-jwt"))
        installations = EncryptedInstallationStateStore(installationPreferences)
        val binding = installations.prepare(17, "initial-login")
        installations.confirm(binding)
        payload = mapOf(
            "version" to "1", "event_id" to "message:51:17", "type" to "conversation.message.created",
            "resource_type" to "conversation", "destination" to "conversation", "resource_id" to "42",
            "recipient_user_id" to "17", "recipient_app" to "consumer",
            "installation_id" to binding.identity.id, "binding_id" to binding.id,
            "title" to "Nuevo mensaje", "body" to "Tenés un nuevo mensaje en LoResuelvo.",
            "expires_at" to "2099-01-01T00:00:00Z",
        )
        createService()
    }

    fun setSituation(situation: String) {
        when (situation) {
            "leyendo otra conversación" -> visibility.show(99)
            "usando otra pantalla de LoResuelvo" -> visibility.show(null)
            "usando otra aplicación", "con la pantalla bloqueada" -> visibility.show(null)
            "sin LoResuelvo en ejecución" -> {
                sessions = EncryptedAuthSessionStore(sessionPreferences)
                installations = EncryptedInstallationStateStore(installationPreferences)
                visibility.show(null)
                createService()
            }
            else -> error("Unknown scenario situation")
        }
    }

    private fun createService() {
        authorize = AuthorizeMessageNotificationUseCase(sessions, installations, NotificationClock { 0 })
        val receive = ReceiveMessageNotificationUseCase(
            authorize, visibility, NotificationAvailability { true }, StoredNotificationEvents(installationPreferences),
            MessageNotificationPublisher { published += it; true },
        )
        val receiver = ConsumerMessageReceiver(MessageNotificationDecoder(), receive)
        service = ConsumerFirebaseMessagingService().apply { this.receiver = Provider { receiver } }
    }

    fun receive(content: String) {
        assertTrue(content in listOf("texto", "fotografías", "audio", "video"))
        val message = mockk<RemoteMessage>()
        every { message.data } returns payload
        service.onMessageReceived(message)
    }

    fun assertPublished() {
        assertEquals("A valid provider message must publish one system notification", 1, published.size)
        assertEquals(42, published.single().conversationId)
        assertTrue(authorize(published.single()))
    }

    fun assertPrivate() {
        val notification = published.single()
        assertEquals(payload["title"], notification.title)
        assertEquals(payload["body"], notification.body)
        assertFalse(notification.title.contains("Private consumer"))
        assertFalse(notification.body.contains("private@example.test"))
        assertFalse(notification.toString().contains("private-jwt"))
        assertFalse(notification.toString().contains("installation_secret"))
    }

    private fun preferences(): SharedPreferences {
        val values = mutableMapOf<String, Any?>()
        val preferences = mockk<SharedPreferences>()
        val editor = mockk<SharedPreferences.Editor>()
        every { preferences.getString(any(), any()) } answers { values[firstArg<String>()] as? String ?: secondArg() }
        every { preferences.getInt(any(), any()) } answers { values[firstArg<String>()] as? Int ?: secondArg() }
        every { preferences.getBoolean(any(), any()) } answers { values[firstArg<String>()] as? Boolean ?: secondArg() }
        every { preferences.contains(any()) } answers { values.containsKey(firstArg()) }
        every { preferences.edit() } returns editor
        every { editor.putString(any(), any()) } answers { values[firstArg<String>()] = secondArg<String?>(); editor }
        every { editor.putInt(any(), any()) } answers { values[firstArg<String>()] = secondArg<Int>(); editor }
        every { editor.putBoolean(any(), any()) } answers { values[firstArg<String>()] = secondArg<Boolean>(); editor }
        every { editor.remove(any()) } answers { values.remove(firstArg<String>()); editor }
        every { editor.commit() } returns true
        return preferences
    }
}
