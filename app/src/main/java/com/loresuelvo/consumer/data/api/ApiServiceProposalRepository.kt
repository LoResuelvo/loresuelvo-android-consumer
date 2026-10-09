package com.loresuelvo.consumer.data.api

import com.loresuelvo.consumer.data.api.mapper.toDomain
import com.loresuelvo.consumer.domain.api.ApiError
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

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
 * Unauthorized resource lookups clear the same local session that
 * supplied the request bearer, allowing the root router to return
 * to Welcome without clearing a replacement account.
 *
 * The list-level mapper drops proposals whose wire status is
 * unknown (see [toDomain]); those never cross the repository
 * boundary, so the domain invariant "every proposal has a
 * recognised status" holds.
 */
@Singleton
class ApiServiceProposalRepository @Inject constructor(
    private val backendApi: BackendApi,
    private val authSessionStore: AuthSessionStore,
) : ServiceProposalRepository {

    override suspend fun getServiceProposals(): ServiceProposalsOutcome {
        val accessToken = authSessionStore.getSession()?.accessToken
        val authContext = RequestAuthContext(accessToken)
        return try {
            ServiceProposalsOutcome.Success(
                backendApi.getServiceProposals(authContext).toDomain(),
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Throwable) {
            val error = e.toApiError()
            if (error is ApiError.Unauthorized && accessToken != null) {
                authSessionStore.clearSessionIfTokenMatches(accessToken)
            }
            mapToFailure(error)
        }
    }

    private fun mapToFailure(error: ApiError): ServiceProposalsOutcome.Failure =
        when (error) {
            is ApiError.Network ->
                ServiceProposalsOutcome.Failure.Network(error.networkCause)
            is ApiError.Unauthorized ->
                ServiceProposalsOutcome.Failure.Server(401, error.errorMessage)
            is ApiError.Server -> when (error.code) {
                403 -> ServiceProposalsOutcome.Failure.AccessDenied
                else -> ServiceProposalsOutcome.Failure.Server(error.code, error.errorMessage)
            }
            is ApiError.Unknown ->
                ServiceProposalsOutcome.Failure.Server(0, error.message ?: "Unknown error")
        }
}
