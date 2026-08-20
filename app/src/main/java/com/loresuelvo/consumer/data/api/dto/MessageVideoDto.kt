package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `video` block nested inside a conversation message (see
 * `openapi/components/schemas/message-video.yaml`). The
 * consumer app doesn't render video bubbles yet — the field
 * exists so the wire deserialises cleanly via
 * `ignoreUnknownKeys` and so the next iteration can surface it
 * on the bubble without a new DTO migration.
 *
 * Not exposed on the domain yet; the mapper in
 * `ConversationDtoMapper` ignores the field. When the
 * consumer-side video flow lands it will gain a
 * `MediaReference.Video` variant and a domain mapper here.
 */
@Serializable
data class MessageVideoDto(
    @SerialName("id") val id: String,
    @SerialName("url") val url: String,
    @SerialName("original_name") val originalName: String,
    @SerialName("mime_type") val mimeType: String = "video/mp4",
    @SerialName("video_codec") val videoCodec: String = "h264",
    @SerialName("audio_codec") val audioCodec: String? = null,
    @SerialName("duration_seconds") val durationSeconds: Int = 0,
    @SerialName("width") val width: Int = 0,
    @SerialName("height") val height: Int = 0,
)