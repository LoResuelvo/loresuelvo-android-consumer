package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for `POST /chatbot/conversations` (first message of
 * a new conversation). The webapp lets callers omit `content` when
 * only an image is attached; `null` here means "absent" in the
 * serialised JSON (kotlinx-serialization `explicitNulls = false`
 * is set on the shared [kotlinx.serialization.json.Json] in
 * `di/NetworkModule`).
 */
@Serializable
data class CreateConversationRequestDto(
    @SerialName("content") val content: String,
    @SerialName("image_file_ids") val imageFileIds: List<String>? = null,
)
