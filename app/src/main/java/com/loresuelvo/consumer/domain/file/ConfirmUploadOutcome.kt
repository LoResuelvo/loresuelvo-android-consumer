package com.loresuelvo.consumer.domain.file

/**
 * Outcome of [FileRepository.confirm]. The success branch
 * carries the backend-issued [ConfirmedFile]; the failure tree
 * mirrors [PresignUploadOutcome.Failure] so the orchestrating
 * use case can collapse the two when reporting a transient
 * error to the UI.
 *
 * The repository never throws on HTTP / network failures:
 * every exception is mapped to a typed
 * [ConfirmUploadOutcome.Failure].
 */
sealed interface ConfirmUploadOutcome {

    data class Success(val file: ConfirmedFile) : ConfirmUploadOutcome

    sealed interface Failure : ConfirmUploadOutcome {
        /** Transport-level failure: timeouts, DNS, connection refused. */
        data class Network(val cause: Throwable) : Failure

        /** Any non-2xx response. Most commonly `400 ErrFileNotAvailable`
         *  (size / mime / ownership mismatch) or
         *  `400 ErrUnsupportedMessageAudio` / `ErrUnsupportedMessageVideo`
         *  (codec / duration / size policy violation). */
        data class Server(val code: Int, val message: String) : Failure

        /** 401: Auth0 session expired or invalid. */
        data class Unauthorized(val message: String) : Failure
    }
}