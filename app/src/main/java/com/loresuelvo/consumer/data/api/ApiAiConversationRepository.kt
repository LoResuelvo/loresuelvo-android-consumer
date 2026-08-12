package com.loresuelvo.consumer.data.api

import com.loresuelvo.consumer.data.api.mapper.toDomain
import com.loresuelvo.consumer.domain.api.ApiError
import com.loresuelvo.consumer.domain.assistant.AiConversationListOutcome
import com.loresuelvo.consumer.domain.assistant.AiConversationRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default implementation of [AiConversationRepository] against
 * the backend's `GET /chatbot/conversations` endpoint. Returns
 * the AI diagnostic conversation summaries the "Asistente IA"
 * tab renders, ordered by the backend's `updated_on` policy.
 *
 * Like the other `Api*Repository` classes, this never throws on
 * HTTP / network failures; every exception is translated to a
 * typed [AiConversationListOutcome.Failure] via [toApiError] so
 * callers handle each branch explicitly.
 */
@Singleton
class ApiAiConversationRepository @Inject constructor(
    private val backendApi: BackendApi,
) : AiConversationRepository {

    override suspend fun getConversations(): AiConversationListOutcome = try {
        val response = backendApi.getAiConversations()
        AiConversationListOutcome.Success(response.map { it.toDomain() })
    } catch (t: Throwable) {
        mapToFailure(t)
    }

    private fun mapToFailure(t: Throwable): AiConversationListOutcome.Failure =
        when (val error = t.toApiError()) {
            is ApiError.Network ->
                AiConversationListOutcome.Failure.Network(error.networkCause)
            is ApiError.Unauthorized ->
                AiConversationListOutcome.Failure.Unauthorized(error.errorMessage)
            is ApiError.Server ->
                AiConversationListOutcome.Failure.Server(error.code, error.errorMessage)
            is ApiError.Unknown ->
                AiConversationListOutcome.Failure.Server(0, error.message ?: "Unknown error")
        }
}
