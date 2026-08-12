package com.loresuelvo.consumer.bdd.diagnosis

import com.loresuelvo.consumer.domain.assistant.AiConversationListOutcome
import com.loresuelvo.consumer.domain.assistant.AiConversationRepository
import com.loresuelvo.consumer.domain.assistant.AiConversationSummary

/**
 * In-memory [AiConversationRepository] used by the AI diagnostic
 * chat BDD layer (scenario 12-DIA). Mirrors the discipline of
 * `FakeDiagnosisRepository` and `FakeAiJobRequestRepository`: the
 * world enqueues the next outcome via [enqueueSuccess] /
 * [enqueueFailure]; the VM consumes it exactly once via the real
 * [com.loresuelvo.consumer.domain.usecase.assistant.GetAiConversationsUseCase].
 *
 * Default outcome: a single empty list so the BDD world doesn't
 * accidentally jump to a non-default state. Scenarios that need
 * a seeded list call [enqueueSuccess] before the world starts
 * observing the Assistant VM.
 */
class FakeAiConversationRepository : AiConversationRepository {

    private var nextOutcome: AiConversationListOutcome =
        AiConversationListOutcome.Success(conversations = emptyList())

    fun enqueueSuccess(conversations: List<AiConversationSummary>) {
        nextOutcome = AiConversationListOutcome.Success(conversations = conversations)
    }

    fun enqueueFailure(failure: AiConversationListOutcome.Failure) {
        nextOutcome = failure
    }

    override suspend fun getConversations(): AiConversationListOutcome = nextOutcome
}
