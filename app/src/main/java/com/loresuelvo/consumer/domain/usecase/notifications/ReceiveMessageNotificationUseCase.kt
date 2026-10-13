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
        return synchronized(visibility) {
            if (events.contains(notification.bindingId, notification.eventId)) {
                return@synchronized MessageNotificationOutcome.Duplicate
            }
            if (!authorize(notification)) return@synchronized MessageNotificationOutcome.Invalid
            if (
                visibility.requestRefreshIfVisible(
                    notification.conversationId,
                    notification.eventId,
                    notification.recipientId,
                )
            ) {
                events.remember(notification.bindingId, notification.eventId)
                return@synchronized MessageNotificationOutcome.VisibleConversation
            }
            if (!availability.messagesAllowed()) return@synchronized MessageNotificationOutcome.Unavailable
            if (!publisher.publish(notification)) return@synchronized MessageNotificationOutcome.Unavailable
            events.remember(notification.bindingId, notification.eventId)
            MessageNotificationOutcome.Published
        }
    }

    private fun safeMessageText(notification: MessageNotification): Boolean =
        (notification.title == "Nuevo mensaje" && notification.body == "Tenés un nuevo mensaje en LoResuelvo.") ||
            (notification.title == "New message" && notification.body == "You have a new message in LoResuelvo.")
}
