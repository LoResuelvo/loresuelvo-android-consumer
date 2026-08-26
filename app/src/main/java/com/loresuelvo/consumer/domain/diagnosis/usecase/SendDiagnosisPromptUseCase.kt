package com.loresuelvo.consumer.domain.diagnosis.usecase

import com.loresuelvo.consumer.domain.diagnosis.DiagnosisRepository
import com.loresuelvo.consumer.domain.diagnosis.SendDiagnosisPromptOutcome
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Application-layer use case that wraps
 * [DiagnosisRepository.sendPrompt] with a guard rail: at least
 * one of [prompt] or [imageFileIds] must be present — the
 * consumer shouldn't be able to fire an empty round-trip.
 *
 *  - `prompt.trim().isEmpty() && imageFileIds.isEmpty()` ⇒
 *    [SendDiagnosisPromptOutcome.Failure.Server] with `code = 0`
 *    (synthetic, non-HTTP). The ViewModel also short-circuits
 *    via `canSend`; this is the defensive mirror in the domain
 *    layer.
 *  - `prompt` non-empty ⇒ delegated verbatim to the repository
 *    (text-only send).
 *  - `prompt` empty BUT [imageFileIds] non-empty ⇒ delegated
 *    verbatim (WhatsApp-style send-photo-without-text — the
 *    AI diagnostic endpoint accepts `content=""` alongside
 *    `image_file_ids[]`).
 *
 * The use case does NOT swallow typed repository failures (Network /
 * Server / Unauthorized): they propagate unchanged to the caller.
 * The single transformation it owns is the empty-payload rule.
 */
@Singleton
class SendDiagnosisPromptUseCase @Inject constructor(
    private val diagnosisRepository: DiagnosisRepository,
) {
    suspend operator fun invoke(
        prompt: String,
        existingConversationId: String? = null,
        imageFileIds: List<String> = emptyList(),
    ): SendDiagnosisPromptOutcome {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty() && imageFileIds.isEmpty()) {
            return SendDiagnosisPromptOutcome.Failure.Server(
                code = 0,
                message = "Prompt and attachments are both empty",
            )
        }
        return diagnosisRepository.sendPrompt(
            content = trimmed,
            existingConversationId = existingConversationId,
            imageFileIds = imageFileIds,
        )
    }
}
