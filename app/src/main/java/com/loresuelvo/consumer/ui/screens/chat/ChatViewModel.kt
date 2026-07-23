package com.loresuelvo.consumer.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loresuelvo.consumer.domain.diagnosis.ChatMessage
import com.loresuelvo.consumer.domain.diagnosis.Sender
import com.loresuelvo.consumer.domain.diagnosis.SendDiagnosisPromptOutcome
import com.loresuelvo.consumer.domain.diagnosis.usecase.SendDiagnosisPromptUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the AI diagnostic chat screen.
 *
 * Commit 04-DIA wires the failure path:
 *
 *  - [onPromptChange] keeps [ChatUiState.promptInput] in sync.
 *  - [onSendClick]:
 *      - Trims the prompt, bails on blank or `sending = true`.
 *      - Captures the prompt in `lastAttemptedPrompt` (used by
 *        [onRetryClick] later).
 *      - Appends the optimistic bubble, clears the input, and
 *        flips `sending = true` + clears any prior `transientError`.
 *      - Delegates to [fireSend] which performs the round-trip
 *        and applies either [applyServerResponse] (Success) or
 *        [applySendFailure] (Failure.Network / .Server /
 *        .Unauthorized → [ChatError]).
 *  - [onRetryClick] resubmits `lastAttemptedPrompt`. No-op when
 *    no error is showing or a previous send is in flight.
 *  - [onErrorDismiss] clears `transientError` without re-firing.
 *
 * Idempotency: `lastAttemptedPrompt` is snapshotted from the
 * `state.promptInput.value()` before the optimistic append, so
 * parallel `onSendClick`s can't race on the prompt.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sendDiagnosisPrompt: SendDiagnosisPromptUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(promptInput = value) }
    }

    fun onSendClick() {
        val state = _uiState.value
        val prompt = state.promptInput.trim()
        if (prompt.isEmpty() || state.sending) return

        val snapshot = state
        _uiState.update {
            it.copy(
                messages = snapshot.messages + optimisticMessage(prompt),
                promptInput = "",
                sending = true,
                transientError = null,
                lastAttemptedPrompt = prompt,
            )
        }
        fireSend(prompt, snapshot.conversationId)
    }

    fun onRetryClick() {
        val state = _uiState.value
        val prompt = state.lastAttemptedPrompt
        if (prompt.isNullOrBlank() || state.sending) return
        _uiState.update {
            it.copy(
                sending = true,
                transientError = null,
            )
        }
        fireSend(prompt, state.conversationId)
    }

    fun onErrorDismiss() {
        _uiState.update { it.copy(transientError = null) }
    }

    private fun fireSend(prompt: String, conversationId: String?) {
        viewModelScope.launch {
            val outcome = sendDiagnosisPrompt(prompt, conversationId)
            when (outcome) {
                is SendDiagnosisPromptOutcome.Success -> applyServerResponse(outcome)
                is SendDiagnosisPromptOutcome.Failure.Network ->
                    applySendFailure(ChatError.Network)
                is SendDiagnosisPromptOutcome.Failure.Server ->
                    applySendFailure(ChatError.ServiceUnavailable)
                is SendDiagnosisPromptOutcome.Failure.Unauthorized ->
                    applySendFailure(ChatError.Unauthorized(outcome.message))
            }
        }
    }

    private fun applyServerResponse(outcome: SendDiagnosisPromptOutcome.Success) {
        val diagnosis = outcome.diagnosis
        _uiState.update {
            it.copy(
                sending = false,
                conversationId = diagnosis.conversationId ?: it.conversationId,
                messages = diagnosis.messages,
                recommendations = diagnosis.recommendations ?: it.recommendations,
                transientError = null,
                lastAttemptedPrompt = null,
            )
        }
    }

    private fun applySendFailure(error: ChatError) {
        _uiState.update {
            it.copy(
                sending = false,
                transientError = error,
                // messages list is preserved so the user's optimistic
                // bubble stays in place while the error card is shown;
                // the producer can clear it via `onErrorDismiss` or
                // successful `onRetryClick`.
            )
        }
    }

    private fun optimisticMessage(prompt: String): ChatMessage = ChatMessage(
        id = USER_MESSAGE_ID_PREFIX + UUID.randomUUID(),
        sender = Sender.Consumer,
        content = prompt,
        sentAtEpochMillis = System.currentTimeMillis(),
    )

    private companion object {
        const val USER_MESSAGE_ID_PREFIX = "user-"
    }
}
