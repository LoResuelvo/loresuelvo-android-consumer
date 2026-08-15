package com.loresuelvo.consumer.data.api

import com.loresuelvo.consumer.data.api.dto.SendMessageRequestDto
import com.loresuelvo.consumer.data.api.mapper.toDomain
import com.loresuelvo.consumer.domain.api.ApiError
import com.loresuelvo.consumer.domain.conversation.ConversationDetailOutcome
import com.loresuelvo.consumer.domain.conversation.ConversationRepository
import com.loresuelvo.consumer.domain.conversation.ConversationsOutcome
import com.loresuelvo.consumer.domain.conversation.MediaReference
import com.loresuelvo.consumer.domain.conversation.MediaUpload
import com.loresuelvo.consumer.domain.conversation.SendMessageOutcome
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Default implementation of [ConversationRepository] against the
 * backend. Adapts the three consumer ↔ provider conversation
 * endpoints (`GET /conversations`, `GET /conversations/{id}`,
 * `POST /conversations/{id}/messages`) to the domain's typed
 * outcome hierarchies.
 *
 * Like every other adapter in this package, it never throws on
 * HTTP / network failures: every exception is translated to a
 * typed `Failure` via [toApiError], so callers handle each
 * branch explicitly. The 401 branch maps to each outcome's
 * dedicated `Unauthorized` subtype so the VM can clear the local
 * session when the JWT expires.
 */
@Singleton
class ApiConversationRepository @Inject constructor(
    private val backendApi: BackendApi,
) : ConversationRepository {

    override suspend fun getConversations(): ConversationsOutcome = try {
        val dtos = backendApi.getConversations()
        ConversationsOutcome.Success(dtos.map { it.toDomain() })
    } catch (t: Throwable) {
        mapConversationsFailure(t)
    }

    override suspend fun getConversationById(
        conversationId: String,
    ): ConversationDetailOutcome = try {
        val dto = backendApi.getConversationById(conversationId)
        ConversationDetailOutcome.Success(dto.toDomain())
    } catch (t: Throwable) {
        mapDetailFailure(t)
    }

    override suspend fun sendMessage(
        conversationId: String,
        content: String,
    ): SendMessageOutcome = try {
        val dto = backendApi.postMessage(
            conversationId,
            SendMessageRequestDto(content = content),
        )
        SendMessageOutcome.Success(dto.toDomain())
    } catch (t: Throwable) {
        mapSendFailure(t)
    }

    override suspend fun sendMediaMessage(
        conversationId: String,
        media: MediaUpload,
    ): SendMessageOutcome = try {
        val part = MultipartBody.Part.createFormData(
            name = "file",
            filename = media.originalName,
            body = media.bytes.toRequestBody(
                contentType = media.mimeType.toMediaTypeOrNull(),
            ),
        )
        val dto = backendApi.postMessageWithMedia(
            conversationId = conversationId,
            file = part,
            content = null,
        )
        SendMessageOutcome.Success(dto.toDomain())
    } catch (t: Throwable) {
        mapSendFailure(t)
    }

    private fun mapConversationsFailure(
        t: Throwable,
    ): ConversationsOutcome.Failure = when (val error = t.toApiError()) {
        is ApiError.Network ->
            ConversationsOutcome.Failure.Network(error.networkCause)
        is ApiError.Unauthorized ->
            ConversationsOutcome.Failure.Unauthorized(error.errorMessage)
        is ApiError.Server ->
            ConversationsOutcome.Failure.Server(error.code, error.errorMessage)
        is ApiError.Unknown ->
            ConversationsOutcome.Failure.Server(0, error.message ?: "Unknown error")
    }

    private fun mapDetailFailure(
        t: Throwable,
    ): ConversationDetailOutcome.Failure = when (val error = t.toApiError()) {
        is ApiError.Network ->
            ConversationDetailOutcome.Failure.Network(error.networkCause)
        is ApiError.Unauthorized ->
            ConversationDetailOutcome.Failure.Unauthorized(error.errorMessage)
        is ApiError.Server ->
            ConversationDetailOutcome.Failure.Server(error.code, error.errorMessage)
        is ApiError.Unknown ->
            ConversationDetailOutcome.Failure.Server(0, error.message ?: "Unknown error")
    }

    private fun mapSendFailure(
        t: Throwable,
    ): SendMessageOutcome.Failure = when (val error = t.toApiError()) {
        is ApiError.Network ->
            SendMessageOutcome.Failure.Network(error.networkCause)
        is ApiError.Unauthorized ->
            SendMessageOutcome.Failure.Unauthorized(error.errorMessage)
        is ApiError.Server ->
            SendMessageOutcome.Failure.Server(error.code, error.errorMessage)
        is ApiError.Unknown ->
            SendMessageOutcome.Failure.Server(0, error.message ?: "Unknown error")
    }
}