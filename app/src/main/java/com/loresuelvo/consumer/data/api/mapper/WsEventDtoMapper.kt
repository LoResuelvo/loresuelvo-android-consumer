package com.loresuelvo.consumer.data.api.mapper

import com.loresuelvo.consumer.data.api.dto.ConversationMessageDto
import com.loresuelvo.consumer.data.api.dto.WsEventDto
import com.loresuelvo.consumer.domain.realtime.WsEvent

/**
 * DTO → domain translation for the WebSocket push frames. The
 * mapper lives in `data/` per AGENTS.md so snake_case ↔ camelCase
 * stays at the wire boundary. Returns `null` for events whose
 * `type` the Android consumer does not yet handle so the
 * upstream [com.loresuelvo.consumer.data.api.WebSocketClient]
 * can drop them silently without crashing the connection.
 *
 * The `message` sub-DTO is mapped through the existing
 * [ConversationMessageDto.toDomain] extension so the wire
 * translation is shared with the REST `GET /conversations/{id}`
 * path — same field names, same parser, same semantics.
 */
internal fun WsEventDto.toDomain(): WsEvent? {
    return when (type) {
        WsEvent.CONVERSATION_MESSAGE_CREATED -> {
            val conversationId = conversationId ?: return null
            val message = message ?: return null
            val domainMessage = ConversationMessageDto(
                id = message.id,
                senderRole = message.senderRole,
                content = message.content,
                createdOn = message.createdOn,
                video = message.video,
            ).toDomain()
            WsEvent.ConversationMessageCreated(conversationId, domainMessage)
        }
        WsEvent.NOTIFICATION_CREATED -> {
            val notification = notification ?: return null
            if (notification.resourceType.isBlank() || notification.resourceId <= 0L) return null
            WsEvent.NotificationCreated(notification.resourceType, notification.resourceId.toString())
        }
        else -> null
    }
}
