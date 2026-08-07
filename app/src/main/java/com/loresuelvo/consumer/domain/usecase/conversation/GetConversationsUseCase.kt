package com.loresuelvo.consumer.domain.usecase.conversation

import com.loresuelvo.consumer.domain.conversation.ConversationRepository
import com.loresuelvo.consumer.domain.conversation.ConversationsOutcome
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Application-layer use case that wraps
 * [ConversationRepository.getConversations] with no transformation:
 * the repository already returns a typed [ConversationsOutcome]
 * whose exhaustive `when` branches make failures explicit. The
 * use case exists to:
 *
 *  - give the ViewModel a single, named dependency to inject (so
 *    `MessagesListViewModel` does not need to know about
 *    [ConversationRepository] directly — it could later swap to a
 *    cached / paged variant without touching the call sites);
 *  - keep the seam testable in isolation with a mockk fake of the
 *    port (see `GetConversationsUseCaseTest`).
 *
 * The use case does NOT swallow typed repository failures
 * (Network / Server / Unauthorized): they propagate unchanged so
 * the VM can decide how to render each branch.
 */
@Singleton
class GetConversationsUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
) {
    suspend operator fun invoke(): ConversationsOutcome =
        conversationRepository.getConversations()
}