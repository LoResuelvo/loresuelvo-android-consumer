package com.loresuelvo.consumer.ui.screens.chat

import com.loresuelvo.consumer.domain.diagnosis.ChatImage
import com.loresuelvo.consumer.domain.diagnosis.ChatMessage

data class MessagesListState(
    val messages: List<ChatMessage>,
    val typingIndicatorVisible: Boolean,
    val transientError: ChatError?,
)

data class MessagesListActions(
    val onRetry: () -> Unit = {},
    val onErrorDismiss: () -> Unit = {},
    val onImageClick: (ChatImage) -> Unit = {},
)
