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
 * Three enqueue modes are exposed:
 *
 *  - [enqueueOutcome] — returns the given outcome on the next call
 *    (happy path used by 01-DIA / 02-DIA).
 *  - [enqueueHangingResponse] — suspends [sendPrompt] forever
 *    (`awaitCancellation`), simulating a slow backend. Used by
 *    03-DIA to keep `state.sending = true` while we assert that
 *    the UI gating logic disables the next send.
 *
 * Each call consumes its enqueued state once; subsequent calls
 * without a fresh enqueue throw an `error("…")` so BDD failures
 * surface loud rather than silently reusing stale state. Future
 * scenarios that need to chain multiple round-trips (03-DIA's
 * `Then` phase for the "second message") seed a fresh outcome
 * before each `tapSend()`.
 *
 * Mirrors the pattern of `FakeUserRepository` in
 * `bdd/onboarding/registerconsumer/`.
 */
class FakeDiagnosisRepository : DiagnosisRepository {

    private val nextOutcomeRef = AtomicReference<SendDiagnosisPromptOutcome?>(null)
    private val hangModeRef = AtomicReference(false)

    /**
     * Enqueue the next outcome to be returned by [sendPrompt].
     * Replaces any previously-enqueued outcome (no queuing).
     */
    fun enqueueOutcome(outcome: SendDiagnosisPromptOutcome) {
        nextOutcomeRef.set(outcome)
        hangModeRef.set(false)
    }

    /**
     * 03-DIA: enqueue a response that never arrives. The next
     * [sendPrompt] call suspends indefinitely, mirroring a backend
     * that takes too long to reply.
     */
    fun enqueueHangingResponse() {
        nextOutcomeRef.set(null)
        hangModeRef.set(true)
    }

    override suspend fun sendPrompt(
        content: String,
        existingConversationId: String?,
    ): SendDiagnosisPromptOutcome {
        if (hangModeRef.getAndSet(false)) {
            kotlinx.coroutines.awaitCancellation()
        }
        val outcome = nextOutcomeRef.getAndSet(null)
            ?: error(
                "FakeDiagnosisRepository: no outcome queued. " +
                    "Call enqueueOutcome(...) before the next send.",
            )
        return outcome
    }
}
