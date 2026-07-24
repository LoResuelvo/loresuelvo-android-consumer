package com.loresuelvo.consumer.ui.screens.chat

import com.loresuelvo.consumer.domain.diagnosis.ChatMessage
import com.loresuelvo.consumer.domain.diagnosis.Recommendations

/**
 * UDF state for the AI diagnostic chat screen.
 *
 *  - [preliminaryWarningVisible]: always `true` while the chat is
 *    open, controls whether the orientation-preliminary banner
 *    renders above the messages. The text comes from the
 *    `chat_preliminary_warning` resource (so it tracks the
 *    active locale). Carried as a flag rather than a literal
 *    string to keep the Spanish copy out of `app/src/main/java/`
 *    per the AGENTS.md i18n rule. Reserved for scenario 05-DIA;
 *    future commits may flip it off when the assessment returns
 *    a `self_service` outcome.
 *
 * The remaining fields are unchanged from commits 01-DIA → 04-DIA.
 */
data class ChatUiState(
    val placeholderBody: String = "",
    val promptInput: String = "",
    val sending: Boolean = false,
    val conversationId: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val recommendations: Recommendations? = null,
    val transientError: ChatError? = null,
    val lastAttemptedPrompt: String? = null,
    val preliminaryWarningVisible: Boolean = true,
) {
    val canSend: Boolean get() = promptInput.trim().isNotEmpty() && !sending
}
