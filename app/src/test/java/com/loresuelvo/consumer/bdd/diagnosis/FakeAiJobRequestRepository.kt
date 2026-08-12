package com.loresuelvo.consumer.bdd.diagnosis

import com.loresuelvo.consumer.domain.jobrequest.AiJobRequestRepository
import com.loresuelvo.consumer.domain.jobrequest.CreateAiJobRequestOutcome
import com.loresuelvo.consumer.domain.jobrequest.JobRequest

/**
 * In-memory [AiJobRequestRepository] used by the AI diagnostic
 * chat BDD layer (scenario 11-DIA). Mirrors the discipline of
 * `FakeDiagnosisRepository`: the world seeds the next outcome
 * via [enqueueOutcome] / [enqueueFailure]; the VM consumes it
 * exactly once via the real [CreateAiJobRequestUseCase].
 *
 * Every [createAiJobRequest] call records the
 * `(conversationId, providerId)` pair so the BDD step
 * "la IA envía su propio resumen al backend para ese
 * prestador" can pin the wire contract without going through
 * `MockWebServer`.
 *
 * Default outcome: a successful `JobRequest` with the
 * canonical `conversation_id = "10"` so the navigation event
 * test stays green out of the box. Steps that want a failure
 * path can call [enqueueFailure] before the next `tap`.
 */
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
