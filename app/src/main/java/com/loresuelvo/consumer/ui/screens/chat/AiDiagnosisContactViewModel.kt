package com.loresuelvo.consumer.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loresuelvo.consumer.domain.jobrequest.CreateAiJobRequestOutcome
import com.loresuelvo.consumer.domain.provider.Provider
import com.loresuelvo.consumer.domain.usecase.jobrequest.CreateAiJobRequestUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns the AI pre-filled contact flow triggered when the user
 * taps "Contactar" on a recommended provider tile INSIDE the AI
 * diagnostic chat. Distinct from `ContactProviderViewModel`:
 *
 *  - this VM hits `POST /chatbot/conversations/{id}/job-requests`
 *    where the backend's AI pre-fills `title` and `description`
 *    from the conversation history (mirrors the webapp's
 *    `useAiDiagnosisChat.handleContactProvider`);
 *  - the user does NOT type a title or description — there is no
 *    modal in this flow.
 *
 * State machine:
 *  - `Idle` → user taps "Contactar" on [provider] with a
 *    known [conversationId] → `Submitting(provider)` →
 *    `CreateAiJobRequestUseCase` → on success, `Idle` +
 *    [AiDiagnosisContactEvent.NavigateToConversation] when the
 *    backend returned a `conversationId`; on failure, `Idle`
 *    (no typed failure UI today).
 *  - A second tap while `Submitting` is a no-op: the use case is
 *    not invoked twice. The carousel button is also disabled via
 *    `ChatRoute` as a UI guard, but the VM must defensively
 *    ignore double-taps because the UI guard is a thin surface.
 *  - `onContactProviderClick` with a blank `conversationId` is a
 *    no-op: the carousel only renders when the diagnosis
 *    concluded, and the conclusion round-trip sets a
 *    conversationId, so this case is a defensive guard for an
 *    early tap that's been accidentally wired.
 *
 * The events are exposed via a buffered `Channel` so a slow
 * collector cannot drop the navigation event. The host in
 * `ChatRoute` collects the events inside a `LaunchedEffect`
 * scoped to the navigation entry, which survives configuration
 * changes.
 */
@HiltViewModel
class AiDiagnosisContactViewModel @Inject constructor(
    private val createAiJobRequest: CreateAiJobRequestUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AiDiagnosisContactUiState>(
        AiDiagnosisContactUiState.Idle,
    )
    val uiState: StateFlow<AiDiagnosisContactUiState> = _uiState.asStateFlow()

    private val _events = Channel<AiDiagnosisContactEvent>(Channel.BUFFERED)
    val events: Flow<AiDiagnosisContactEvent> = _events.receiveAsFlow()

    fun onContactProviderClick(provider: Provider, conversationId: String?) {
        if (conversationId.isNullOrBlank()) return
        if (_uiState.value is AiDiagnosisContactUiState.Submitting) return
        _uiState.update { AiDiagnosisContactUiState.Submitting(provider) }
        viewModelScope.launch {
            when (val outcome = createAiJobRequest(conversationId, provider.id)) {
                is CreateAiJobRequestOutcome.Success -> {
                    _uiState.update { AiDiagnosisContactUiState.Idle }
                    val responseConversationId = outcome.jobRequest.conversationId
                    if (responseConversationId != null) {
                        _events.send(
                            AiDiagnosisContactEvent.NavigateToConversation(
                                responseConversationId,
                            ),
                        )
                    }
                }
                is CreateAiJobRequestOutcome.Failure -> {
                    _uiState.update { AiDiagnosisContactUiState.Idle }
                }
            }
        }
    }
}
