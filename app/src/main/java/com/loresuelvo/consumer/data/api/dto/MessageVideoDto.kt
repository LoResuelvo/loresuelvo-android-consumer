package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `video` block nested inside a conversation message. The mapper
 * exposes the block as `MediaReference.Video` for rendering and
 * playback by the conversation UI.
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
