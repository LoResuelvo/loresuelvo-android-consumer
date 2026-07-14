package com.loresuelvo.consumer.domain.auth

sealed interface CurrentUserOutcome {
    data class Success(val user: User) : CurrentUserOutcome
    data object NotFound : CurrentUserOutcome

    sealed interface Failure : CurrentUserOutcome {
        data class Network(val cause: Throwable) : Failure
        data class Server(val code: Int, val message: String) : Failure
        data class Unauthorized(val message: String) : Failure
    }
}
