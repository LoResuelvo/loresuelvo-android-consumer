package com.loresuelvo.consumer.ui.screens.chat

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * First-pass ViewModel for the AI diagnostic chat screen. Subsequent
 * scenarios (01-DIA onwards) will inject the
 * [com.loresuelvo.consumer.domain.diagnosis.usecase.SendDiagnosisPromptUseCase]
 * and start swapping optimistic messages, server responses and
 * failures. This initial commit only exposes the screen's static
 * placeholder via [uiState] so the navigation scenario (06-DIA) can
 * render the chat screen with real Compose nodes.
 */
@HiltViewModel
class ChatViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
}
