package com.loresuelvo.consumer.domain.usecase.notifications

import com.loresuelvo.consumer.domain.notifications.MessageNotification
import com.loresuelvo.consumer.domain.notifications.MessageNotificationOutcome
import com.loresuelvo.consumer.domain.notifications.ConversationVisibility
import com.loresuelvo.consumer.domain.notifications.NotificationAvailability
import com.loresuelvo.consumer.domain.notifications.NotificationEventStore
import com.loresuelvo.consumer.domain.notifications.MessageNotificationPublisher

class ReceiveMessageNotificationUseCase(
    private val authorize: AuthorizeMessageNotificationUseCase,
    private val visibility: ConversationVisibility,
    private val availability: NotificationAvailability,
    private val events: NotificationEventStore,
    private val publisher: MessageNotificationPublisher,
) {
    @Synchronized
    operator fun invoke(notification: MessageNotification): MessageNotificationOutcome {
        if (!authorize(notification) || !safeMessageText(notification)) return MessageNotificationOutcome.Invalid
        if (events.contains(notification.bindingId, notification.eventId)) return MessageNotificationOutcome.Duplicate
        if (!authorize(notification)) return MessageNotificationOutcome.Invalid
        if (
            visibility.requestRefreshIfVisible(
                notification.conversationId,
                notification.eventId,
                notification.recipientId,
            )
        ) {
            events.remember(notification.bindingId, notification.eventId)
            return MessageNotificationOutcome.VisibleConversation
        }
        if (!availability.messagesAllowed()) return MessageNotificationOutcome.Unavailable
        if (!publisher.publish(notification)) return MessageNotificationOutcome.Unavailable
        events.remember(notification.bindingId, notification.eventId)
        return MessageNotificationOutcome.Published
    }

    private fun safeMessageText(notification: MessageNotification): Boolean =
        (notification.title == "Nuevo mensaje" && notification.body == "Tenés un nuevo mensaje en LoResuelvo.") ||
            (notification.title == "New message" && notification.body == "You have a new message in LoResuelvo.")
}
