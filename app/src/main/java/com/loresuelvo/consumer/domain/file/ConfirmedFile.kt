package com.loresuelvo.consumer.domain.file

/**
 * Backend-issued confirmation result. Pure domain; the wire
 * shape lives in the data layer (`FileResponseDto`).
 *
 *  - [id] is the API-issued UUID the caller forwards as
 *    `audio_file_id` / `image_file_ids[]` / `video_file_id`
 *    to the message endpoint.
 *  - [mimeType] is the validated mime the backend accepted
 *    (`audio/webm` for our 03-MM flow).
 *  - [originalName] is the original file name the user picked.
 *  - [codec] is the audio/video codec the backend parsed from
 *    the bytes (`opus` for conversation audio, `h264` for
 *    conversation video). Empty for image files.
 *  - [durationSeconds] is the validated duration rounded up
 *    to whole seconds (audio ≤ 300, video ≤ 120). `0` for
 *    image files.
 *
 * `FileResponseDto.url` is intentionally NOT carried here:
 * it's only populated for public files (profile photos), and
 * the private download URL the chat bubble needs is generated
 * server-side and shipped back inside the message payload
 * (`SentMessage.audio.url`, `SentMessage.images[].url`,
 * `SentMessage.video.url`). Splitting the two keeps the
 * upload-time entity free of URL semantics that don't apply
 * to private files.
 */
data class ConfirmedFile(
    val id: String,
    val mimeType: String,
    val originalName: String,
    val codec: String,
    val durationSeconds: Int,
)