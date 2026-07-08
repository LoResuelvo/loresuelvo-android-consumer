package com.loresuelvo.consumer.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loresuelvo.consumer.data.auth.SessionStateHolder
import com.loresuelvo.consumer.domain.auth.AuthSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SessionViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(computeState(SessionStateHolder.state.value))
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            SessionStateHolder.state.collect { session ->
                _uiState.value = computeState(session)
            }
        }
    }

    private fun computeState(session: AuthSession?): SessionUiState {
        return SessionUiState(
            loading = false,
            authenticated = session != null,
            profileCompleted = session?.user?.isProfileComplete() == true,
        )
    }
}