package com.loresuelvo.consumer.data.api

import com.loresuelvo.consumer.data.api.mapper.toDomain
import com.loresuelvo.consumer.domain.api.ApiError
import com.loresuelvo.consumer.domain.provider.ProviderProfileOutcome
import com.loresuelvo.consumer.domain.provider.ProviderProfileRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiProviderProfileRepository @Inject constructor(
    private val backendApi: BackendApi,
) : ProviderProfileRepository {

    override suspend fun getProviderProfile(providerId: Int): ProviderProfileOutcome =
        try {
            ProviderProfileOutcome.Success(backendApi.getProviderProfile(providerId).toDomain())
        } catch (e: Throwable) {
            when (val error = e.toApiError()) {
                is ApiError.Network -> ProviderProfileOutcome.Failure.Network(error.networkCause)
                is ApiError.Unauthorized ->
                    ProviderProfileOutcome.Failure.Server(401, error.errorMessage)
                is ApiError.Server ->
                    ProviderProfileOutcome.Failure.Server(error.code, error.errorMessage)
                is ApiError.Unknown ->
                    ProviderProfileOutcome.Failure.Server(0, error.message ?: "Unknown error")
            }
        }
}
