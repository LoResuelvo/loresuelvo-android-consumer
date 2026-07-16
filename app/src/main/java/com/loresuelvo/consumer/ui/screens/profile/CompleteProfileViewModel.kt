package com.loresuelvo.consumer.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.auth.UserRegistrationOutcome
import com.loresuelvo.consumer.domain.usecase.auth.RegisterConsumerCommand
import com.loresuelvo.consumer.domain.usecase.auth.RegisterConsumerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UDF ViewModel for the `CompleteProfile` screen. Owns the form
 * state, drives the registration use case, and emits one-shot
 * events for navigation.
 *
 * State vs event separation follows AGENTS.md: persistent state
 * lives in [CompleteProfileUiState]; one-shot signals (navigate,
 * session cleared, …) live in [CompleteProfileEvent] and flow
 * through a buffered Channel.
 *
 * Side effects: on [UserRegistrationOutcome.Failure.Unauthorized]
 * the VM calls `AuthSessionStore.clearSession()` so the navigation
 * graph can fall back to Welcome. No other side effects.
 */
@HiltViewModel
class CompleteProfileViewModel @Inject constructor(
    private val registerConsumerUseCase: RegisterConsumerUseCase,
    private val sessionStore: AuthSessionStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CompleteProfileUiState(
            firstName = sessionStore.sessionFlow.value?.user?.firstName.orEmpty(),
            lastName = sessionStore.sessionFlow.value?.user?.lastName.orEmpty(),
        )
    )
    val uiState: StateFlow<CompleteProfileUiState> = _uiState.asStateFlow()

    private val _events = Channel<CompleteProfileEvent>(
        capacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: Flow<CompleteProfileEvent> = _events.receiveAsFlow()

    fun onFirstNameChange(value: String) {
        _uiState.update { it.copy(firstName = value, error = null) }
    }

    fun onLastNameChange(value: String) {
        _uiState.update { it.copy(lastName = value, error = null) }
    }

    fun onContinueClick() {
        val state = _uiState.value
        when {
            state.firstName.isBlank() -> {
                _uiState.update { it.copy(error = CompleteProfileError.MissingFirstName) }
                return
            }
            state.lastName.isBlank() -> {
                _uiState.update { it.copy(error = CompleteProfileError.MissingLastName) }
                return
            }
        }
        // Double-tap protection: a second click while the first
        // registration is in flight is a no-op. Locking on
        // state.loading rather than a separate flag means the
        // protection is automatically released on any completion
        // path (Success, Failure, or an unexpected throw).
        if (state.loading) return
        _uiState.update { it.copy(loading = true, error = null) }

        viewModelScope.launch {
            val outcome = registerConsumerUseCase(
                RegisterConsumerCommand(
                    firstName = state.firstName,
                    lastName = state.lastName,
                )
            )
            when (outcome) {
                is UserRegistrationOutcome.Success -> {
                    _uiState.update { it.copy(loading = false, error = null) }
                    _events.trySend(CompleteProfileEvent.NavigateToHome)
                }
                is UserRegistrationOutcome.Failure.Network -> _uiState.update {
                    it.copy(
                        loading = false,
                        error = CompleteProfileError.Network(
                            outcome.cause.message ?: "Network error"
                        ),
                    )
                }
                is UserRegistrationOutcome.Failure.Server -> _uiState.update {
                    it.copy(
                        loading = false,
                        error = CompleteProfileError.Server(
                            code = outcome.code,
                            message = outcome.message,
                        ),
                    )
                }
                is UserRegistrationOutcome.Failure.Unauthorized -> {
                    sessionStore.clearSession()
                    _uiState.update {
                        it.copy(
                            loading = false,
                            error = CompleteProfileError.Unauthorized(outcome.message),
                        )
                    }
                }
            }
        }
    }
}
