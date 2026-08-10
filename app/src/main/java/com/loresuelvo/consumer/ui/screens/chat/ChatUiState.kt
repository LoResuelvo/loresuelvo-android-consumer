package com.loresuelvo.consumer.ui.screens.chat

import com.loresuelvo.consumer.domain.diagnosis.ChatMessage
import com.loresuelvo.consumer.domain.provider.Provider

data class ChatUiState(
    val placeholderBody: String = "",
    val promptInput: String = "",
    val sending: Boolean = false,
    val conversationId: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val assessment: String? = null,
    val recommendedProviders: List<Provider>? = null,
    val transientError: ChatError? = null,
    val lastAttemptedPrompt: String? = null,
    val preliminaryWarningVisible: Boolean = true,
) {
    val canSend: Boolean get() = promptInput.trim().isNotEmpty() && !sending
}

