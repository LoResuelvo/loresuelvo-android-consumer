package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire format for one entry in the `GET /conversations` response
 * (the consumer's conversations list, ordered by `updated_on`
 * descending).
 *
 * Example:
 * ```
 * {
 *   "id": 1,
 *   "status": "pending",
 *   "counterpart": { "id": 20, "role": "provider", ... },
 *   "last_message": { "id": 1, "sender_role": "consumer", ... },
 *   "updated_on": "2026-05-30T14:20:00Z"
 * }
 * ```
 *
 * `last_message` is optional in the wire because the backend may
 * return a freshly-created conversation before persisting its
 * first message; the mapper translates that to a `null`
 * [com.loresuelvo.consumer.domain.conversation.ConversationMessage]
 * in domain.
 *
 * The detail endpoint (`GET /conversations/{id}`) returns a
 * different shape (with a `messages[]` array instead of
 * `last_message`); that DTO is added when scenario 04-IC lands.
 */
@Serializable
data class ConversationDto(
    @SerialName("id") val id: Long,
    @SerialName("status") val status: String,
    @SerialName("counterpart") val counterpart: ConversationCounterpartDto,
    @SerialName("last_message") val lastMessage: ConversationMessageDto? = null,
    @SerialName("updated_on") val updatedOn: String? = null,
)