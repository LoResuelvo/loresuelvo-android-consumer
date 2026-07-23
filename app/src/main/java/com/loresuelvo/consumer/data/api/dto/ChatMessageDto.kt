package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire shape for a single message in the AI diagnostic chat. The
 * backend issues numeric message ids (`{"id": 1}`) and uses
 * `sender_role: "consumer" | "chatbot"` as a free-form string;
 * the mapper translates the union into the domain's `Sender`
 * sealed type.
 *
 * Timestamps carry alternative names (`sent_at`, `created_on`)
 * because the backend has historically mixed fields; the mapper
 * accepts either.
 */
@Serializable
data class ChatMessageDto(
    @SerialName("id") val id: Long,
    @SerialName("sender_role") val senderRole: String,
    @SerialName("content") val content: String,
    @SerialName("sent_at") val sentAt: String? = null,
    @SerialName("created_on") val createdOn: String? = null,
    @SerialName("images") val images: List<MessageImageDto>? = null,
)
