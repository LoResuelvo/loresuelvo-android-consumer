package com.loresuelvo.consumer.domain.auth

sealed interface SessionSynchronizationOutcome {
    data class Success(val session: AuthSession) : SessionSynchronizationOutcome

    sealed interface Failure : SessionSynchronizationOutcome {
        data class Network(val cause: Throwable) : Failure
        data class Server(val code: Int, val message: String) : Failure
        data class Unauthorized(val message: String) : Failure
    }
}
