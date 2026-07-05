package com.loresuelvo.consumer.ui.session

import androidx.lifecycle.ViewModel
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionViewModel(
    private val sessionStore: AuthSessionStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState(loading = true))
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = computeState(sessionStore.getSession())
    }

    fun refresh() {
        _uiState.value = computeState(sessionStore.getSession())
    }

    private fun computeState(session: AuthSession?): SessionUiState {
        return SessionUiState(
            loading = false,
            authenticated = session != null,
            profileCompleted = session?.user?.isProfileComplete() == true,
        )
    }
}