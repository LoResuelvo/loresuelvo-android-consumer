package com.loresuelvo.consumer.data.api

import com.loresuelvo.consumer.data.api.mapper.toDomain
import com.loresuelvo.consumer.domain.api.ApiError
import com.loresuelvo.consumer.domain.provider.ProviderRepository
import com.loresuelvo.consumer.domain.provider.ProvidersOutcome
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default implementation of the [ProviderRepository] port. Adapts
 * the [BackendApi] (Retrofit-typed) `GET /providers?category_id=…`
 * call to the domain's [ProvidersOutcome] hierarchy.
 *
 * Like [ApiCategoryRepository] and [ApiUserRepository], it never
 * throws on HTTP / network failures: every exception is translated
 * to a typed failure via [toApiError], so the [ProfessionalsViewModel]
 * handles each branch explicitly (Loading / Ready / Empty / Error).
 *
 * Empty backend responses are returned as a successful empty list;
 * the UI renders the "no professionals found" empty state for that
 * branch.
 */
@Singleton
class ApiProviderRepository @Inject constructor(
    private val backendApi: BackendApi,
) : ProviderRepository {

    override suspend fun getProvidersByCategory(
        categoryId: Int,
    ): ProvidersOutcome =
        try {
            ProvidersOutcome.Success(backendApi.getProviders(categoryId).toDomain())
        } catch (e: Throwable) {
            mapToFailure(e)
        }

    private fun mapToFailure(e: Throwable): ProvidersOutcome.Failure =
        when (val error = e.toApiError()) {
            is ApiError.Network ->
                ProvidersOutcome.Failure.Network(error.networkCause)
            is ApiError.Unauthorized ->
                ProvidersOutcome.Failure.Server(401, error.errorMessage)
            is ApiError.Server ->
                ProvidersOutcome.Failure.Server(error.code, error.errorMessage)
            is ApiError.Unknown ->
                ProvidersOutcome.Failure.Server(0, error.message ?: "Unknown error")
        }
}
