package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Image attached to a diagnostic chat message. The webapp carries
 * `id`, `url`, and `original_name` per image. Image attachments
 * are NOT part of the 11 Gherkin scenarios — this DTO exists so
 * the mapper compiles when the backend echoes them on a response.
 */
@Serializable
data class MessageImageDto(
    @SerialName("id") val id: String,
    @SerialName("url") val url: String,
    @SerialName("original_name") val originalName: String,
)
