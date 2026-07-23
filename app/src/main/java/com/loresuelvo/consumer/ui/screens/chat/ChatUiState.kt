package com.loresuelvo.consumer.ui.screens.chat

import com.loresuelvo.consumer.domain.diagnosis.ChatMessage
import com.loresuelvo.consumer.domain.diagnosis.Recommendations

/**
 * UDF state for the AI diagnostic chat screen.
 *
 *  - [transientError]: non-null while the last send surfaced a
 *    failure the user must acknowledge. Pairs with
 *    [lastAttemptedPrompt] so the retry CTA can resubmit the
 *    same content without forcing the consumer to retype.
 *    Set in commits 04-DIA onwards; rendered as the
 *    [ChatErrorCard] in the assistant's lane.
 *  - [lastAttemptedPrompt]: snapshot of the prompt whose send
 *    landed in [transientError]. Cleared once the user retries
 *    successfully.
 *
 * The remaining fields mirror commits 01-DIA → 03-DIA.
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
) {
    val canSend: Boolean get() = promptInput.trim().isNotEmpty() && !sending
}
