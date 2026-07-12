package com.loresuelvo.consumer.domain.provider

/**
 * Outcome of `ProviderRepository.getProvidersByCategory`. Sealed so
 * callers explicitly handle the happy path and every failure branch
 * — same shape as `CategoriesOutcome` / `UserRegistrationOutcome`.
 *
 *  - [Success]: empty list is allowed and the UI renders the "no
 *    professionals found" message (this is NOT a failure).
 *  - [Failure.Network]: transport-level error (timeout, DNS, refused).
 *  - [Failure.Server]: any non-2xx HTTP response.
 */
sealed interface ProvidersOutcome {

    data class Success(val providers: List<Provider>) : ProvidersOutcome

    sealed interface Failure : ProvidersOutcome {
        data class Network(val cause: Throwable) : Failure
        data class Server(val code: Int, val message: String) : Failure
    }
}
