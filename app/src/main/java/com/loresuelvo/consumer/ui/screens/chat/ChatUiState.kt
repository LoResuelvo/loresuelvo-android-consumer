package com.loresuelvo.consumer.ui.screens.chat

import com.loresuelvo.consumer.domain.diagnosis.ChatMessage
import com.loresuelvo.consumer.domain.diagnosis.Recommendations

/**
 * UDF state for the AI diagnostic chat screen.
 *
 *  - [promptInput]: current text in the bottom composer.
 *  - [sending]: `true` while the round-trip to the backend is in
 *    flight. Gates the send action and surfaces the typing
 *    indicator (scenario 03-DIA).
 *  - [conversationId]: backend-issued id after the first
 *    successful round-trip; `null` until then. Drives the create
 *    vs append decision in `ChatViewModel.onSendClick`.
 *  - [messages]: ordered history. The local optimistic append is
 *    REPLACED by the server's full history once the round-trip
 *    succeeds, so this list is always source-of-truth.
 *  - [recommendations]: optional assessment + providers block;
 *    populated when the assistant concludes the diagnosis (09-DIA
 *    onwards). Stays `null` for the in-flight scenarios.
 *  - [canSend]: derived; true when there's a non-blank prompt and
 *    the VM is not mid-round-trip.
 */
data class ChatUiState(
    val promptInput: String = "",
    val sending: Boolean = false,
    val conversationId: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val recommendations: Recommendations? = null,
) {
    val canSend: Boolean get() = promptInput.trim().isNotEmpty() && !sending
}
