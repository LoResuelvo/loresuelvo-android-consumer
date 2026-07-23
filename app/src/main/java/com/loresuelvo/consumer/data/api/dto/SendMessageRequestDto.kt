package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for `POST /chatbot/conversations/{conversationId}/messages`
 * (subsequent messages in an existing conversation). Image
 * attachments are out of scope for the 11 Gherkin scenarios; the
 * field is reserved for future "attach a photo" iteration.
 */
@Serializable
data class SendMessageRequestDto(
    @SerialName("content") val content: String,
    @SerialName("image_file_ids") val imageFileIds: List<String>? = null,
)
