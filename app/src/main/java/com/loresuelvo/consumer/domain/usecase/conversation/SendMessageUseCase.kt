package com.loresuelvo.consumer.domain.usecase.conversation

import com.loresuelvo.consumer.domain.conversation.ConversationRepository
import com.loresuelvo.consumer.domain.conversation.SendMessageOutcome
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Application-layer use case that wraps
 * [ConversationRepository.sendMessage] with a guard rail: an
 * empty / whitespace-only message must never reach the backend.
 *
 *  - `content.trim().isEmpty()` ⇒
 *    [SendMessageOutcome.Failure.Server] with `code = 0`
 *    (synthetic, non-HTTP). The ViewModel also short-circuits
 *    empty input via its own `canSend` flag; this is the
 *    defensive mirror in the domain layer so a future caller
 *    (e.g. an in-app shortcut or a deep link) cannot bypass the
 *    rule.
 *  - non-empty content ⇒ delegated verbatim to the repository
 *    (trimmed).
 *
 * The use case does NOT swallow typed repository failures
 * (Network / Server / Unauthorized): they propagate unchanged
 * so the VM can render each branch explicitly.
 *
 * Mirrors the empty-prompt rule of
 * [com.loresuelvo.consumer.domain.diagnosis.usecase.SendDiagnosisPromptUseCase].
 */
@Singleton
class SendMessageUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
) {
    suspend operator fun invoke(
        conversationId: String,
        content: String,
    ): SendMessageOutcome {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) {
            return SendMessageOutcome.Failure.Server(
                code = 0,
                message = "Message is empty",
            )
        }
        return conversationRepository.sendMessage(conversationId, trimmed)
    }
}