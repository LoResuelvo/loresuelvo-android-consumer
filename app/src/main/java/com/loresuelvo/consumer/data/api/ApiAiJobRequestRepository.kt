package com.loresuelvo.consumer.data.api

import com.loresuelvo.consumer.data.api.mapper.createAiJobRequestRequest
import com.loresuelvo.consumer.data.api.mapper.toDomain
import com.loresuelvo.consumer.domain.api.ApiError
import com.loresuelvo.consumer.domain.jobrequest.AiJobRequestRepository
import com.loresuelvo.consumer.domain.jobrequest.CreateAiJobRequestOutcome
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default implementation of [AiJobRequestRepository] against the
 * backend's `POST /chatbot/conversations/{conversationId}/job-requests`
 * endpoint. The backend's AI pre-fills `title` and `description`
 * from the conversation history; the consumer only sends the
 * `provider_id`. The response shape is identical to the legacy
 * `POST /job-requests` endpoint, so the existing
 * `JobRequestDtoMapper.toDomain()` handles the response.
 *
 * Like the other `Api*Repository` classes, this never throws on
 * HTTP / network failures; every exception is translated to a
 * typed [CreateAiJobRequestOutcome.Failure] via [toApiError] so
 * callers handle each branch explicitly (the
 * `CreateAiJobRequestUseCase` propagates these failures
 * unchanged).
 */
@Singleton
class ApiAiJobRequestRepository @Inject constructor(
    private val backendApi: BackendApi,
) : AiJobRequestRepository {

    override suspend fun createAiJobRequest(
        conversationId: String,
        providerId: Int,
    ): CreateAiJobRequestOutcome = try {
        val response = backendApi.createAiJobRequest(
            conversationId = conversationId,
            body = createAiJobRequestRequest(providerId),
        )
        CreateAiJobRequestOutcome.Success(response.toDomain())
    } catch (t: Throwable) {
        mapToFailure(t)
    }

    private fun mapToFailure(t: Throwable): CreateAiJobRequestOutcome.Failure =
        when (val error = t.toApiError()) {
            is ApiError.Network ->
                CreateAiJobRequestOutcome.Failure.Network(error.networkCause)
            is ApiError.Unauthorized ->
                CreateAiJobRequestOutcome.Failure.Unauthorized(error.errorMessage)
            is ApiError.Server ->
                CreateAiJobRequestOutcome.Failure.Server(error.code, error.errorMessage)
            is ApiError.Unknown ->
                CreateAiJobRequestOutcome.Failure.Server(0, error.message ?: "Unknown error")
        }
}
