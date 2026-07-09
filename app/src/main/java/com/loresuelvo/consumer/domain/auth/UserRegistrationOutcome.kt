package com.loresuelvo.consumer.domain.auth

/**
 * Outcome of [RegisterConsumerUseCase]. Sealed so callers must
 * explicitly handle the happy path and every documented failure
 * branch. No `Failure.Generic(String)` — every subclass carries the
 * information the UI needs to react meaningfully.
 */
sealed interface UserRegistrationOutcome {

    /**
     * Account was created on the backend. [user] is the session-local
     * `User` with the freshly-entered firstName / lastName already
     * applied; the backend `POST /consumers` endpoint does not return
     * the persisted user, so this value is constructed in the data
     * layer from the existing session + the form input.
     */
    data class Success(val user: User) : UserRegistrationOutcome

    sealed interface Failure : UserRegistrationOutcome {
        /**
         * Network-level failure: timeouts, DNS errors, connection
         * refused, etc. The original throwable is preserved for
         * diagnostics but not shown to the user.
         */
        data class Network(val cause: Throwable) : Failure

        /**
         * Any non-2xx response that the API returned with a parsable
         * body. [code] is the HTTP status, [message] is the human-
         * readable message extracted from `error` or `message` JSON
         * fields.
         */
        data class Server(val code: Int, val message: String) : Failure

        /**
         * 401 from the API. Use cases must clear the local session
         * so the next screen is the Welcome / re-auth flow.
         */
        data class Unauthorized(val message: String) : Failure
    }
}
