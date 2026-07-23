package com.loresuelvo.consumer.bdd.diagnosis

import com.loresuelvo.consumer.domain.diagnosis.DiagnosisRepository
import com.loresuelvo.consumer.domain.diagnosis.SendDiagnosisPromptOutcome
import java.util.concurrent.atomic.AtomicReference

/**
 * In-memory [DiagnosisRepository] used by the AI diagnostic chat
 * BDD layer. The world enqueues an outcome per scenario; the VM's
 * `viewModelScope.launch` consumes it on the first
 * [sendPrompt] call.
 *
 * This fake is intentionally SIMPLE — it does not chain multiple
 * queued responses. Once an outcome is consumed, the next call
 * will throw unless a new outcome is enqueued. Future scenarios
 * (03-DIA delay, 04-DIA failure) extend the contract by wrapping
 * the body of [sendPrompt] (e.g. a `delay(...)` before returning,
 * or a `sendPrompts: Channel<Response>` for round-trip tests).
 *
 * Mirrors the pattern of `FakeUserRepository` in
 * `bdd/onboarding/registerconsumer/`.
 */
class FakeDiagnosisRepository : DiagnosisRepository {

    private val nextOutcomeRef = AtomicReference<SendDiagnosisPromptOutcome?>(null)

    /**
     * Enqueue the next outcome to be returned by [sendPrompt].
     * Replaces any previously-enqueued outcome (no queuing).
     */
    fun enqueueOutcome(outcome: SendDiagnosisPromptOutcome) {
        nextOutcomeRef.set(outcome)
    }

    override suspend fun sendPrompt(
        content: String,
        existingConversationId: String?,
    ): SendDiagnosisPromptOutcome {
        val outcome = nextOutcomeRef.getAndSet(null)
            ?: error(
                "FakeDiagnosisRepository: no outcome queued. " +
                    "Call enqueueOutcome(...) before the next send.",
            )
        return outcome
    }
}
