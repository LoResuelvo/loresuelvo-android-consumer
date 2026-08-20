package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire format for a single message in a consumer ↔ provider
 * conversation. Used in two places by the backend:
 *
 *  - `last_message` block of each list entry in
 *    `GET /conversations`.
 *  - `messages[]` array of the detail endpoint
 *    `GET /conversations/{id}`.
 *
 * The same DTO serves both because the per-message shape is
 * identical — only the parent field name differs. The
 * conversation message is mutually exclusive on its media
 * block per the OpenAPI `oneOf` / `not.anyOf` rules in
 * `openapi/components/schemas/send-message-request.yaml` and
 * the backend's `internal/domain/conversation/service.go`
 * `sendParticipantMessage`: a persisted bubble carries at most
 * one of `images[]`, `audio`, or `video`.
 *
 *  - `images` carries the confirmed image attachments. The
 *    mapper collapses the first one into a single
 *    [com.loresuelvo.consumer.domain.conversation.MediaReference]
 *    so the domain stays single-valued; future multi-image
 *    scenarios add a separate fan-out branch.
 *  - `audio` carries the confirmed audio attachment (scenario
 *    03-MM). Audio is audio-only — no `content`, no `images`,
 *    no `video` on the same message.
 *  - `video` is reserved for a future scenario; the backend
 *    currently sends it for provider replies with a recorded
 *    video. The mapper ignores the field today but deserialises
 *    it so `ignoreUnknownKeys` doesn't drop nested metadata.
 */
@Serializable
data class ConversationMessageDto(
    @SerialName("id") val id: Long,
    @SerialName("sender_role") val senderRole: String,
    @SerialName("content") val content: String,
    @SerialName("created_on") val createdOn: String? = null,
    @SerialName("images") val images: List<MessageImageDto> = emptyList(),
    @SerialName("audio") val audio: MessageAudioDto? = null,
    @SerialName("video") val video: MessageVideoDto? = null,
)