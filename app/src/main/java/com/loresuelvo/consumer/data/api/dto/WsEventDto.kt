package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire shape of a single frame pushed by the backend's WebSocket
 * (`/ws`). Mirrors the webapp's `WsEvent` interface in
 * `infrastructure/websocket/types.ts`.
 *
 * Example frame (mirrors the user's manual curl):
 * ```
 * {
 *   "type": "conversation.message.created",
 *   "conversation_id": 1,
 *   "message": {
 *     "id": 2,
 *     "sender_role": "consumer",
 *     "content": "¿El jueves por la mañana te queda cómodo?",
 *     "created_on": "2026-05-31T12:00:00Z"
 *   }
 * }
 * ```
 *
 * The wire carries `conversation_id` (snake_case) plus the embedded
 * `message` block. `created_on` is optional (matches the schema
 * used elsewhere in this repo); `ignoreUnknownKeys = true`
 * discards extra fields like `images` if the backend ever sends
 * them (the webapp's `WsEventMessage` interface has `images?`).
 */
@Serializable
data class WsEventDto(
    @SerialName("type") val type: String,
    @SerialName("conversation_id") val conversationId: Long,
    @SerialName("message") val message: WsEventMessageDto,
)

/**
 * Wire shape of the `message` field in [WsEventDto]. Same shape
 * as [ConversationMessageDto] (the embedded message in
 * `GET /conversations/{id}.messages[]` and the response of
 * `POST /conversations/{id}/messages`) — the mapper reuses the
 * `ConversationMessageDto.toDomain()` extension to avoid
 * duplicating the field translation.
 */
@Serializable
data class WsEventMessageDto(
    @SerialName("id") val id: Long,
    @SerialName("sender_role") val senderRole: String,
    @SerialName("content") val content: String,
    @SerialName("created_on") val createdOn: String? = null,
)