package com.loresuelvo.consumer.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loresuelvo.consumer.domain.conversation.ConversationDetail
import com.loresuelvo.consumer.domain.conversation.ConversationDetailOutcome
import com.loresuelvo.consumer.domain.conversation.ConversationMessage
import com.loresuelvo.consumer.domain.conversation.SendMessageOutcome
import com.loresuelvo.consumer.domain.usecase.conversation.GetConversationByIdUseCase
import com.loresuelvo.consumer.domain.usecase.conversation.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UDF ViewModel for the consumer ↔ provider conversation detail
 * screen (`Route.Conversation`). Drives `GET /conversations/{id}`
 * through [GetConversationByIdUseCase] and `POST
 * /conversations/{id}/messages` through [SendMessageUseCase],
 * mapping the typed outcomes into the sealed
 * [ConversationUiState].
 *
 * Detail loading:
 *  - `Success(detail)` → [ConversationUiState.Ready] with empty
 *    prompt and `sending = false`.
 *  - `Failure.Network / .Server / .Unauthorized` →
 *    [ConversationUiState.Error] carrying the typed failure.
 *
 * Send flow (mirrors the AI diagnostic's [ChatViewModel]):
 *  - [onPromptChange] mirrors the field on the current
 *    [ConversationUiState.Ready] and clears any prior
 *    `transientError`.
 *  - [onSendClick]:
 *      * Trims the prompt, bails on blank or `sending = true`.
 *      * Snapshots the previous state and clears the prompt +
 *        flips `sending = true` + clears the prior
 *        `transientError` synchronously.
 *      * Fires [sendMessage]; on success, appends the
 *        server-persisted message to `detail.messages` and
 *        clears `lastAttemptedPrompt`; on failure, surfaces the
 *        typed failure in `transientError` and preserves the
 *        prompt in `lastAttemptedPrompt` so [onRetryClick] can
 *        resubmit it.
 *  - [onRetryClick] re-fires `sendMessage` with
 *    `lastAttemptedPrompt`. No-op when no previous failure or a
 *    previous send is in flight.
 *  - [onErrorDismiss] clears `transientError` without re-firing
 *    (the user can still hit the retry CTA — `lastAttemptedPrompt`
 *    is kept).
 *
 * The composer is **never** gated on `ConversationStatus.Pending`
 * (scenario 05-IC: "without restrictions"). The "Pendiente" badge
 * in the top bar is informational only.
 *
 * The conversation id is provided by the host
 * ([com.loresuelvo.consumer.ui.navigation.ConversationRoute])
 * via [load] on first composition (and on screen-level retry).
 * Hilt scopes the VM to the route entry so the same instance
 * survives configuration changes.
 */
@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val getConversationById: GetConversationByIdUseCase,
    private val sendMessage: SendMessageUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ConversationUiState>(
        ConversationUiState.Loading,
    )
    val uiState: StateFlow<ConversationUiState> = _uiState.asStateFlow()

    /**
     * Loads the conversation detail for [conversationId]. Public
     * so the host can re-trigger on retry (and the host invokes
     * it once on first composition with the nav argument).
     */
    fun load(conversationId: String) {
        viewModelScope.launch {
            _uiState.update { ConversationUiState.Loading }
            val next = when (val outcome = getConversationById(conversationId)) {
                is ConversationDetailOutcome.Success ->
                    ConversationUiState.Ready(
                        detail = outcome.detail,
                        promptInput = "",
                        sending = false,
                    )
                is ConversationDetailOutcome.Failure ->
                    ConversationUiState.Error(outcome)
            }
            _uiState.update { next }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { state ->
            if (state is ConversationUiState.Ready) {
                state.copy(promptInput = value, transientError = null)
            } else {
                state
            }
        }
    }

    fun onSendClick() {
        val state = _uiState.value
        if (state !is ConversationUiState.Ready) return
        val prompt = state.promptInput.trim()
        if (prompt.isEmpty() || state.sending) return

        _uiState.update { currentState ->
            if (currentState is ConversationUiState.Ready) {
                currentState.copy(
                    promptInput = "",
                    sending = true,
                    transientError = null,
                    lastAttemptedPrompt = prompt,
                )
            } else {
                currentState
            }
        }
        fireSend(state.detail.id, prompt)
    }

    fun onRetryClick() {
        val state = _uiState.value
        if (state !is ConversationUiState.Ready) return
        val prompt = state.lastAttemptedPrompt
        if (prompt.isNullOrBlank() || state.sending) return
        _uiState.update { currentState ->
            if (currentState is ConversationUiState.Ready) {
                currentState.copy(
                    sending = true,
                    transientError = null,
                )
            } else {
                currentState
            }
        }
        fireSend(state.detail.id, prompt)
    }

    fun onErrorDismiss() {
        _uiState.update { state ->
            if (state is ConversationUiState.Ready) {
                state.copy(transientError = null)
            } else {
                state
            }
        }
    }

    private fun fireSend(conversationId: String, content: String) {
        viewModelScope.launch {
            when (val outcome = sendMessage(conversationId, content)) {
                is SendMessageOutcome.Success -> applyServerResponse(outcome.message)
                is SendMessageOutcome.Failure.Network ->
                    applySendFailure(outcome)
                is SendMessageOutcome.Failure.Server ->
                    applySendFailure(outcome)
                is SendMessageOutcome.Failure.Unauthorized ->
                    applySendFailure(outcome)
            }
        }
    }

    private fun applyServerResponse(sentMessage: ConversationMessage) {
        _uiState.update { state ->
            if (state is ConversationUiState.Ready) {
                state.copy(
                    sending = false,
                    detail = state.detail.copy(
                        messages = state.detail.messages + sentMessage,
                    ),
                    transientError = null,
                    lastAttemptedPrompt = null,
                )
            } else {
                state
            }
        }
    }

    private fun applySendFailure(failure: SendMessageOutcome.Failure) {
        _uiState.update { state ->
            if (state is ConversationUiState.Ready) {
                state.copy(
                    sending = false,
                    transientError = failure,
                    // `lastAttemptedPrompt` is preserved so the
                    // retry CTA can resubmit it.
                )
            } else {
                state
            }
        }
    }
}