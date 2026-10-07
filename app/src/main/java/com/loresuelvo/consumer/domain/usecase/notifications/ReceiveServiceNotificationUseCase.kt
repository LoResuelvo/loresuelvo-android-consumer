package com.loresuelvo.consumer.domain.usecase.notifications

import com.loresuelvo.consumer.domain.notifications.NotificationEventStore
import com.loresuelvo.consumer.domain.notifications.ServiceNotification
import com.loresuelvo.consumer.domain.notifications.ServiceNotificationAvailability
import com.loresuelvo.consumer.domain.notifications.ServiceNotificationOutcome
import com.loresuelvo.consumer.domain.notifications.ServiceNotificationPublisher

class ReceiveServiceNotificationUseCase(
    private val authorize: AuthorizeServiceNotificationUseCase,
    private val availability: ServiceNotificationAvailability,
    private val events: NotificationEventStore,
    private val publisher: ServiceNotificationPublisher,
) {
    @Synchronized
    operator fun invoke(notification: ServiceNotification): ServiceNotificationOutcome {
        if (!authorize(notification) || !safeServiceText(notification)) return ServiceNotificationOutcome.Invalid
        if (!availability.servicesAllowed()) return ServiceNotificationOutcome.Unavailable
        if (events.contains(notification.bindingId, notification.eventId)) return ServiceNotificationOutcome.Duplicate
        if (!publisher.publish(notification)) return ServiceNotificationOutcome.Unavailable
        events.remember(notification.bindingId, notification.eventId)
        return ServiceNotificationOutcome.Published
    }

    private fun safeServiceText(notification: ServiceNotification): Boolean = when (notification.type) {
        AuthorizeServiceNotificationUseCase.TYPE_PROPOSAL_RECEIVED ->
            (notification.title == "Nueva propuesta" && notification.body == "Recibiste una propuesta de servicio.") ||
                (notification.title == "New proposal" && notification.body == "You received a service proposal.")
        AuthorizeServiceNotificationUseCase.TYPE_REMINDER ->
            (notification.title == "Turno próximo" && notification.body == "Tenés un servicio programado dentro de las próximas 24 horas.") ||
                (notification.title == "Upcoming appointment" && notification.body == "You have a service scheduled within the next 24 hours.")
        AuthorizeServiceNotificationUseCase.TYPE_COMPLETION ->
            (notification.title == "Trabajo finalizado" && notification.body == "El prestador informó la finalización. Revisá el detalle del servicio.") ||
                (notification.title == "Work completed" && notification.body == "The provider reported completion. Review the service details.")
        else -> false
    }
}
