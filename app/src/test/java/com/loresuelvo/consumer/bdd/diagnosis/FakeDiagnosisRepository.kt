package com.loresuelvo.consumer.bdd.diagnosis

import com.loresuelvo.consumer.domain.diagnosis.DiagnosisRepository
import com.loresuelvo.consumer.domain.diagnosis.LoadAiConversationOutcome
import com.loresuelvo.consumer.domain.diagnosis.SendDiagnosisPromptOutcome
import java.util.concurrent.atomic.AtomicReference

class FakeDiagnosisRepository : DiagnosisRepository {

    private val nextOutcomeRef = AtomicReference<SendDiagnosisPromptOutcome?>(null)
    private val nextLoadOutcomeRef = AtomicReference<LoadAiConversationOutcome?>(null)
    private val hangModeRef = AtomicReference(false)
    private var lastImageFileIds: List<String> = emptyList()

    /**
     * Enqueue the next outcome to be returned by [sendPrompt].
     * Replaces any previously-enqueued outcome (no queuing).
     */
    fun enqueueOutcome(outcome: SendDiagnosisPromptOutcome) {
        nextOutcomeRef.set(outcome)
        hangModeRef.set(false)
    }

    fun enqueueFailure(failure: SendDiagnosisPromptOutcome.Failure) {
        enqueueOutcome(failure)
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
        imageFileIds: List<String>,
    ): SendDiagnosisPromptOutcome {
        lastImageFileIds = imageFileIds
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

    fun lastImageFileIdsSnapshot(): List<String> = lastImageFileIds.toList()

    /**
     * Seed the outcome for the next [getAiConversation] call
     * (used by the resume-AI-session flow). Mirrors
     * [enqueueOutcome]'s "consumes its enqueued state once" rule.
     */
    fun enqueueLoadOutcome(outcome: LoadAiConversationOutcome) {
        nextLoadOutcomeRef.set(outcome)
    }

    override suspend fun getAiConversation(
        conversationId: String,
    ): LoadAiConversationOutcome {
        val outcome = nextLoadOutcomeRef.getAndSet(null)
            ?: error(
                "FakeDiagnosisRepository: no load outcome queued. " +
                    "Call enqueueLoadOutcome(...) before the next get.",
            )
        return outcome
    }
}
