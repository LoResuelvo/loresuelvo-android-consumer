package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Image attached to a diagnostic chat message. The webapp carries
 * `id`, `url`, and `original_name` per image. Image attachments
 * are NOT part of the 11 Gherkin scenarios — this DTO exists so
 * the mapper compiles when the backend echoes them on a response.
 *
 * `mime_type` is optional in the wire shape: a legacy backend
 * revision may omit it. The mapper falls back to
 * `application/octet-stream` so the bubble still renders instead
 * of crashing on a backend regression.
 */
@Serializable
data class MessageImageDto(
    @SerialName("id") val id: String,
    @SerialName("url") val url: String,
    @SerialName("original_name") val originalName: String,
    @SerialName("mime_type") val mimeType: String = "application/octet-stream",
)
