package com.loresuelvo.consumer.domain.diagnosis

/**
 * Outcome of [DiagnosisRepository.sendPrompt]. Sealed so callers
 * handle every branch explicitly (mirrors
 * [com.loresuelvo.consumer.domain.category.CategoriesOutcome] and
 * [com.loresuelvo.consumer.domain.auth.UserRegistrationOutcome]).
 */
sealed interface SendDiagnosisPromptOutcome {

    data class Success(val diagnosis: Diagnosis) : SendDiagnosisPromptOutcome

    sealed interface Failure : SendDiagnosisPromptOutcome {

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
