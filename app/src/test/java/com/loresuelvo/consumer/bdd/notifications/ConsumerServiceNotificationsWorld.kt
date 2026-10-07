package com.loresuelvo.consumer.bdd.notifications

import android.content.SharedPreferences
import com.google.firebase.messaging.RemoteMessage
import com.loresuelvo.consumer.data.auth.EncryptedAuthSessionStore
import com.loresuelvo.consumer.data.installation.EncryptedInstallationStateStore
import com.loresuelvo.consumer.data.notifications.ServiceNotificationDecoder
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

class ConsumerServiceNotificationsWorld {
    private val published = mutableListOf<ServiceNotification>()
    private val sessionPreferences = preferences()
    private val installationPreferences = preferences()
    private lateinit var sessions: EncryptedAuthSessionStore
    private lateinit var installations: EncryptedInstallationStateStore
    private lateinit var binding: com.loresuelvo.consumer.domain.installation.InstallationBinding
    private lateinit var service: ConsumerFirebaseMessagingService
    private lateinit var authorize: AuthorizeServiceNotificationUseCase
    private var currentPayload: Map<String, String>? = null

    fun start() {
        published.clear()
        sessions = EncryptedAuthSessionStore(sessionPreferences)
        sessions.saveSession(
            AuthSession(
                User("Private consumer", email = "private@example.test", backendUserId = 17),
                "private-jwt",
            )
        )
        installations = EncryptedInstallationStateStore(installationPreferences)
        binding = installations.prepare(17, "service-login")
        installations.confirm(binding)
        createService()
    }

    fun notUsingLoResuelvo() {
        // App is not in foreground/visible when receiving external service notice
    }

    private fun createService() {
        authorize = AuthorizeServiceNotificationUseCase(sessions, installations, NotificationClock { 0 })
        val receive = ReceiveServiceNotificationUseCase(
            authorize,
            ServiceNotificationAvailability { true },
            StoredNotificationEvents(installationPreferences),
            ServiceNotificationPublisher { published += it; true },
        )
        val receiver = ConsumerServiceNotificationReceiver(ServiceNotificationDecoder(), receive)
        service = ConsumerFirebaseMessagingService().apply {
            this.receiver = Provider { mockk(relaxed = true) }
            this.serviceReceiver = Provider { receiver }
        }
    }

    fun receive(novelty: String) {
        val payload = when (novelty) {
            "una propuesta recibida del prestador" -> mapOf(
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
            "un turno dentro de las próximas 24 horas" -> mapOf(
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
            "la finalización reportada por prestador" -> mapOf(
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
            else -> error("Unknown novelty: $novelty")
        }
        currentPayload = payload
        val message = mockk<RemoteMessage>()
        every { message.data } returns payload
        service.onMessageReceived(message)
    }

    fun assertPublished(notice: String, destination: String) {
        assertEquals("A valid service update must publish one system notification", 1, published.size)
        val notification = published.single()
        assertTrue(authorize(notification))

        when (notice) {
            "propuesta recibida" -> {
                assertEquals("Nueva propuesta", notification.title)
                assertEquals("Recibiste una propuesta de servicio.", notification.body)
                assertEquals("service_proposal", notification.destination)
                assertEquals("la propuesta", destination)
                assertEquals("88", notification.resourceId)
            }
            "turno próximo" -> {
                assertEquals("Turno próximo", notification.title)
                assertEquals("Tenés un servicio programado dentro de las próximas 24 horas.", notification.body)
                assertEquals("work_order", notification.destination)
                assertEquals("la orden del turno", destination)
                assertEquals("77", notification.resourceId)
            }
            "servicio finalizado" -> {
                assertEquals("Trabajo finalizado", notification.title)
                assertEquals("El prestador informó la finalización. Revisá el detalle del servicio.", notification.body)
                assertEquals("work_order", notification.destination)
                assertEquals("la orden correspondiente", destination)
                assertEquals("77", notification.resourceId)
            }
            else -> error("Unknown notice: $notice")
        }
    }

    fun assertPrivate() {
        val notification = published.single()
        val payload = currentPayload ?: error("No current payload")
        assertEquals(payload["title"], notification.title)
        assertEquals(payload["body"], notification.body)
        assertFalse(notification.title.contains("Private consumer"))
        assertFalse(notification.body.contains("private@example.test"))
        assertFalse(notification.title.contains("Av."))
        assertFalse(notification.body.contains("Calle"))
        assertFalse(notification.body.contains("$"))
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
