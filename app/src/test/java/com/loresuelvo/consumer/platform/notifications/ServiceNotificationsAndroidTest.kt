package com.loresuelvo.consumer.platform.notifications

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.loresuelvo.consumer.data.api.mapper.toDomain
import com.loresuelvo.consumer.data.auth.EncryptedAuthSessionStore
import com.loresuelvo.consumer.data.installation.EncryptedInstallationStateStore
import com.loresuelvo.consumer.data.notifications.ServiceNotificationDecoder
import com.loresuelvo.consumer.data.notifications.StoredNotificationEvents
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.User
import com.loresuelvo.consumer.domain.notifications.*
import com.loresuelvo.consumer.domain.usecase.notifications.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [24, 34])
class ServiceNotificationsAndroidTest {
    private lateinit var context: Context
    private lateinit var sessions: EncryptedAuthSessionStore
    private lateinit var installations: EncryptedInstallationStateStore
    private lateinit var receiver: ConsumerServiceNotificationReceiver
    private lateinit var authorize: AuthorizeServiceNotificationUseCase
    private lateinit var payload: Map<String, String>
    private lateinit var manager: NotificationManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val sessionPrefs = context.getSharedPreferences("service_sessions", 0)
        val installationPrefs = context.getSharedPreferences("service_installations", 0)
        sessionPrefs.edit().clear().commit()
        installationPrefs.edit().clear().commit()
        sessions = EncryptedAuthSessionStore(sessionPrefs)
        sessions.saveSession(AuthSession(User("Private name", backendUserId = 17), "private-jwt"))
        installations = EncryptedInstallationStateStore(installationPrefs)
        val binding = installations.prepare(17, "login")
        installations.confirm(binding)
        authorize = AuthorizeServiceNotificationUseCase(sessions, installations, NotificationClock { 1000 })
        val publisher = AndroidServiceNotificationPublisher(context, authorize)
        receiver = ConsumerServiceNotificationReceiver(
            ServiceNotificationDecoder(),
            ReceiveServiceNotificationUseCase(
                authorize,
                publisher,
                StoredNotificationEvents(installationPrefs),
                publisher,
            )
        )
        payload = mapOf(
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
        manager = context.getSystemService(NotificationManager::class.java)
        shadowOf(manager).setNotificationsEnabled(true)
    }

    @Test
    fun publishes_private_notification_and_immutable_explicit_service_intent() {
        assertEquals(ServiceNotificationOutcome.Published, receiver.receive(payload))
        val notification = shadowOf(manager).allNotifications.single()
        assertEquals(Notification.VISIBILITY_PRIVATE, notification.visibility)
        assertNotNull(notification.publicVersion)
        assertNotEquals(0, notification.smallIcon.resId)
        val pending = shadowOf(notification.contentIntent)
        val intent = pending.savedIntent
        assertTrue(pending.flags and android.app.PendingIntent.FLAG_IMMUTABLE != 0)
        assertEquals("com.loresuelvo.consumer.MainActivity", intent.component?.className)
        val target = ServiceNotificationNavigation(authorize).target(intent)
        assertNotNull(target)
        assertEquals("service_proposal", target?.destination)
        assertEquals("88", target?.resourceId)
        assertFalse(intent.extras.toString().contains("private-jwt"))
        sessions.clearSession()
        assertNull(ServiceNotificationNavigation(authorize).target(intent))
    }

    @Test
    fun appointment_reminder_and_completion_notices_publish_with_correct_targets() {
        val reminderPayload = payload + mapOf(
            "event_id" to "notification:work_order_close_to_scheduled_time:102",
            "type" to "work_order_close_to_scheduled_time",
            "resource_type" to "work_order",
            "destination" to "work_order",
            "resource_id" to "77",
            "title" to "Turno próximo",
            "body" to "Tenés un servicio programado dentro de las próximas 24 horas.",
        )
        assertEquals(ServiceNotificationOutcome.Published, receiver.receive(reminderPayload))
        val reminderIntent = shadowOf(shadowOf(manager).allNotifications.last().contentIntent).savedIntent
        val reminderTarget = ServiceNotificationNavigation(authorize).target(reminderIntent)
        assertEquals("work_order", reminderTarget?.destination)
        assertEquals("77", reminderTarget?.resourceId)

        val completionPayload = payload + mapOf(
            "event_id" to "notification:work_order_completion_reported:103",
            "type" to "work_order_completion_reported",
            "resource_type" to "work_order",
            "destination" to "work_order",
            "resource_id" to "77",
            "title" to "Trabajo finalizado",
            "body" to "El prestador informó la finalización. Revisá el detalle del servicio.",
        )
        assertEquals(ServiceNotificationOutcome.Published, receiver.receive(completionPayload))
        val completionIntent = shadowOf(shadowOf(manager).allNotifications.last().contentIntent).savedIntent
        val completionTarget = ServiceNotificationNavigation(authorize).target(completionIntent)
        assertEquals("work_order", completionTarget?.destination)
        assertEquals("77", completionTarget?.resourceId)
    }

    @Test
    fun provider_events_and_invalid_payloads_never_publish() {
        val providerProposalAccepted = payload + mapOf(
            "event_id" to "notification:service_proposal_accepted:104",
            "type" to "service_proposal_accepted",
            "resource_type" to "work_order",
            "destination" to "work_order",
            "title" to "Propuesta aceptada",
            "body" to "Se confirmó una contratación.",
        )
        val providerFinalPayment = payload + mapOf(
            "event_id" to "notification:work_order_final_payment_approved:105",
            "type" to "work_order_final_payment_approved",
            "resource_type" to "work_order",
            "destination" to "work_order",
            "title" to "Pago final confirmado",
            "body" to "Se aprobó el pago del saldo de tu servicio.",
        )
        assertEquals(ServiceNotificationOutcome.Invalid, receiver.receive(providerProposalAccepted))
        assertEquals(ServiceNotificationOutcome.Invalid, receiver.receive(providerFinalPayment))
        assertTrue(shadowOf(manager).allNotifications.isEmpty())
    }

    @Test
    fun deduplicated_after_receiver_recreation() {
        assertEquals(ServiceNotificationOutcome.Published, receiver.receive(payload))
        val events = StoredNotificationEvents(context.getSharedPreferences("service_installations", 0))
        val publisher = AndroidServiceNotificationPublisher(context, authorize)
        val recreated = ConsumerServiceNotificationReceiver(
            ServiceNotificationDecoder(),
            ReceiveServiceNotificationUseCase(authorize, publisher, events, publisher),
        )
        assertEquals(ServiceNotificationOutcome.Duplicate, recreated.receive(payload))
        assertEquals(1, shadowOf(manager).allNotifications.size)
    }

    @Test
    fun disabled_notifications_return_unavailable() {
        shadowOf(manager).setNotificationsEnabled(false)
        assertEquals(ServiceNotificationOutcome.Unavailable, receiver.receive(payload))
        assertTrue(shadowOf(manager).allNotifications.isEmpty())
    }
}
