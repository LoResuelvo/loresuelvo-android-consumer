package com.loresuelvo.consumer.domain.auth

/**
 * Port for user-related persistence operations. Implemented in
 * `data/api/ApiUserRepository.kt` against the backend. No Android,
 * no Retrofit, no JSON — just the contract the use cases depend on.
 */
interface UserRepository {

    /**
     * Registers the authenticated user as a consumer in the backend.
     * Returns a [UserRegistrationOutcome] with the updated local
     * [User] on success, or a typed [UserRegistrationOutcome.Failure]
     * on any backend or transport error. Implementations must not
     * throw on HTTP / network failures; they translate to outcomes.
     */
    suspend fun registerConsumer(data: RegisterConsumerData): UserRegistrationOutcome
}
