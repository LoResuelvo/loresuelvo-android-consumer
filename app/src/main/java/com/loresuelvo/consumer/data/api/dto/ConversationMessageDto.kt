package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire format for a single message in a consumer ↔ provider
 * conversation. Used in two places by the backend:
 *
 *  - `last_message` block of each list entry in
 *    `GET /conversations` (this commit).
 *  - `messages[]` array of the detail endpoint
 *    `GET /conversations/{id}` (lands with scenario 04-IC).
 *
 * The same DTO serves both because the per-message shape is
 * identical — only the parent field name differs.
 *
 * `images` carries the media attachments for the message. The
 * dev backend echoes the image the consumer uploaded (with the
 * server-issued URL) on every persisted bubble. The mapper
 * collapses a non-empty list into a single [MediaReference] so
 * the domain stays simple (`ConversationMessage.media` is
 * single-valued); future multi-image scenarios add a separate
 * fan-out branch.
 */
@Serializable
data class ConversationMessageDto(
    @SerialName("id") val id: Long,
    @SerialName("sender_role") val senderRole: String,
    @SerialName("content") val content: String,
    @SerialName("created_on") val createdOn: String? = null,
    @SerialName("images") val images: List<MessageImageDto> = emptyList(),
)