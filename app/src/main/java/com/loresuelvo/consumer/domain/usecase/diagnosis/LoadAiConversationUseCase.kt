package com.loresuelvo.consumer.domain.diagnosis

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads a saved AI diagnostic conversation from the backend so
 * the [com.loresuelvo.consumer.ui.screens.chat.ChatViewModel] can
 * hydrate the chat scroll when the consumer taps a row in the
 * "Asistente IA" tab. Distinct from `SendDiagnosisPromptUseCase`:
 * the latter appends a new prompt to an existing conversation;
 * this one pulls the full conversation history without sending
 * anything.
 *
 * Mirrors the validation discipline of the other AI use cases:
 *  - `conversationId` is non-blank — the backend rejects empty
 *    values; the use case short-circuits with a typed
 *    [LoadAiConversationOutcome.Failure.Server] (`code = 0`,
 *    synthetic non-HTTP) before the round-trip.
 *  - The repository's typed failures (Network / Server /
 *    Unauthorized) propagate unchanged.
 */
@Singleton
class LoadAiConversationUseCase @Inject constructor(
    private val repository: DiagnosisRepository,
) {
    suspend operator fun invoke(conversationId: String): LoadAiConversationOutcome {
        if (conversationId.isBlank()) {
            return LoadAiConversationOutcome.Failure.Server(
                code = 0,
                message = "Conversation id is required",
            )
        }
        return repository.getAiConversation(conversationId)
    }
}
