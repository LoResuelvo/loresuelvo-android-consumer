package com.loresuelvo.consumer.domain.file

/**
 * Port for the presign → upload → confirm file upload flow
 * used by the consumer ↔ provider chat (and any future flow
 * that needs to attach a file to a business resource: profile
 * photos, job-request images, work-order completion images).
 *
 * The split into three methods is deliberate:
 *  - [presign] asks the backend for a direct upload URL.
 *  - [uploadBytes] PUTs the bytes to that URL. The storage
 *    adapter's signature is the auth — NOT the Auth0 bearer
 *    token. The client must therefore apply the headers from
 *    [PresignUploadResult.headers] verbatim and never strip
 *    them, otherwise the storage adapter rejects the upload.
 *  - [confirm] tells the backend the upload completed so it
 *    can re-validate the object (codec / duration for audio
 *    and video) and flip the file to `confirmed`.
 *
 * Implementations live in `data/api/`. The port never throws
 * on HTTP / network failures: every exception is mapped to a
 * typed `*Outcome.Failure` so the orchestrating use case (and
 * ultimately the ViewModel) can render each branch
 * explicitly.
 *
 * Pure domain. The only non-pure surface is the [uploadBytes]
 * signature, which carries the upload URL string and the
 * `Map<String, String>` of headers — both JDK types, so the
 * abstraction stays testable in plain JUnit without
 * Robolectric.
 */
interface FileRepository {

    /**
     * `POST /files/presign` — request a direct upload URL for
     * a file the caller intends to associate with a business
     * resource later. Returns the storage target the caller
     * will [uploadBytes] to.
     */
    suspend fun presign(
        request: PresignUploadRequest,
    ): PresignUploadOutcome

    /**
     * `PUT uploadUrl` — uploads the file bytes to the storage
     * adapter. The [headers] map MUST include the exact headers
     * the presign response carried (typically at least
     * `Content-Type`); stripping them will fail the upload.
     * No Auth0 token is sent — the storage signature is the
     * auth.
     */
    suspend fun uploadBytes(
        uploadUrl: String,
        headers: Map<String, String>,
        bytes: ByteArray,
    ): UploadBytesOutcome

    /**
     * `POST /files/{fileID}/confirm` — tells the backend the
     * upload is in place. Re-validates codec / duration for
     * audio / video before flipping the file to `confirmed`.
     * The returned [ConfirmedFile.id] is what the caller
     * passes as `audio_file_id` / `image_file_ids[]` /
     * `video_file_id` to the message endpoint.
     */
    suspend fun confirm(
        fileId: String,
        request: ConfirmUploadRequest,
    ): ConfirmUploadOutcome
}