package com.loresuelvo.consumer.bdd.diagnosis

import com.loresuelvo.consumer.domain.assistant.AiConversationListOutcome
import com.loresuelvo.consumer.domain.assistant.AiConversationRepository
import com.loresuelvo.consumer.domain.assistant.AiConversationSummary

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
