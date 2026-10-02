package com.loresuelvo.consumer.bdd.diagnosis

import com.loresuelvo.consumer.domain.jobrequest.AiJobRequestRepository
import com.loresuelvo.consumer.domain.jobrequest.CreateAiJobRequestOutcome
import com.loresuelvo.consumer.domain.jobrequest.JobRequest

class FakeAiJobRequestRepository : AiJobRequestRepository {

    data class RecordedCall(
        val conversationId: String,
        val providerId: Int,
    )

    private val recorded: MutableList<RecordedCall> = mutableListOf()
    private var nextOutcome: CreateAiJobRequestOutcome = CreateAiJobRequestOutcome.Success(
        JobRequest(
            id = "1",
            conversationId = "10",
            title = "Reparación de fuga en la cocina",
            description = "Hola, necesito reparar una fuga de agua.",
            status = "pending",
            images = emptyList(),
        ),
    )

    fun enqueueOutcome(outcome: CreateAiJobRequestOutcome) {
        nextOutcome = outcome
    }

    fun enqueueFailure(failure: CreateAiJobRequestOutcome.Failure) {
        enqueueOutcome(failure)
    }

    fun lastRecordedCall(): RecordedCall? =
        recorded.lastOrNull()

    fun recordedCalls(): List<RecordedCall> =
        recorded.toList()

    override suspend fun createAiJobRequest(
        conversationId: String,
        providerId: Int,
    ): CreateAiJobRequestOutcome {
        recorded += RecordedCall(conversationId, providerId)
        return nextOutcome
    }
}
