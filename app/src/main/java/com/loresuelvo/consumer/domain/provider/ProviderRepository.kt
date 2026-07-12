package com.loresuelvo.consumer.domain.provider

/**
 * Port for the public catalog of service providers. Implemented in
 * `data/api/ApiProviderRepository.kt` against the backend's
 * `GET /providers?category_id=…` endpoint. No Android, no Retrofit,
 * no JSON — just the contract the use cases depend on.
 *
 * Empty backend responses are returned as a successful list; that
 * path renders as the "empty state" in the UI, not as a failure.
 * Implementations must not throw on HTTP / network problems; they
 * translate to a [ProvidersOutcome.Failure].
 */
interface ProviderRepository {

    suspend fun getProvidersByCategory(categoryId: Int): ProvidersOutcome
}
