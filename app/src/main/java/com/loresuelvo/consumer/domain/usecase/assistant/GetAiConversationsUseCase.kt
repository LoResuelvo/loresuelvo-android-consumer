package com.loresuelvo.consumer.domain.usecase.assistant

import com.loresuelvo.consumer.domain.assistant.AiConversationListOutcome
import com.loresuelvo.consumer.domain.assistant.AiConversationRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads the list of AI diagnostic conversations for the
 * "Asistente IA" tab. Procedurally a thin pass-through to
 * [AiConversationRepository.getConversations] today, but the
 * use case layer keeps the convention in this repo: every
 * repository call goes through a use case so the future
 * "filter by diagnosis concluded" / "sort by assistant-clicked
 * first" / "truncate to the last N" rules have a single, testable
 * home.
 *
 * The repository's typed failures (Network / Server / Unauthorized)
 * propagate unchanged.
 */
@Singleton
class GetAiConversationsUseCase @Inject constructor(
    private val repository: AiConversationRepository,
) {
    suspend operator fun invoke(): AiConversationListOutcome =
        repository.getConversations()
}
