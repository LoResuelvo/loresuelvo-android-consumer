package com.loresuelvo.consumer.ui.screens.chat

import androidx.lifecycle.ViewModel
import com.loresuelvo.consumer.domain.diagnosis.ChatMessage
import com.loresuelvo.consumer.domain.diagnosis.Sender
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * ViewModel for the AI diagnostic chat screen.
 *
 * This commit (scenario 01-DIA) wires only the consumer side:
 *  - [onPromptChange] keeps [ChatUiState.promptInput] in sync with
 *    the input field;
 *  - [onSendClick] trims the prompt, no-ops on empty input, and
 *    appends an optimistic [ChatMessage] (sender = [Sender.Consumer],
 *    id = `user-<uuid>`) to [ChatUiState.messages], clearing the
 *    input on success.
 *
 * The server round-trip (commit 02-DIA) replaces the optimistic
 * message with the backend's full history; the error path (commit
 * 04-DIA) renders an error card; the typing indicator (commit
 * 03-DIA) flips on a `sending` flag for the duration of the call.
 * This commit deliberately does not depend on any repository or use
 * case so the BDD layer can drive the VM with no fakes.
 */
@HiltViewModel
class ChatViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(promptInput = value) }
    }

    fun onSendClick() {
        val state = _uiState.value
        val prompt = state.promptInput.trim()
        if (prompt.isEmpty()) return

        val message = ChatMessage(
            id = USER_MESSAGE_ID_PREFIX + UUID.randomUUID(),
            sender = Sender.Consumer,
            content = prompt,
            sentAtEpochMillis = System.currentTimeMillis(),
        )

        _uiState.update {
            it.copy(
                messages = state.messages + message,
                promptInput = "",
            )
        }
    }

    private companion object {
        const val USER_MESSAGE_ID_PREFIX = "user-"
    }
}
