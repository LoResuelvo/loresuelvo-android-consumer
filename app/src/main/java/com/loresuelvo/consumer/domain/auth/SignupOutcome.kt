package com.loresuelvo.consumer.domain.auth

sealed interface SignupOutcome {
    data class Success(val session: AuthSession) : SignupOutcome
    data object Cancelled : SignupOutcome
    data class Failed(val message: String) : SignupOutcome
}