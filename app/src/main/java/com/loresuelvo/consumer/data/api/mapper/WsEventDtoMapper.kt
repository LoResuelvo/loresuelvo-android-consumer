package com.loresuelvo.consumer.data.api.mapper

import com.loresuelvo.consumer.data.api.dto.WsEventDto
import com.loresuelvo.consumer.domain.conversation.ConversationMessage
import com.loresuelvo.consumer.domain.conversation.ConversationSender
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
    if (type != WsEvent.CONVERSATION_MESSAGE_CREATED) return null
    val sender = when (message.senderRole.lowercase()) {
        "consumer" -> ConversationSender.Consumer
        "provider" -> ConversationSender.Provider
        // Defensive: a third sender role from a backend revision
        // renders as a Provider bubble so the user can still read
        // the message body.
        else -> ConversationSender.Provider
    }
    val createdOnEpochMillis = parseIsoTimestampMillisOrZero(message.createdOn) ?: 0L
    val domainMessage = ConversationMessage(
        id = message.id.toString(),
        sender = sender,
        content = message.content,
        createdOnEpochMillis = createdOnEpochMillis,
    )
    return WsEvent(
        type = type,
        conversationId = conversationId,
        message = domainMessage,
    )
}