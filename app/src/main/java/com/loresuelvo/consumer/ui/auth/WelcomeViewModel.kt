package com.loresuelvo.consumer.ui.auth

import androidx.lifecycle.ViewModel
import com.loresuelvo.consumer.domain.auth.AuthProvider
import com.loresuelvo.consumer.domain.auth.AuthSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WelcomeViewModel(
    private val authProvider: AuthProvider,
    private val onAuthenticated: (AuthSession) -> Unit,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WelcomeUiState())
    val uiState: StateFlow<WelcomeUiState> = _uiState.asStateFlow()

    init {
        authProvider.onAuthenticated = { session ->
            _uiState.value = _uiState.value.copy(loading = false, error = null)
            onAuthenticated(session)
        }

        authProvider.onAuthenticationError = { message ->
            _uiState.value = _uiState.value.copy(loading = false, error = message)
        }
    }

    fun signup() {
        _uiState.value = _uiState.value.copy(loading = true, error = null)
        authProvider.signup()
    }
}