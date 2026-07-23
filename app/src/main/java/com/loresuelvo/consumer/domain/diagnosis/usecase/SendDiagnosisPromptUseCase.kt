package com.loresuelvo.consumer.domain.diagnosis.usecase

import com.loresuelvo.consumer.domain.diagnosis.DiagnosisRepository
import com.loresuelvo.consumer.domain.diagnosis.SendDiagnosisPromptOutcome
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Application-layer use case that wraps
 * [DiagnosisRepository.sendPrompt] with a guard rail: an empty /
 * whitespace-only prompt must never reach the backend.
 *
 *  - `prompt.trim().isEmpty()` ⇒
 *    [SendDiagnosisPromptOutcome.Failure.Server] with `code = 0`
 *    (synthetic, non-HTTP). The ViewModel also short-circuits
 *    empty input via `canSend`; this is the defensive mirror in
 *    the domain layer.
 *  - non-empty prompt ⇒ delegated verbatim to the repository,
 *    including [existingConversationId] (used by the VM to pick
 *    between create-new and append-to-existing on the backend).
 *
 * The use case does NOT swallow typed repository failures (Network /
 * Server / Unauthorized): they propagate unchanged to the caller.
 * The single transformation it owns is the empty-prompt rule.
 */
@Singleton
class SendDiagnosisPromptUseCase @Inject constructor(
    private val diagnosisRepository: DiagnosisRepository,
) {
    suspend operator fun invoke(
        prompt: String,
        existingConversationId: String? = null,
    ): SendDiagnosisPromptOutcome {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) {
            return SendDiagnosisPromptOutcome.Failure.Server(
                code = 0,
                message = "Prompt is empty",
            )
        }
        return diagnosisRepository.sendPrompt(trimmed, existingConversationId)
    }
}
