package com.loresuelvo.consumer.domain.file

/**
 * Body for `POST /files/{fileID}/confirm`. The client echoes
 * the values it received from the presign response so the
 * backend can cross-check them against the storage object's
 * actual metadata before flipping the file to `confirmed`
 * (see `internal/domain/file/service.go` `ConfirmUpload`).
 *
 * Any mismatch — wrong key, wrong mime, wrong size — is
 * rejected with `400 ErrFileNotAvailable`. For audio / video
 * the backend additionally re-validates the codec and
 * duration against the policy (see `confirmAudioFile` /
 * `confirmVideoFile`); the client never sends those fields,
 * the backend reads them from the bytes.
 *
 * Pure domain; the wire shape lives in the data layer
 * (`ConfirmFileRequestDto`).
 */
data class ConfirmUploadRequest(
    val key: String,
    val mimeType: String,
    val sizeBytes: Int,
)