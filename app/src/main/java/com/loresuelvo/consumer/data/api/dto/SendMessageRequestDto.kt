package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body shared by both message endpoints:
 *  - `POST /chatbot/conversations/{conversationId}/messages`
 *    (AI diagnostic follow-up; the 11 Gherkin scenarios only ever
 *    send `content`).
 *  - `POST /conversations/{conversationId}/messages` (consumer ↔
 *    provider chat; supports text, images, audio, video per
 *    scenario 01-MM onwards — see `openapi/components/schemas/
 *    send-message-request.yaml`).
 *
 * Wire shape (matching the backend's `sendMessageRequest` in
 * `internal/adapters/http/handler/conversation_handler/request.go`):
 *  - `content` is non-nullable because the AI diagnostic endpoint
 *    always requires it; for the consumer ↔ provider flow an
 *    audio-only or video-only message sends `content = ""`.
 *  - `image_file_ids` is `List<String>?` of confirmed file UUIDs
 *    (max 5, backend-enforced; see
 *    `openapi/components/schemas/send-message-request.yaml:19-26`).
 *  - `audio_file_id` is a single confirmed audio UUID
 *    (`audio/webm` + `opus`, ≤ 5 MiB, ≤ 300 s, validated server-
 *    side at confirm time — see `internal/domain/file/service.go`
 *    `confirmAudioFile`). It is mutually exclusive with `content`,
 *    `image_file_ids` and `video_file_id` (the OpenAPI `not.anyOf`
 *    rules map to `ErrMessageAudioMustBeExclusive` in the
 *    service).
 *  - `video_file_id` is a single confirmed video UUID
 *    (`video/mp4` + H.264, ≤ 50 MiB, ≤ 120 s, ≤ 1920×1920 px). It
 *    may be combined with `content` but is mutually exclusive with
 *    `image_file_ids` and `audio_file_id`.
 *
 * The four fields are nullable so the AI diagnostic call site
 * (which only ever sets `content`) keeps compiling unchanged.
 */
@Serializable
data class SendMessageRequestDto(
    @SerialName("content") val content: String,
    @SerialName("image_file_ids") val imageFileIds: List<String>? = null,
    @SerialName("audio_file_id") val audioFileId: String? = null,
    @SerialName("video_file_id") val videoFileId: String? = null,
)
