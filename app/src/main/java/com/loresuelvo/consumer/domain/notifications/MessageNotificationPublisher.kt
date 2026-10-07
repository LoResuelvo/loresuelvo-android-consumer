package com.loresuelvo.consumer.domain.notifications

fun interface MessageNotificationPublisher {
    fun publish(notification: MessageNotification): Boolean
}
