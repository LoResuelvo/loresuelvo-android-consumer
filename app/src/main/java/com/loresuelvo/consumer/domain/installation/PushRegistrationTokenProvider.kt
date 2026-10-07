package com.loresuelvo.consumer.domain.installation

sealed interface PushTokenOutcome {
    data class Available(val token: String) : PushTokenOutcome
    data object ConfigurationUnavailable : PushTokenOutcome
    data class Failure(val cause: Throwable) : PushTokenOutcome
}

interface PushRegistrationTokenProvider {
    suspend fun token(): PushTokenOutcome
}
