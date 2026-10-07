package com.loresuelvo.consumer.domain.usecase.notifications

import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.auth.User
import com.loresuelvo.consumer.domain.notifications.NotificationClock
import com.loresuelvo.consumer.domain.notifications.NotificationInstallation
import com.loresuelvo.consumer.domain.notifications.NotificationInstallationReader
import com.loresuelvo.consumer.domain.notifications.ServiceNotification
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthorizeServiceNotificationUseCaseTest {
    private val sessions: AuthSessionStore = mockk()
    private val installations: NotificationInstallationReader = mockk()
    private val clock = NotificationClock { 1000 }
    private lateinit var useCase: AuthorizeServiceNotificationUseCase

    private val baseNotification = ServiceNotification(
        version = "1",
        eventId = "notification:service_proposal_received:10",
        type = "service_proposal_received",
        resourceType = "service_proposal",
        destination = "service_proposal",
        resourceId = "42",
        recipientId = 7,
        app = "consumer",
        installationId = "inst-uuid-1",
        bindingId = "bind-uuid-1",
        title = "Nueva propuesta",
        body = "Recibiste una propuesta de servicio.",
        expiresAt = 2000,
    )

    @Before
    fun setup() {
        useCase = AuthorizeServiceNotificationUseCase(sessions, installations, clock)
        every { sessions.getSession() } returns AuthSession(User("Test User", backendUserId = 7), "jwt")
        every { installations.confirmedInstallation() } returns NotificationInstallation("inst-uuid-1", "bind-uuid-1", 7)
    }

    @Test
    fun valid_service_proposal_is_authorized() {
        assertTrue(useCase(baseNotification))
    }

    @Test
    fun valid_reminder_and_completion_are_authorized() {
        val reminder = baseNotification.copy(
            eventId = "notification:work_order_close_to_scheduled_time:11",
            type = "work_order_close_to_scheduled_time",
            resourceType = "work_order",
            destination = "work_order",
        )
        val completion = baseNotification.copy(
            eventId = "notification:work_order_completion_reported:12",
            type = "work_order_completion_reported",
            resourceType = "work_order",
            destination = "work_order",
        )
        assertTrue(useCase(reminder))
        assertTrue(useCase(completion))
    }

    @Test
    fun provider_events_are_strictly_rejected() {
        val acceptedProposal = baseNotification.copy(
            eventId = "notification:service_proposal_accepted:13",
            type = "service_proposal_accepted",
            resourceType = "work_order",
            destination = "work_order",
        )
        val finalPayment = baseNotification.copy(
            eventId = "notification:work_order_final_payment_approved:14",
            type = "work_order_final_payment_approved",
            resourceType = "work_order",
            destination = "work_order",
        )
        assertFalse(useCase(acceptedProposal))
        assertFalse(useCase(finalPayment))
    }

    @Test
    fun mismatched_recipient_or_binding_is_rejected() {
        assertFalse(useCase(baseNotification.copy(recipientId = 99)))
        assertFalse(useCase(baseNotification.copy(bindingId = "other-binding")))
        assertFalse(useCase(baseNotification.copy(installationId = "other-inst")))
    }

    @Test
    fun expired_notification_is_rejected() {
        assertFalse(useCase(baseNotification.copy(expiresAt = 999)))
    }

    @Test
    fun invalid_app_or_version_is_rejected() {
        assertFalse(useCase(baseNotification.copy(app = "provider")))
        assertFalse(useCase(baseNotification.copy(version = "2")))
    }
}
