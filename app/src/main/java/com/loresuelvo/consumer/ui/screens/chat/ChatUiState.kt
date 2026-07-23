package com.loresuelvo.consumer.ui.screens.chat

/**
 * UDF state for the AI diagnostic chat screen.
 *
 * The screen is modelled as a single, identity-continuous state — the
 * conversation lives in one `ChatUiState` instance, evolving through
 * `MutableStateFlow.update {}`. Subsequent scenarios will add fields
 * (messages, recommendations, conversationId, sending flag, error,
 * last attempted prompt); this first commit only carries the
 * placeholder copy.
 */
data class ChatUiState(
    val placeholderBody: String = "",
)
