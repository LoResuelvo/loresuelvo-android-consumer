package com.loresuelvo.consumer.data.api.mapper

import com.loresuelvo.consumer.data.api.dto.ServiceNotificationDto
import com.loresuelvo.consumer.domain.notifications.ServiceNotification

fun ServiceNotificationDto.toDomain() = ServiceNotification(
    version = version,
    eventId = eventId,
    type = type,
    resourceType = resourceType,
    destination = destination,
    resourceId = resourceId,
    recipientId = recipientUserId,
    app = recipientApp,
    installationId = installationId,
    bindingId = bindingId,
    title = title,
    body = body,
    expiresAt = expiresAt,
)
