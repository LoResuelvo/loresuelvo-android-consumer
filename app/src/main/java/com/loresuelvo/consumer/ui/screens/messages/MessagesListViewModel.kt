package com.loresuelvo.consumer.ui.screens.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loresuelvo.consumer.domain.conversation.ConversationsOutcome
import com.loresuelvo.consumer.domain.usecase.conversation.GetConversationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MessagesListViewModel @Inject constructor(
    private val getConversations: GetConversationsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MessagesListUiState>(
        MessagesListUiState.Loading,
    )
    val uiState: StateFlow<MessagesListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /**
     * Loads the consumer's conversations list. Public so the
     * screen can re-trigger it on retry (and, in a follow-up,
     * pull-to-refresh).
     */
    fun load() {
        viewModelScope.launch {
            _uiState.update { MessagesListUiState.Loading }
            val next = when (val outcome = getConversations()) {
                is ConversationsOutcome.Success ->
                    MessagesListUiState.Ready(outcome.conversations)
                is ConversationsOutcome.Failure ->
                    MessagesListUiState.Error(outcome)
            }
            _uiState.update { next }
        }
    }
}