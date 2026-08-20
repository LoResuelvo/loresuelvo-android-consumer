package com.loresuelvo.consumer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response body for `POST /files/presign` (see
 * `openapi/components/schemas/presign-file-response.yaml`).
 *
 *  - [fileId] is the API-issued UUID the client later sends as
 *    `audio_file_id` (or `image_file_ids[]`) when calling
 *    `POST /conversations/{id}/messages`.
 *  - [key] is the storage object key the backend generated; the
 *    client must echo it verbatim in the subsequent confirm call.
 *  - [uploadUrl] is a pre-signed storage URL (S3 `PUT` in prod,
 *    in-memory `PUT` in tests). It does NOT require the Auth0
 *    bearer token — the storage signature is the auth.
 *  - [headers] is the set of headers the client must include
 *    verbatim on the `PUT` to [uploadUrl]. At minimum it carries
 *    `Content-Type` (which the S3 adapter also signs into the
 *    URL). Stripping or rewriting them will fail the upload.
 */
@Serializable
data class PresignFileResponseDto(
    @SerialName("file_id") val fileId: String,
    @SerialName("key") val key: String,
    @SerialName("upload_url") val uploadUrl: String,
    @SerialName("headers") val headers: Map<String, String>,
)