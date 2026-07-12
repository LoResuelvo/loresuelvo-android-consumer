package com.loresuelvo.consumer.domain.usecase.provider

import com.loresuelvo.consumer.domain.provider.ProviderRepository
import com.loresuelvo.consumer.domain.provider.ProvidersOutcome
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches the providers registered against a given category through
 * the [ProviderRepository] port. Stateless; propagates the typed
 * [ProvidersOutcome] without swallowing errors. The endpoint is
 * public, so no session is required — same model as
 * [com.loresuelvo.consumer.domain.usecase.category.GetCategoriesUseCase].
 */
@Singleton
class GetProvidersByCategoryUseCase @Inject constructor(
    private val providerRepository: ProviderRepository,
) {
    suspend operator fun invoke(categoryId: Int): ProvidersOutcome =
        providerRepository.getProvidersByCategory(categoryId)
}
