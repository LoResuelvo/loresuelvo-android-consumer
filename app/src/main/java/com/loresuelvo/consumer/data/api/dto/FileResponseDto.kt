package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FileResponseDto(
    @SerialName("id") val id: String,
    @SerialName("url") val url: String? = null,
    @SerialName("original_name") val originalName: String,
    @SerialName("mime_type") val mimeType: String,
    @SerialName("type") val type: String,
    @SerialName("audio") val audio: FileAudioMetadataDto? = null,
)

/**
 * `audio` block nested inside a confirmed audio [FileResponseDto]
 * (see `openapi/components/schemas/file-audio-metadata.yaml`).
 *
 * `codec` is the audio codec the backend actually observed in the
 * uploaded bytes after parsing; `opus` is the only allowed value
 * for the conversation audio policy. `durationSeconds` is the
 * real duration rounded up to whole seconds (the audio policy
 * caps it at 300).
 *
 * The client never sends these — they come from the backend's
 * re-validation of the bytes (see
 * `internal/domain/file/service.go` `confirmAudioFile`).
 */
@Serializable
data class FileAudioMetadataDto(
    @SerialName("codec") val codec: String,
    @SerialName("duration_seconds") val durationSeconds: Int,
)