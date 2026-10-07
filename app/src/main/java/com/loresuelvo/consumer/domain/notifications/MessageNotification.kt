package com.loresuelvo.consumer.domain.notifications

data class MessageNotification(
    val version: String,
    val eventId: String,
    val type: String,
    val resourceType: String,
    val destination: String,
    val conversationId: Int,
    val recipientId: Int,
    val app: String,
    val installationId: String,
    val bindingId: String,
    val title: String,
    val body: String,
    val expiresAt: Long,
)

data class NotificationInstallation(val installationId: String, val bindingId: String, val userId: Int)

enum class MessageNotificationOutcome { Published, Invalid, Unavailable, VisibleConversation, Duplicate }
