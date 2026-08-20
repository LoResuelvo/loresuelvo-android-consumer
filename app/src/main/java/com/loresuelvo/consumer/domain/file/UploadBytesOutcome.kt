package com.loresuelvo.consumer.domain.file

/**
 * Outcome of [FileRepository.uploadBytes]. The success branch
 * carries no payload — the storage adapter either accepted the
 * bytes (2xx) or it didn't. The orchestrating use case only
 * cares about whether to proceed to [FileRepository.confirm].
 *
 * The repository never throws on HTTP / network failures:
 * every exception is mapped to a typed
 * [UploadBytesOutcome.Failure].
 */
sealed interface UploadBytesOutcome {

    data object Success : UploadBytesOutcome

    sealed interface Failure : UploadBytesOutcome {
        /** Transport-level failure: timeouts, DNS, connection refused,
         *  TLS handshake failures against the storage endpoint. */
        data class Network(val cause: Throwable) : Failure

        /** Any non-2xx response from the storage adapter.
         *  S3 returns 403 when the signature expired or the
         *  headers were stripped; either surfaces here. */
        data class Server(val code: Int, val message: String) : Failure

        /** Reserved for parity with the backend endpoints that
         *  do require auth; storage pre-signed URLs do not,
         *  so this branch is never used in practice today. */
        data class Unauthorized(val message: String) : Failure
    }
}