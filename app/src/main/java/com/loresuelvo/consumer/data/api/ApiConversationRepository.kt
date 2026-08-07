package com.loresuelvo.consumer.data.api

import com.loresuelvo.consumer.data.api.mapper.toDomain
import com.loresuelvo.consumer.domain.api.ApiError
import com.loresuelvo.consumer.domain.conversation.ConversationRepository
import com.loresuelvo.consumer.domain.conversation.ConversationsOutcome
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default implementation of [ConversationRepository] against the
 * backend. Adapts `GET /conversations` (Retrofit-typed via
 * [BackendApi]) to the domain's [ConversationsOutcome] hierarchy.
 *
 * Like every other adapter in this package, it never throws on
 * HTTP / network failures: every exception is translated to a
 * typed [ConversationsOutcome.Failure] via [toApiError], so
 * callers handle each branch explicitly. The 401 branch maps to
 * [ConversationsOutcome.Failure.Unauthorized] (a dedicated subtype
 * exists on this outcome — the categories endpoint collapses 401
 * into `Server(401, …)` because its outcome lacks the dedicated
 * case).
 */
@Singleton
class ApiConversationRepository @Inject constructor(
    private val backendApi: BackendApi,
) : ConversationRepository {

    override suspend fun getConversations(): ConversationsOutcome = try {
        val dtos = backendApi.getConversations()
        ConversationsOutcome.Success(dtos.map { it.toDomain() })
    } catch (t: Throwable) {
        mapGetToFailure(t)
    }

    /**
     * Detail snapshot of a single conversation. Implementation
     * lands in a follow-up commit alongside the
     * `BackendApi.getConversationById` call.
     */
    override suspend fun getConversationById(
        conversationId: String,
    ): com.loresuelvo.consumer.domain.conversation.ConversationDetailOutcome =
        throw NotImplementedError(
            "ApiConversationRepository.getConversationById not yet implemented",
        )

    /**
     * Append a consumer message to an existing conversation.
     * Implementation lands in a follow-up commit alongside
     * `BackendApi.sendMessage`.
     */
    override suspend fun sendMessage(
        conversationId: String,
        content: String,
    ): com.loresuelvo.consumer.domain.conversation.SendMessageOutcome =
        throw NotImplementedError(
            "ApiConversationRepository.sendMessage not yet implemented",
        )

    private fun mapGetToFailure(
        e: Throwable,
    ): ConversationsOutcome.Failure = when (val error = e.toApiError()) {
        is ApiError.Network ->
            ConversationsOutcome.Failure.Network(error.networkCause)
        is ApiError.Unauthorized ->
            ConversationsOutcome.Failure.Unauthorized(error.errorMessage)
        is ApiError.Server ->
            ConversationsOutcome.Failure.Server(error.code, error.errorMessage)
        is ApiError.Unknown ->
            ConversationsOutcome.Failure.Server(0, error.message ?: "Unknown error")
    }
}