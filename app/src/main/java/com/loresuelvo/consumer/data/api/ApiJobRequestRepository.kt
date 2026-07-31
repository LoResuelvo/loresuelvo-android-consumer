package com.loresuelvo.consumer.data.api

import com.loresuelvo.consumer.data.api.mapper.toDomain
import com.loresuelvo.consumer.data.api.mapper.toDto
import com.loresuelvo.consumer.domain.api.ApiError
import com.loresuelvo.consumer.domain.jobrequest.CreateJobRequestData
import com.loresuelvo.consumer.domain.jobrequest.CreateJobRequestOutcome
import com.loresuelvo.consumer.domain.jobrequest.JobRequestRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default implementation of [JobRequestRepository] against the
 * backend's `POST /job-requests` endpoint. Adapts the
 * Retrofit-typed [BackendApi] to the domain's
 * [CreateJobRequestOutcome] hierarchy.
 *
 * Like the other `Api*Repository` classes, this never throws on
 * HTTP / network failures; every exception is translated to a
 * typed failure via [toApiError] so callers handle each branch
 * explicitly (the [CreateJobRequestUseCase] propagates these
 * failures unchanged).
 */
@Singleton
class ApiJobRequestRepository @Inject constructor(
    private val backendApi: BackendApi,
) : JobRequestRepository {

    override suspend fun createJobRequest(
        data: CreateJobRequestData,
    ): CreateJobRequestOutcome = try {
        CreateJobRequestOutcome.Success(
            backendApi.createJobRequest(data.toDto()).toDomain(),
        )
    } catch (e: Throwable) {
        mapToFailure(e)
    }

    private fun mapToFailure(e: Throwable): CreateJobRequestOutcome.Failure =
        when (val error = e.toApiError()) {
            is ApiError.Network ->
                CreateJobRequestOutcome.Failure.Network(error.networkCause)
            is ApiError.Unauthorized ->
                CreateJobRequestOutcome.Failure.Unauthorized(error.errorMessage)
            is ApiError.Server ->
                CreateJobRequestOutcome.Failure.Server(error.code, error.errorMessage)
            is ApiError.Unknown ->
                CreateJobRequestOutcome.Failure.Server(0, error.message ?: "Unknown error")
        }
}
