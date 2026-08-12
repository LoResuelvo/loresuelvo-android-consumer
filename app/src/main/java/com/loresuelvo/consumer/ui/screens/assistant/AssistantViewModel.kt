package com.loresuelvo.consumer.ui.screens.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loresuelvo.consumer.domain.usecase.assistant.GetAiConversationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns the "Asistente IA" list. Loads the AI conversations on
 * construction (the screen is the bottom-bar destination the
 * user lands on via the tab; pre-loading avoids an empty
 * surface on first open) and exposes a `retry()` entry point
 * for the error state's retry CTA.
 *
 * The state branch mapping lives in
 * [AssistantUiState.toUiState] so the VM's `load()` body stays
 * a flat `dispatch on outcome` block — the VM AND the
 * extension function are independently unit-testable.
 */
@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val getConversations: GetAiConversationsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AssistantUiState>(AssistantUiState.Loading)
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() {
        load()
    }

    private fun load() {
        _uiState.update { AssistantUiState.Loading }
        viewModelScope.launch {
            val outcome = getConversations()
            _uiState.update { outcome.toUiState() }
        }
    }
}
