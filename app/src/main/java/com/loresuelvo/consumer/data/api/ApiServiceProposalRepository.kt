package com.loresuelvo.consumer.data.api

import com.loresuelvo.consumer.data.api.mapper.toDomain
import com.loresuelvo.consumer.domain.api.ApiError
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default implementation of the [ServiceProposalRepository] port.
 * Adapts the [BackendApi] (Retrofit-typed) `GET /service-proposals`
 * call to the domain's [ServiceProposalsOutcome] hierarchy.
 *
 * Like [ApiCategoryRepository] and [ApiProviderRepository], it
 * never throws on HTTP / network failures: every exception is
 * translated to a typed failure via [toApiError], so callers
 * (and ultimately [com.loresuelvo.consumer.ui.screens.home.HomeViewModel])
 * handle each branch explicitly (Loading / Ready / Error).
 *
 * `ApiError.Unauthorized` collapses to `Failure.Server(401, …)`
 * for symmetry with the other repositories: the domain does not
 * need to expose the transport-level distinction, and the Home
 * dashboard does not need to clear the local session — the smart
 * router in `LoResuelvoNav` reacts to session changes
 * independently.
 *
 * The list-level mapper drops proposals whose wire status is
 * unknown (see [toDomain]); those never cross the repository
 * boundary, so the domain invariant "every proposal has a
 * recognised status" holds.
 */
@Singleton
class ApiServiceProposalRepository @Inject constructor(
    private val backendApi: BackendApi,
) : ServiceProposalRepository {

    override suspend fun getServiceProposals(): ServiceProposalsOutcome =
        try {
            ServiceProposalsOutcome.Success(
                backendApi.getServiceProposals().toDomain(),
            )
        } catch (e: Throwable) {
            mapToFailure(e)
        }

    private fun mapToFailure(e: Throwable): ServiceProposalsOutcome.Failure =
        when (val error = e.toApiError()) {
            is ApiError.Network ->
                ServiceProposalsOutcome.Failure.Network(error.networkCause)
            is ApiError.Unauthorized ->
                ServiceProposalsOutcome.Failure.Server(401, error.errorMessage)
            is ApiError.Server ->
                ServiceProposalsOutcome.Failure.Server(error.code, error.errorMessage)
            is ApiError.Unknown ->
                ServiceProposalsOutcome.Failure.Server(0, error.message ?: "Unknown error")
        }
}
