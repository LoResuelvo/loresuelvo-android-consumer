package com.loresuelvo.consumer.domain.notifications

fun interface ServiceNotificationPublisher {
    fun publish(notification: ServiceNotification): Boolean
}
