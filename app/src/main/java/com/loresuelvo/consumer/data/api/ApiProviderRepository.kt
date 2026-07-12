package com.loresuelvo.consumer.data.api

import com.loresuelvo.consumer.domain.provider.ProviderRepository
import com.loresuelvo.consumer.domain.provider.ProvidersOutcome
import javax.inject.Inject
import javax.inject.Singleton

/**
 * STUB ONLY — replaced in the next commit by a Retrofit-backed
 * implementation. Exists so the Hilt graph compiles while we TDD the
 * domain + UI: the real wire call (`GET /providers?category_id=…`)
 * ships in the data-layer commit that follows this skeleton.
 *
 * The stub intentionally returns an empty success: never a failure
 * and never a non-empty list, so any observable list of providers
 * in the UI must come from a test fake, not from production code.
 */
@Singleton
class ApiProviderRepository @Inject constructor() : ProviderRepository {
    override suspend fun getProvidersByCategory(
        categoryId: Int,
    ): ProvidersOutcome = ProvidersOutcome.Success(emptyList())
}
