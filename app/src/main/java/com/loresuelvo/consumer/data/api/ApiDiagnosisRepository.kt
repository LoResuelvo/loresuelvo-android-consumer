package com.loresuelvo.consumer.data.api

import com.loresuelvo.consumer.data.api.dto.CreateConversationRequestDto
import com.loresuelvo.consumer.data.api.dto.SendMessageRequestDto
import com.loresuelvo.consumer.data.api.mapper.toDomain
import com.loresuelvo.consumer.domain.api.ApiError
import com.loresuelvo.consumer.domain.diagnosis.DiagnosisRepository
import com.loresuelvo.consumer.domain.diagnosis.SendDiagnosisPromptOutcome
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default implementation of [DiagnosisRepository] against the
 * backend. Routes `existingConversationId == null` to
 * `POST /chatbot/conversations` (create) and non-null to
 * `POST /chatbot/conversations/{id}/messages` (append).
 *
 * Never throws on HTTP / network failures: every exception is
 * translated to a typed [SendDiagnosisPromptOutcome.Failure] via
 * [toApiError], so callers can handle each branch explicitly.
 * Mirrors the failure-mapping discipline of `ApiCategoryRepository`
 * and `ApiUserRepository`.
 */
@Singleton
class ApiDiagnosisRepository @Inject constructor(
    private val backendApi: BackendApi,
) : DiagnosisRepository {

    override suspend fun sendPrompt(
        content: String,
        existingConversationId: String?,
    ): SendDiagnosisPromptOutcome = try {
        val dto = if (existingConversationId == null) {
            backendApi.createConversation(
                CreateConversationRequestDto(content = content),
            )
        } else {
            backendApi.sendMessage(
                conversationId = existingConversationId,
                body = SendMessageRequestDto(content = content),
            )
        }
        SendDiagnosisPromptOutcome.Success(dto.toDomain())
    } catch (t: Throwable) {
        mapSendToFailure(t)
    }

    private fun mapSendToFailure(
        e: Throwable,
    ): SendDiagnosisPromptOutcome.Failure = when (val error = e.toApiError()) {
        is ApiError.Network ->
            SendDiagnosisPromptOutcome.Failure.Network(error.networkCause)
        is ApiError.Unauthorized ->
            SendDiagnosisPromptOutcome.Failure.Unauthorized(error.errorMessage)
        is ApiError.Server ->
            SendDiagnosisPromptOutcome.Failure.Server(error.code, error.errorMessage)
        is ApiError.Unknown ->
            SendDiagnosisPromptOutcome.Failure.Server(0, error.message ?: "Unknown error")
    }
}
