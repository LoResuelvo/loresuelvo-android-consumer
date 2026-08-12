package com.loresuelvo.consumer.domain.assistant

/**
 * Outcome of [AiConversationRepository.getConversations]. Sealed
 * so callers (the future `AssistantViewModel`) must explicitly
 * handle the happy path and every documented failure branch —
 * mirrors the convention used by
 * `SendDiagnosisPromptOutcome` and `CreateAiJobRequestOutcome`.
 *
 *  - [Success]: the backend returned the list of
 *    [AiConversationSummary] rows. Empty list = no past sessions
 *    (the screen renders the empty state). Ordering is the
 *    backend's (timestamp-descending by convention).
 *  - [Failure.Network]: transport-level failure (timeout, DNS,
 *    connection refused).
 *  - [Failure.Server]: any non-2xx response with a parsable
 *    body. [code] is the HTTP status, [message] the
 *    human-readable message extracted from the `error`/`message`
 *    JSON fields (see `ApiErrorMapping`).
 *  - [Failure.Unauthorized]: 401 from the backend. The Auth0
 *    session is invalid or expired; downstream code typically
 *    clears the local session and routes the user back to
 *    Welcome.
 */
sealed interface AiConversationListOutcome {

    data class Success(
        val conversations: List<AiConversationSummary>,
    ) : AiConversationListOutcome

    sealed interface Failure : AiConversationListOutcome {
        data class Network(val cause: Throwable) : Failure
        data class Server(val code: Int, val message: String) : Failure
        data class Unauthorized(val message: String) : Failure
    }
}
