package com.loresuelvo.consumer.data.api.dto

data class MessageNotificationDto(
    val version: String, val eventId: String, val type: String,
    val resourceType: String, val destination: String, val resourceId: Int,
    val recipientUserId: Int, val recipientApp: String,
    val installationId: String, val bindingId: String,
    val title: String, val body: String, val expiresAt: Long,
)
