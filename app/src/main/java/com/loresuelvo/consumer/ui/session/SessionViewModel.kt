package com.loresuelvo.consumer.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UDF ViewModel that mirrors the [AuthSessionStore]'s flow into a
 * Compose-friendly [SessionUiState]. Replaces the old
 * `SessionStateHolder`-based implementation (Fase 8 of the master
 * plan): the VM is now injected via Hilt and observes the
 * `sessionFlow` exposed by the production-ready
 * `EncryptedAuthSessionStore`.
 *
 * `SessionUiState.loading` is always `false` once the first
 * `sessionFlow.value` has been read at construction — auth is a
 * synchronous read, not an asynchronous one. `WelcomeViewModel`
 * owns its own loading flag for the IdP signup itself.
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionStore: AuthSessionStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(computeState(sessionStore.sessionFlow.value))
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionStore.sessionFlow.collect { session ->
                _uiState.update { computeState(session) }
            }
        }
    }

    /**
     * Clears the cached session. Called from the `Home` screen on
     * logout; the smart-router in `LoResuelvoNav` reacts to the
     * resulting null session and pops back to `Welcome`.
     */
    fun signOut() {
        sessionStore.clearSession()
    }

    private fun computeState(session: AuthSession?): SessionUiState =
        SessionUiState(loading = false, session = session)
}
