package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `audio` block nested inside a conversation message (see
 * `openapi/components/schemas/message-audio.yaml`). Carries the
 * confirmed audio file the backend persisted for the message —
 * the consumer ↔ provider chat bubble uses `url` to stream the
 * bytes (private, time-limited per the storage adapter) and
 * `duration_seconds` to render the `mm:ss` counter without a
 * second round-trip.
 *
 *  - [id] is the confirmed audio file UUID (matches the value
 *    the client sent as `audio_file_id` when posting the
 *    message).
 *  - [url] is a private, time-limited signed download URL
 *    scoped to authorized conversation participants. The
 *    client treats it as opaque and never persists it.
 *  - [codec] is the validated codec (`opus` for the
 *    conversation audio policy).
 *  - [durationSeconds] is the validated duration rounded up
 *    to whole seconds (1–300). The mapper multiplies by 1000
 *    to feed `MediaReference.Audio.durationMillis` without
 *    losing precision.
 */
@Serializable
data class MessageAudioDto(
    @SerialName("id") val id: String,
    @SerialName("url") val url: String,
    @SerialName("original_name") val originalName: String,
    @SerialName("mime_type") val mimeType: String = "audio/webm",
    @SerialName("codec") val codec: String = "opus",
    @SerialName("duration_seconds") val durationSeconds: Int = 0,
)