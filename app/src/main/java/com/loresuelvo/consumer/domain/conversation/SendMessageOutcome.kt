package com.loresuelvo.consumer.domain.conversation

/**
 * Outcome of [ConversationRepository.sendMessage]. Sealed so
 * callers handle every branch explicitly — mirrors
 * [ConversationsOutcome].
 *
 * On success, the carried [ConversationMessage] is the
 * server-persisted bubble (with the backend-issued numeric id
 * and the authoritative `created_on` timestamp). The VM uses it
 * to replace the optimistic bubble it appended locally — the
 * optimistic id (`user-<uuid>`) is dropped in favour of the
 * stable server id so `LazyColumn` keys stay stable across
 * rotation and process death.
 *
 * The repository never throws on HTTP / network failures: every
 * exception is mapped to a typed [SendMessageOutcome.Failure].
 */
sealed interface SendMessageOutcome {

    data class Success(val message: ConversationMessage) : SendMessageOutcome

    sealed interface Failure : SendMessageOutcome {

        /** Transport-level failure: timeouts, DNS, connection refused. */
        data class Network(val cause: Throwable) : Failure

        /**
         * Any non-2xx response. [code] is the HTTP status, [message]
         * the human-readable text extracted from the error body.
         */
        data class Server(val code: Int, val message: String) : Failure

        /** 401: Auth0 session expired or invalid. */
        data class Unauthorized(val message: String) : Failure
    }
}