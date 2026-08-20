package com.loresuelvo.consumer.domain.file

/**
 * Outcome of [FileRepository.presign]. Mirrors the failure
 * tree used across the app (`SendMessageOutcome.Failure`,
 * `ConversationsOutcome.Failure`, ...) so the orchestrating
 * use case can translate every branch uniformly.
 *
 * The repository never throws on HTTP / network failures:
 * every exception is mapped to a typed
 * [PresignUploadOutcome.Failure] so the caller handles each
 * branch explicitly.
 */
sealed interface PresignUploadOutcome {

    data class Success(val result: PresignUploadResult) : PresignUploadOutcome

    sealed interface Failure : PresignUploadOutcome {
        /** Transport-level failure: timeouts, DNS, connection refused. */
        data class Network(val cause: Throwable) : Failure

        /** Any non-2xx response. [code] is the HTTP status, [message] the
         *  human-readable text extracted from the error body. */
        data class Server(val code: Int, val message: String) : Failure

        /** 401: Auth0 session expired or invalid. */
        data class Unauthorized(val message: String) : Failure
    }
}