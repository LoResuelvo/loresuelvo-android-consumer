package com.loresuelvo.consumer.domain.jobrequest

/**
 * Outcome of [JobRequestRepository.createJobRequest]. Sealed so
 * callers must explicitly handle the happy path and every
 * documented failure branch — same shape as
 * `UserRegistrationOutcome` and `SendDiagnosisPromptOutcome`.
 *
 *  - [Success]: the backend accepted the job request and returned
 *    the freshly-persisted [JobRequest] (with a backend-issued
 *    `id` and `conversationId`).
 *  - [Failure.Network]: transport-level failure (timeout, DNS,
 *    connection refused). The original throwable is preserved
 *    for diagnostics but not shown to the user.
 *  - [Failure.Server]: any non-2xx response that the backend
 *    returned with a parsable body. [code] is the HTTP status,
 *    [message] is the human-readable message extracted from the
 *    `error`/`message` JSON fields (see `ApiErrorMapping`).
 *    Validation errors (400) land here.
 *  - [Failure.Unauthorized]: 401 from the backend. The Auth0
 *    session is invalid or expired; downstream code typically
 *    clears the local session and routes the user back to
 *    Welcome.
 */
sealed interface CreateJobRequestOutcome {

    data class Success(val jobRequest: JobRequest) : CreateJobRequestOutcome

    sealed interface Failure : CreateJobRequestOutcome {
        data class Network(val cause: Throwable) : Failure
        data class Server(val code: Int, val message: String) : Failure
        data class Unauthorized(val message: String) : Failure
    }
}
