package com.loresuelvo.consumer.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loresuelvo.consumer.domain.auth.AuthProvider
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.auth.SignupOutcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WelcomeViewModel(
    private val authProvider: AuthProvider,
    private val sessionStore: AuthSessionStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WelcomeUiState())
    val uiState: StateFlow<WelcomeUiState> = _uiState.asStateFlow()

    fun signup() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            when (val outcome = authProvider.signup()) {
                is SignupOutcome.Success -> {
                    sessionStore.saveSession(outcome.session)
                    _uiState.value = _uiState.value.copy(loading = false, error = null)
                }
                SignupOutcome.Cancelled -> {
                    _uiState.value = _uiState.value.copy(loading = false, error = null)
                }
                is SignupOutcome.Failed -> {
                    _uiState.value = _uiState.value.copy(loading = false, error = outcome.message)
                }
            }
        }
    }
}