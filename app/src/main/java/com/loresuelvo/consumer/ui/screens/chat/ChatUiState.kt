package com.loresuelvo.consumer.ui.screens.chat

import com.loresuelvo.consumer.domain.diagnosis.ChatMessage

/**
 * UDF state for the AI diagnostic chat screen.
 *
 *  - [placeholderBody]: localisable empty-state copy shown above
 *    the messages list when there is no conversation yet.
 *  - [promptInput]: the current text in the input bar at the bottom.
 *  - [messages]: ordered history of [ChatMessage] — user-typed
 *    prompts (optimistic) followed by assistant responses (server).
 *  - [canSend]: derived; true when there is a non-blank prompt.
 *  - [sending] / [recommendations] / [conversationId] /
 *    [transientError] / [lastAttemptedPrompt] will be introduced by
 *    subsequent scenarios (02-..DIA). They live here, not in a
 *    sealed hierarchy, because the chat is identity-continuous: a
 *    single ChatUiState evolves throughout the session.
 */
data class ChatUiState(
    val placeholderBody: String = "",
    val promptInput: String = "",
    val messages: List<ChatMessage> = emptyList(),
) {
    val canSend: Boolean get() = promptInput.trim().isNotEmpty()
}
