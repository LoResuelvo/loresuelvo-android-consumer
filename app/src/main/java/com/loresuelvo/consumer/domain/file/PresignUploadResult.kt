package com.loresuelvo.consumer.domain.file

/**
 * Backend-issued presigned upload target. Pure domain; the wire
 * shape lives in the data layer (`PresignFileResponseDto`).
 *
 *  - [fileId] is the API-issued UUID the client later sends as
 *    `audio_file_id` (or `image_file_ids[]` / `video_file_id`)
 *    when calling `POST /conversations/{id}/messages`.
 *  - [key] is the storage object key the backend generated;
 *    the client echoes it verbatim in the subsequent confirm
 *    call.
 *  - [uploadUrl] is a pre-signed storage URL (S3 `PUT` in
 *    prod, in-memory `PUT` in tests). It does NOT require
 *    the Auth0 bearer token — the storage signature is the
 *    auth.
 *  - [headers] are the headers the client must include
 *    verbatim on the `PUT` to [uploadUrl]. At minimum they
 *    carry `Content-Type`; stripping them will fail the upload.
 */
data class PresignUploadResult(
    val fileId: String,
    val key: String,
    val uploadUrl: String,
    val headers: Map<String, String>,
)