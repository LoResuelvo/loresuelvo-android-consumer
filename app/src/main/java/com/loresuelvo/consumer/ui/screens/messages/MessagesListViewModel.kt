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

/**
 * UDF ViewModel for the consumer's conversations list screen
 * (`Route.Messages`). Drives `GET /conversations` through
 * [GetConversationsUseCase] and maps the typed [ConversationsOutcome]
 * into the sealed [MessagesListUiState].
 *
 *  - `Success(list)` → [MessagesListUiState.Ready] (the list may
 *    be empty; the screen renders an empty-state card in that
 *    case).
 *  - `Failure.Network / .Server / .Unauthorized` →
 *    [MessagesListUiState.Error] carrying the typed failure so
 *    the screen can render network vs server vs unauthorized
 *    copy distinctly.
 *
 * The first load fires from [init] so the screen never has to
 * dispatch it explicitly (`LoResuelvoNav.MessagesRoute` does not
 * call any method on the VM). Pull-to-refresh lands in a follow-up
 * commit; for now [load] is exposed as the public retry path.
 * No in-flight guard yet — out of scope for scenario 03-IC.
 */
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