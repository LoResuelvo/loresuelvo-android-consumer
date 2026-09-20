package com.loresuelvo.consumer.data.api

import com.loresuelvo.consumer.data.api.mapper.toDomain
import com.loresuelvo.consumer.domain.api.ApiError
import com.loresuelvo.consumer.domain.turno.TurnosOutcome
import com.loresuelvo.consumer.domain.turno.TurnosRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default implementation of the [TurnosRepository] port. Adapts
 * the [BackendApi] (Retrofit-typed) `GET /work-orders` call to
 * the domain's [TurnosOutcome] hierarchy.
 *
 * Like the other `ApiXxxRepository` adapters, it never throws
 * on HTTP / network failures: every exception is translated to
 * a typed failure via `toApiError`, so the
 * [com.loresuelvo.consumer.ui.screens.turnos.TurnosViewModel]
 * handles each branch explicitly (Loading / Ready / Error).
 */
@Singleton
class ApiTurnosRepository @Inject constructor(
    private val backendApi: BackendApi,
) : TurnosRepository {

    override suspend fun getTurnos(): TurnosOutcome =
        try {
            TurnosOutcome.Success(
                backendApi.getWorkOrders().toDomain(),
            )
        } catch (e: Throwable) {
            mapToFailure(e)
        }

    private fun mapToFailure(e: Throwable): TurnosOutcome.Failure =
        when (val error = e.toApiError()) {
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
