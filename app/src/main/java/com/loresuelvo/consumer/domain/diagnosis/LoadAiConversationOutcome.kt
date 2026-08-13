package com.loresuelvo.consumer.domain.diagnosis

import com.loresuelvo.consumer.domain.provider.Provider

/**
 * Outcome of [DiagnosisRepository.getAiConversation]. Sealed so
 * the future `ChatViewModel.loadExisting` (board commit 5b) must
 * explicitly handle the happy path and every documented failure
 * branch — same shape as
 * `SendDiagnosisPromptOutcome` and `CreateAiJobRequestOutcome`.
 *
 *  - [Success]: the backend returned the full AI conversation
 *    snapshot including the saved [ChatMessage] history,
 *    [Diagnosis.assessment] (if the diagnosis concluded), and
 *    [Diagnosis.recommendedProviders] (if the AI matched a rubro).
 *    The chat scroll hydrates from this state.
 *  - [Failure.Network]: transport-level failure (timeout, DNS,
 *    connection refused).
 *  - [Failure.Server]: any non-2xx response. [code] is the HTTP
 *    status, [message] the human-readable message extracted from
 *    the `error`/`message` JSON fields (see `ApiErrorMapping`).
 *  - [Failure.Unauthorized]: 401 from the backend. The Auth0
 *    session is invalid or expired.
 */
sealed interface LoadAiConversationOutcome {

    data class Success(val diagnosis: Diagnosis) : LoadAiConversationOutcome

    sealed interface Failure : LoadAiConversationOutcome {
        data class Network(val cause: Throwable) : Failure
        data class Server(val code: Int, val message: String) : Failure
        data class Unauthorized(val message: String) : Failure
    }
}
