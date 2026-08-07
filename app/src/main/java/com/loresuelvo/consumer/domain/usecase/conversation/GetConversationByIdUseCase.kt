package com.loresuelvo.consumer.domain.usecase.conversation

import com.loresuelvo.consumer.domain.conversation.ConversationDetailOutcome
import com.loresuelvo.consumer.domain.conversation.ConversationRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Application-layer use case that wraps
 * [ConversationRepository.getConversationById] with no
 * transformation: the repository already returns a typed
 * [ConversationDetailOutcome] whose exhaustive `when` branches
 * make failures explicit.
 *
 * Mirrors [GetConversationsUseCase]: the use case exists to give
 * the [com.loresuelvo.consumer.ui.screens.chat.ConversationViewModel]
 * a single, named dependency to inject and to keep the seam
 * testable in isolation with a mockk fake of the port (see
 * `GetConversationByIdUseCaseTest`).
 *
 * Typed repository failures (Network / Server / Unauthorized)
 * propagate unchanged so the VM can decide how to render each
 * branch.
 */
@Singleton
class GetConversationByIdUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
) {
    suspend operator fun invoke(
        conversationId: String,
    ): ConversationDetailOutcome =
        conversationRepository.getConversationById(conversationId)
}