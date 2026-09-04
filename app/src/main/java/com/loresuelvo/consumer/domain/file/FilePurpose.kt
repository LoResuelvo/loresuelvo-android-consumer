package com.loresuelvo.consumer.domain.file

/**
 * Business purpose the client intends to use an uploaded file
 * for. Maps 1:1 to the `purpose` enum the backend validates
 * against its `UploadPolicy` table (see
 * `internal/domain/file/upload_policy.go`); the actual wire
 * constant lives in the data layer's mapper.
 *
 * Pure domain. The case names mirror the backend identifiers
 * verbatim so the mapper is a trivial `name.lowercase()` call —
 * anything else would be a translation bug.
 *
 * The list mirrors the wire enum in
 * `openapi/components/schemas/presign-file-request.yaml:23-29`:
 *  - `profile_photo` — public avatar on the user profile.
 *  - `conversation_message_audio` — private WebM/Opus audio
 *    attached to a conversation message (scenario 03-MM).
 *    Backend policy caps it at 5 MiB and 300 seconds.
 *  - `conversation_message_video` — private MP4/H.264 video
 *    attached to a conversation message. Backend policy caps
 *    it at 50 MiB, 120 seconds and 1920×1920 px.
 *  - `conversation_message_image` — private JPEG/PNG/WebP
 *    image attached to a conversation message. Backend
 *    policy caps it at 5 MiB per file and 5 files per
 *    message; the consumer app currently sends one image per
 *    message (single-valued `MediaReference.Image`).
 *  - `job_request_image` — private JPEG/PNG/WebP image
 *    attached to a `JobRequest` (the consumer's "Contactar
 *    proveedor" form). Backend policy mirrors the chat image
 *    caps and the consumer app caps the per-request total at
 *    [com.loresuelvo.consumer.domain.jobrequest.MAX_JOB_REQUEST_IMAGES].
 *
 * The work-order completion image purpose is out of scope for
 * the consumer app today; if a future flow needs it, add the
 * constant here without touching the existing cases (callers
 * dispatch on exhaustive `when`).
 */
enum class FilePurpose {
    PROFILE_PHOTO,
    CONVERSATION_MESSAGE_AUDIO,
    CONVERSATION_MESSAGE_VIDEO,
    CONVERSATION_MESSAGE_IMAGE,
    JOB_REQUEST_IMAGE,
}