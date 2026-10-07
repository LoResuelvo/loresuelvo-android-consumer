package com.loresuelvo.consumer.domain.notifications

interface NotificationEventStore {
    fun contains(bindingId: String, eventId: String): Boolean
    fun remember(bindingId: String, eventId: String): Boolean
}
