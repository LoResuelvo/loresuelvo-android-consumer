package com.loresuelvo.consumer.data.api.upload

import com.loresuelvo.consumer.domain.file.UploadBytesOutcome

/**
 * Port for uploading raw bytes to a pre-signed storage URL.
 * Lives in `data/api/upload/` because every implementation must
 * speak HTTP, but stays behind an interface so the rest of the
 * data layer (the `FileRepository` adapter) does not couple to
 * OkHttp directly.
 *
 * The split between [FileRepository] (three methods) and this
 * single-purpose port is intentional: the presign / confirm
 * round-trips go through the backend's REST API and share the
 * Auth0 interceptor + the JSON converter, while the upload
 * itself is a raw `PUT` to a storage URL signed by the
 * storage adapter — it MUST NOT carry the Auth0 bearer token,
 * and the headers the presign response carries must be applied
 * verbatim (the storage signature includes them).
 *
 * `data/api/upload/OkHttpFileUploader` is the production impl
 * and consumes a dedicated `OkHttpClient` configured in
 * `NetworkModule` that has the `AuthInterceptor` removed.
 */
interface FileUploader {

    /**
     * `PUT` [bytes] to [uploadUrl], setting every entry in
     * [headers] on the outbound request. Returns the upload
     * outcome — success when the storage adapter accepts the
     * bytes (2xx), or a typed [UploadBytesOutcome.Failure]
     * for transport errors, non-2xx responses, or any
     * unexpected throwable.
     *
     * The caller is responsible for setting [bytes].size into
     * `Content-Length` when the underlying transport requires
     * it; this port does not add it implicitly so the headers
     * from the presign response are not silently mutated.
     */
    suspend fun upload(
        uploadUrl: String,
        headers: Map<String, String>,
        bytes: ByteArray,
    ): UploadBytesOutcome
}