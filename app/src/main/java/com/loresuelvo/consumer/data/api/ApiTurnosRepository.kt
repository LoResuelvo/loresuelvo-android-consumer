package com.loresuelvo.consumer.data.api

import com.loresuelvo.consumer.data.api.mapper.toDomain
import com.loresuelvo.consumer.domain.api.ApiError
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.turno.TurnosOutcome
import com.loresuelvo.consumer.domain.turno.TurnosRepository
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default implementation of the [TurnosRepository] port. Adapts
 * the [BackendApi] (Retrofit-typed) `GET /work-orders` call to
 * the domain's [TurnosOutcome] hierarchy.
 *
 * Like the other `ApiXxxRepository` adapters, it translates
 * HTTP / network failures to a typed failure via `toApiError`, so the
 * [com.loresuelvo.consumer.ui.screens.turnos.TurnosViewModel]
 * handles each branch explicitly (Loading / Ready / Error).
 */
@Singleton
class ApiTurnosRepository @Inject constructor(
    private val backendApi: BackendApi,
    private val authSessionStore: AuthSessionStore,
) : TurnosRepository {

    override suspend fun getTurnos(): TurnosOutcome {
        val accessToken = authSessionStore.getSession()?.accessToken
        val authContext = RequestAuthContext(accessToken)
        return try {
            TurnosOutcome.Success(
                backendApi.getWorkOrders(authContext).toDomain(),
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

    private fun mapToFailure(error: ApiError): TurnosOutcome.Failure =
        when (error) {
            is ApiError.Network ->
                TurnosOutcome.Failure.Network(error.networkCause)
            is ApiError.Unauthorized ->
                TurnosOutcome.Failure.Server(401, error.errorMessage)
            is ApiError.Server ->
                TurnosOutcome.Failure.Server(error.code, error.errorMessage)
            is ApiError.Unknown ->
                TurnosOutcome.Failure.Server(0, error.message ?: "Unknown error")
        }
}
