package com.loresuelvo.consumer.ui.screens.chat

import com.loresuelvo.consumer.domain.diagnosis.ChatMessage
import com.loresuelvo.consumer.domain.diagnosis.DiagnosisAssessment
import com.loresuelvo.consumer.domain.provider.Provider

/**
 * UDF state for the AI diagnostic chat screen.
 *
 *  - [preliminaryWarningVisible]: always `true` while the chat is
 *    open, controls whether the orientation-preliminary banner
 *    renders above the messages. The text comes from the
 *    `chat_preliminary_warning` resource (so it tracks the
 *    active locale). Carried as a flag rather than a literal
 *    string to keep the Spanish copy out of `app/src/main/java/`
 *    per the AGENTS.md i18n rule.
 *  - [assessment]: structured AI outcome (`outcome` + matched
 *    `problem_category`). `null` while the AI is still asking
 *    follow-up questions. The UI gates the summary card on
 *    `assessment?.isProfessionalRequired == true` per the
 *    09-DIA / 10-DIA scenarios.
 *  - [recommendedProviders]: providers the AI suggests for the
 *    matched category. Populated alongside the
 *    `professional_required` outcome.
 */
data class ChatUiState(
    val placeholderBody: String = "",
    val promptInput: String = "",
    val sending: Boolean = false,
    val conversationId: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val assessment: DiagnosisAssessment? = null,
    val recommendedProviders: List<Provider>? = null,
    val transientError: ChatError? = null,
    val lastAttemptedPrompt: String? = null,
    val preliminaryWarningVisible: Boolean = true,
) {
    val canSend: Boolean get() = promptInput.trim().isNotEmpty() && !sending
}
