package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for `POST /files/presign` (see
 * `openapi/paths/files-presign.yaml` and
 * `openapi/components/schemas/presign-file-request.yaml`).
 *
 * The backend creates a pending `File` row tied to the
 * authenticated user and returns a direct upload URL plus the
 * headers the client must include when `PUT`-ing the bytes. The
 * client never uploads to the backend itself — it goes straight to
 * the storage adapter (S3 in prod, in-memory in tests).
 *
 * `purpose` is a free-form string at the wire layer (the backend
 * validates it against its enum of `UploadPolicy.Purpose` values
 * via `purposeFor`). The mapping from this DTO's `purpose` to the
 * backend's wire constant is owned by `ApiFileRepository` — the
 * domain layer never sees the literal string.
 *
 * `sizeBytes` is `Int` to match the backend's `int` (int32) wire
 * type; the largest payload the platform accepts today is the
 * 50 MiB conversation video, well under Int.MAX_VALUE.
 */
@Serializable
data class PresignFileRequestDto(
    @SerialName("original_name") val originalName: String,
    @SerialName("mime_type") val mimeType: String,
    @SerialName("size_bytes") val sizeBytes: Int,
    @SerialName("purpose") val purpose: String,
)