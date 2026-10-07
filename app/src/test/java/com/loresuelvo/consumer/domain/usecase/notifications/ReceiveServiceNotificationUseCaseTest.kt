package com.loresuelvo.consumer.domain.usecase.notifications

import com.loresuelvo.consumer.domain.notifications.NotificationEventStore
import com.loresuelvo.consumer.domain.notifications.ServiceNotification
import com.loresuelvo.consumer.domain.notifications.ServiceNotificationAvailability
import com.loresuelvo.consumer.domain.notifications.ServiceNotificationOutcome
import com.loresuelvo.consumer.domain.notifications.ServiceNotificationPublisher
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ReceiveServiceNotificationUseCaseTest {
    private val authorize: AuthorizeServiceNotificationUseCase = mockk()
    private val availability: ServiceNotificationAvailability = mockk()
    private val events: NotificationEventStore = mockk()
    private val publisher: ServiceNotificationPublisher = mockk()
    private lateinit var useCase: ReceiveServiceNotificationUseCase

    private val notification = ServiceNotification(
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
        useCase = ReceiveServiceNotificationUseCase(authorize, availability, events, publisher)
        every { authorize(any()) } returns true
        every { availability.servicesAllowed() } returns true
        every { events.contains(any(), any()) } returns false
        every { events.remember(any(), any()) } returns true
        every { publisher.publish(any()) } returns true
    }

    @Test
    fun valid_notification_is_published_and_stored() {
        val outcome = useCase(notification)
        assertEquals(ServiceNotificationOutcome.Published, outcome)
    }

    @Test
    fun unsafe_body_with_pii_is_rejected_as_invalid() {
        val unsafe = notification.copy(body = "Oferta de $5000 por Carlos en Av. Santa Fe")
        val outcome = useCase(unsafe)
        assertEquals(ServiceNotificationOutcome.Invalid, outcome)
    }

    @Test
    fun duplicate_notification_is_reported_as_duplicate() {
        every { events.contains("bind-uuid-1", "notification:service_proposal_received:10") } returns true
        val outcome = useCase(notification)
        assertEquals(ServiceNotificationOutcome.Duplicate, outcome)
    }

    @Test
    fun notifications_disabled_returns_unavailable() {
        every { availability.servicesAllowed() } returns false
        val outcome = useCase(notification)
        assertEquals(ServiceNotificationOutcome.Unavailable, outcome)
    }
}
