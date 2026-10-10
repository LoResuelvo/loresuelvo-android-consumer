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

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var cachedConversations: List<com.loresuelvo.consumer.domain.conversation.Conversation> = emptyList()
    private var loadRequestId = 0L

    init {
        load()
    }

    /**
     * Loads the consumer's conversations list. Public so the
     * screen can re-trigger it on retry.
     */
    fun load() {
        val requestId = ++loadRequestId
        viewModelScope.launch {
            _uiState.update { MessagesListUiState.Loading }
            when (val outcome = getConversations()) {
                is ConversationsOutcome.Success -> {
                    if (requestId != loadRequestId) return@launch
                    cachedConversations = outcome.conversations
                        .sortedByDescending { it.updatedOnEpochMillis }
                    _uiState.update {
                        filteredState(cachedConversations, _searchQuery.value)
                    }
                }
                is ConversationsOutcome.Failure -> {
                    if (requestId != loadRequestId) return@launch
                    _uiState.update { MessagesListUiState.Error(outcome) }
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        _uiState.update { current ->
            if (current is MessagesListUiState.Ready) {
                filteredState(cachedConversations, query)
            } else {
                current
            }
        }
    }

    private fun filteredState(
        conversations: List<com.loresuelvo.consumer.domain.conversation.Conversation>,
        query: String,
    ): MessagesListUiState.Ready {
        val normalizedQuery = query.trim()
        val filtered = if (normalizedQuery.isBlank()) {
            conversations
        } else {
            conversations.filter { conversation ->
                val providerName = listOf(
                    conversation.counterpart.name,
                    conversation.counterpart.surname,
                ).joinToString(" ")
                val preview = conversation.lastMessage?.content.orEmpty()
                providerName.contains(normalizedQuery, ignoreCase = true) ||
                    preview.contains(normalizedQuery, ignoreCase = true)
            }
        }
        return MessagesListUiState.Ready(
            conversations = filtered,
            searchQuery = query,
            totalConversations = conversations.size,
        )
    }
}
