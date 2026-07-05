package com.loresuelvo.consumer.ui.auth

import androidx.lifecycle.ViewModel
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CompleteProfileViewModel(
    private val sessionStore: AuthSessionStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CompleteProfileUiState(
            firstName = sessionStore.sessionFlow.value?.user?.firstName.orEmpty(),
            lastName = sessionStore.sessionFlow.value?.user?.lastName.orEmpty(),
        )
    )
    val uiState: StateFlow<CompleteProfileUiState> = _uiState.asStateFlow()

    fun onFirstNameChange(value: String) {
        _uiState.value = _uiState.value.copy(firstName = value, error = null)
    }

    fun onLastNameChange(value: String) {
        _uiState.value = _uiState.value.copy(lastName = value, error = null)
    }

    fun onContinueClick() {
        val state = _uiState.value

        when {
            state.firstName.isBlank() -> {
                _uiState.value = state.copy(error = "El nombre es obligatorio")
                return
            }
            state.lastName.isBlank() -> {
                _uiState.value = state.copy(error = "El apellido es obligatorio")
                return
            }
        }

        val currentSession = sessionStore.sessionFlow.value ?: return
        val updatedSession = currentSession.copy(
            user = currentSession.user.copy(
                firstName = state.firstName,
                lastName = state.lastName,
            )
        )

        sessionStore.saveSession(updatedSession)
    }
}