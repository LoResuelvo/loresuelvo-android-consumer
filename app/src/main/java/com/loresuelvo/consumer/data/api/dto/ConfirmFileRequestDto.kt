package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for `POST /files/{fileID}/confirm` (see
 * `openapi/components/schemas/confirm-file-request.yaml`).
 *
 * The client echoes back the values it received from the presign
 * response so the backend can cross-check them against the
 * storage object's actual metadata before flipping the file to
 * `confirmed`. Any mismatch — wrong key, wrong mime, wrong size —
 * is rejected with `ErrFileNotAvailable`
 * (`internal/domain/file/service.go` `ConfirmUpload`).
 *
 * For audio / video the backend additionally re-validates the
 * codec and duration against the policy (see
 * `confirmAudioFile` / `confirmVideoFile`). The client never has
 * to send those fields — the backend reads the bytes.
 */
@Serializable
data class ConfirmFileRequestDto(
    @SerialName("key") val key: String,
    @SerialName("mime_type") val mimeType: String,
    @SerialName("size_bytes") val sizeBytes: Int,
)