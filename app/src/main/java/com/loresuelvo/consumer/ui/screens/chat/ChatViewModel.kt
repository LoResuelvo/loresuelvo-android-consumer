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
 * Commit 02-DIA wires the full client → server round-trip:
 *
 *  1. [onPromptChange] keeps [ChatUiState.promptInput] in sync
 *     with the input field.
 *  2. [onSendClick]:
 *     - Trims [ChatUiState.promptInput] and bails on empty or
 *       `sending = true` (defensive mirror of [ChatUiState.canSend]).
 *     - Appends an optimistic [ChatMessage]
 *       (`sender = Sender.Consumer`) to [ChatUiState.messages],
 *       clears the input, and flips `sending = true`.
 *     - Launches `sendDiagnosisPrompt(prompt, conversationId)` via
 *       [viewModelScope]. On `Success`, [ChatUiState.messages] is
 *       REPLACED with the server's full history and `conversationId`
 *       is updated. On `Failure.{Network|Server|Unauthorized}`,
 *       `sending` is reset; the typed error visualisation lands
 *       in commit 04-DIA, for now the conversation stays usable.
 *
 * Idempotency: the round-trip snapshots the conversation id before
 * launching so two parallel `onSendClick` invocations don't race on
 * the path argument to the use case.
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

        val conversationIdAtLaunch = state.conversationId
        val optimistic = ChatMessage(
            id = USER_MESSAGE_ID_PREFIX + UUID.randomUUID(),
            sender = Sender.Consumer,
            content = prompt,
            sentAtEpochMillis = System.currentTimeMillis(),
        )

        _uiState.update {
            it.copy(
                messages = state.messages + optimistic,
                promptInput = "",
                sending = true,
            )
        }

        viewModelScope.launch {
            val outcome = sendDiagnosisPrompt(prompt, conversationIdAtLaunch)
            when (outcome) {
                is SendDiagnosisPromptOutcome.Success -> applyServerResponse(outcome)
                is SendDiagnosisPromptOutcome.Failure.Network -> clearSendingOnly()
                is SendDiagnosisPromptOutcome.Failure.Server -> clearSendingOnly()
                is SendDiagnosisPromptOutcome.Failure.Unauthorized -> clearSendingOnly()
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
            )
        }
    }

    private fun clearSendingOnly() {
        _uiState.update { it.copy(sending = false) }
    }

    private companion object {
        const val USER_MESSAGE_ID_PREFIX = "user-"
    }
}
