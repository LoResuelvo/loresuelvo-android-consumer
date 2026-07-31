package com.loresuelvo.consumer.ui.screens.professional

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loresuelvo.consumer.domain.jobrequest.CreateJobRequestData
import com.loresuelvo.consumer.domain.jobrequest.CreateJobRequestOutcome
import com.loresuelvo.consumer.domain.provider.Provider
import com.loresuelvo.consumer.domain.usecase.jobrequest.CreateJobRequestUseCase
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
 * UDF ViewModel for the contact-provider bottom sheet.
 *
 * State machine:
 *  - `Closed` → user taps "Contactar" on a provider card →
 *    `Open(provider, "", "", false, null)`.
 *  - `Open` → user types in the fields → state mutates with
 *    the new value AND clears any prior error.
 *  - `Open` → user taps "Enviar solicitud" (gated by `canSubmit`)
 *    → `Open(... isSubmitting = true, error = null)` →
 *    `CreateJobRequestUseCase` → on success, `Closed` +
 *    [ContactProviderEvent.NavigateToConversation]; on failure,
 *    `Open(... isSubmitting = false, error = <typed>)`.
 *  - `Open` → user taps "Cancelar" or swipes the modal down →
 *    `Closed`.
 *
 * The events are exposed via a buffered `Channel` so a slow
 * collector cannot drop the navigation event. The host in
 * `LoResuelvoNav` collects them inside a `LaunchedEffect` scoped
 * to the navigation entry, which survives configuration changes.
 */
@HiltViewModel
class ContactProviderViewModel @Inject constructor(
    private val createJobRequest: CreateJobRequestUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ContactProviderUiState>(
        ContactProviderUiState.Closed,
    )
    val uiState: StateFlow<ContactProviderUiState> = _uiState.asStateFlow()

    private val _events = Channel<ContactProviderEvent>(Channel.BUFFERED)
    val events: Flow<ContactProviderEvent> = _events.receiveAsFlow()

    fun onOpenContact(provider: Provider) {
        _uiState.update { ContactProviderUiState.Open(provider) }
    }

    fun onCancel() {
        _uiState.update { ContactProviderUiState.Closed }
    }

    fun onDismissError() {
        _uiState.update { state ->
            if (state is ContactProviderUiState.Open) state.copy(error = null) else state
        }
    }

    fun onTitleChange(title: String) {
        _uiState.update { state ->
            if (state is ContactProviderUiState.Open) {
                state.copy(title = title, error = null)
            } else {
                state
            }
        }
    }

    fun onDescriptionChange(description: String) {
        _uiState.update { state ->
            if (state is ContactProviderUiState.Open) {
                state.copy(description = description, error = null)
            } else {
                state
            }
        }
    }

    fun onSubmit() {
        val state = _uiState.value
        if (state !is ContactProviderUiState.Open) return
        if (!state.canSubmit) return
        viewModelScope.launch {
            _uiState.update { state.copy(isSubmitting = true, error = null) }
            when (val outcome = createJobRequest(
                CreateJobRequestData(
                    providerId = state.provider.id,
                    title = state.title,
                    description = state.description,
                ),
            )) {
                is CreateJobRequestOutcome.Success -> {
                    val conversationId = outcome.jobRequest.conversationId
                    if (conversationId != null) {
                        _uiState.update { ContactProviderUiState.Closed }
                        _events.send(
                            ContactProviderEvent.NavigateToConversation(conversationId),
                        )
                    } else {
                        // The backend's contract guarantees a
                        // conversation_id on success. If it ever
                        // changes, surface the disagreement instead
                        // of silently dropping the user.
                        _uiState.update {
                            state.copy(
                                isSubmitting = false,
                                error = ContactProviderError.Server(
                                    code = 0,
                                    message = "Conversation ID missing in response",
                                ),
                            )
                        }
                    }
                }
                is CreateJobRequestOutcome.Failure -> {
                    _uiState.update {
                        state.copy(
                            isSubmitting = false,
                            error = outcome.toContactProviderError(),
                        )
                    }
                }
            }
        }
    }

    private fun CreateJobRequestOutcome.Failure.toContactProviderError():
        ContactProviderError = when (this) {
        is CreateJobRequestOutcome.Failure.Network -> ContactProviderError.Network
        is CreateJobRequestOutcome.Failure.Unauthorized ->
            ContactProviderError.Unauthorized
        is CreateJobRequestOutcome.Failure.Server ->
            ContactProviderError.Server(code, message)
    }
}
