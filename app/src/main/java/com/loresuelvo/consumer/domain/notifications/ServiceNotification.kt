package com.loresuelvo.consumer.domain.notifications

data class ServiceNotification(
    val version: String,
    val eventId: String,
    val type: String,
    val resourceType: String,
    val destination: String,
    val resourceId: String,
    val recipientId: Int,
    val app: String,
    val installationId: String,
    val bindingId: String,
    val title: String,
    val body: String,
    val expiresAt: Long,
)

enum class ServiceNotificationOutcome {
    Published,
    Invalid,
    Unavailable,
    Duplicate,
}
