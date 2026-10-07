package com.loresuelvo.consumer.domain.usecase.notifications

import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.notifications.MessageNotification
import com.loresuelvo.consumer.domain.notifications.NotificationClock
import com.loresuelvo.consumer.domain.notifications.NotificationInstallationReader

class AuthorizeMessageNotificationUseCase(
    private val sessions: AuthSessionStore,
    private val installations: NotificationInstallationReader,
    private val clock: NotificationClock,
) {
    operator fun invoke(notification: MessageNotification): Boolean {
        if (!validDestination(notification)) return false
        if (notification.expiresAt <= clock.now()) return false
        val session = sessions.getSession() ?: return false
        val installation = installations.confirmedInstallation() ?: return false
        return session.user.backendUserId == notification.recipientId &&
            installation.userId == notification.recipientId &&
            installation.installationId == notification.installationId &&
            installation.bindingId == notification.bindingId
    }

    private fun validDestination(notification: MessageNotification): Boolean =
        notification.version == "1" && notification.type == "conversation.message.created" &&
            notification.resourceType == "conversation" && notification.destination == "conversation" &&
            notification.app == "consumer" && notification.conversationId > 0 && notification.recipientId > 0 &&
            Regex("message:[1-9][0-9]*:${notification.recipientId}").matches(notification.eventId)

}
