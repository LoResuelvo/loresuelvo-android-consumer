package com.loresuelvo.consumer.domain.file

/**
 * Body for `POST /files/presign`. The backend creates a pending
 * `File` row tied to the authenticated user and returns a direct
 * upload URL. See `openapi/paths/files-presign.yaml` and
 * `internal/domain/file/service.go` `RequestUpload`.
 *
 * Pure domain; the wire shape lives in the data layer
 * (`PresignFileRequestDto`). The [FilePurpose] never sees the
 * backend's wire string — the data layer maps it.
 *
 * [sizeBytes] is bounded by the matching `UploadPolicy`
 * (`profilePhotoPolicy`, `conversationMessageAudioPolicy`,
 * `conversationMessageVideoPolicy`, ...) on the backend side;
 * passing a value that exceeds the policy's `MaxSizeBytes`
 * returns `400 ErrUnsupported*`. The client mirrors the check
 * before posting to fail fast on local validation.
 */
data class PresignUploadRequest(
    val originalName: String,
    val mimeType: String,
    val sizeBytes: Int,
    val purpose: FilePurpose,
)