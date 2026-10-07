package com.loresuelvo.consumer.domain.usecase.notifications

import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.notifications.NotificationClock
import com.loresuelvo.consumer.domain.notifications.NotificationInstallationReader
import com.loresuelvo.consumer.domain.notifications.ServiceNotification

class AuthorizeServiceNotificationUseCase(
    private val sessions: AuthSessionStore,
    private val installations: NotificationInstallationReader,
    private val clock: NotificationClock,
) {
    operator fun invoke(notification: ServiceNotification): Boolean {
        if (!validDestination(notification)) return false
        if (notification.expiresAt <= clock.now()) return false
        val session = sessions.getSession() ?: return false
        val installation = installations.confirmedInstallation() ?: return false
        return session.user.backendUserId == notification.recipientId &&
            installation.userId == notification.recipientId &&
            installation.installationId == notification.installationId &&
            installation.bindingId == notification.bindingId
    }

    private fun validDestination(notification: ServiceNotification): Boolean {
        if (notification.version != "1" || notification.app != "consumer" || notification.recipientId <= 0) return false
        if (notification.resourceId.isBlank()) return false
        if (FORBIDDEN_TYPES.contains(notification.type)) return false

        val validEventIdPattern = Regex("^notification:(service_proposal_received|work_order_close_to_scheduled_time|work_order_completion_reported):[1-9][0-9]*$")
        if (!validEventIdPattern.matches(notification.eventId)) return false

        return when (notification.type) {
            TYPE_PROPOSAL_RECEIVED ->
                notification.resourceType == DESTINATION_PROPOSAL &&
                    notification.destination == DESTINATION_PROPOSAL
            TYPE_REMINDER, TYPE_COMPLETION ->
                notification.resourceType == DESTINATION_WORK_ORDER &&
                    notification.destination == DESTINATION_WORK_ORDER
            else -> false
        }
    }

    companion object {
        const val TYPE_PROPOSAL_RECEIVED = "service_proposal_received"
        const val TYPE_REMINDER = "work_order_close_to_scheduled_time"
        const val TYPE_COMPLETION = "work_order_completion_reported"

        const val DESTINATION_PROPOSAL = "service_proposal"
        const val DESTINATION_WORK_ORDER = "work_order"

        val FORBIDDEN_TYPES = setOf(
            "service_proposal_accepted",
            "work_order_final_payment_approved",
        )
    }
}
