package com.loresuelvo.consumer.domain.provider

sealed interface ProviderProfileOutcome {
    data class Success(val profile: ProviderProfile) : ProviderProfileOutcome

    sealed interface Failure : ProviderProfileOutcome {
        data class Network(val cause: Throwable) : Failure
        data class Server(val code: Int, val message: String) : Failure
    }
}
