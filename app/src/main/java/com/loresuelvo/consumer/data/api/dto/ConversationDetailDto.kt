package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire format for the `GET /conversations/{id}` response — the
 * full snapshot of a single conversation including its complete
 * `messages` thread.
 *
 * Example:
 * ```
 * {
 *   "id": 1,
 *   "status": "pending",
 *   "counterpart": { "id": 20, "role": "provider", ... },
 *   "messages": [
 *     {"id": 1, "sender_role": "consumer", "content": "...", "created_on": "..."}
 *   ],
 *   "updated_on": "2026-05-29T18:01:00Z"
 * }
 * ```
 *
 * Distinct from [ConversationDto] (the list element) which carries
 * `last_message` (singular) instead of `messages[]`. The detail
 * mapper at `ConversationDtoMapper.toDomain()` translates the
 * `messages[]` array into a `List<ConversationMessage>` in
 * domain.
 */
@Serializable
data class ConversationDetailDto(
    @SerialName("id") val id: Long,
    @SerialName("status") val status: String,
    @SerialName("counterpart") val counterpart: ConversationCounterpartDto,
    @SerialName("messages") val messages: List<ConversationMessageDto>,
    @SerialName("updated_on") val updatedOn: String? = null,
)