package com.loresuelvo.consumer.data.api.mapper

import com.loresuelvo.consumer.data.api.dto.MessageNotificationDto
import com.loresuelvo.consumer.domain.notifications.MessageNotification

fun MessageNotificationDto.toDomain() = MessageNotification(
    version = version, eventId = eventId, type = type,
    resourceType = resourceType, destination = destination, conversationId = resourceId,
    recipientId = recipientUserId, app = recipientApp,
    installationId = installationId, bindingId = bindingId,
    title = title, body = body, expiresAt = expiresAt,
)
