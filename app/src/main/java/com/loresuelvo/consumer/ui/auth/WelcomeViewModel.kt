package com.loresuelvo.consumer.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loresuelvo.consumer.domain.auth.AuthProvider
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.auth.SignupOutcome
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UDF ViewModel for the `Welcome` screen. Drives the IdP signup flow
 * via [AuthProvider] and persists the result on [AuthSessionStore].
 *
 * The Activity [Context] is supplied per-call by the Composable
 * (via `LocalContext.current`) rather than captured at construction
 * time, so the VM is `@HiltViewModel`-clean.
 */
@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val authProvider: AuthProvider,
    private val sessionStore: AuthSessionStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WelcomeUiState())
    val uiState: StateFlow<WelcomeUiState> = _uiState.asStateFlow()

    fun signup(activityContext: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            val outcome = authProvider.signup(activityContext)
            when (outcome) {
                is SignupOutcome.Success -> {
                    sessionStore.saveSession(outcome.session)
                    _uiState.update { it.copy(loading = false, error = null) }
                }
                SignupOutcome.Cancelled -> {
                    _uiState.update { it.copy(loading = false, error = null) }
                }
                is SignupOutcome.Failed -> {
                    _uiState.update {
                        it.copy(loading = false, error = outcome.message)
                    }
                }
            }
        }
    }
}
