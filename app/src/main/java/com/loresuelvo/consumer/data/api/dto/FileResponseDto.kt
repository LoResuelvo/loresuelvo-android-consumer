package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response body for `POST /files/{fileID}/confirm` (see
 * `openapi/components/schemas/file-response.yaml`).
 *
 * `type` is the discriminator the backend uses to pick the
 * matching nested metadata block: `image`, `audio`, or `video`.
 * The wire is a `oneOf` whose branches require the nested block
 * for the declared type and forbid the other variants — the
 * backend never sends `audio` and `video` together.
 *
 * `url` is omitted for private files (conversation audio/video,
 * conversation images, job-request images, work-order completion
 * images); it's only populated for public profile photos. Audio
 * is private, so for our 03-MM flow [url] will be `null`.
 *
 * `ignoreUnknownKeys = true` (set in `NetworkModule.provideJson`)
 * makes kotlinx-serialization tolerant to the other variants'
 * nested blocks being absent.
 */
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