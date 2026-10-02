package com.loresuelvo.consumer.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loresuelvo.consumer.domain.usecase.serviceproposal.GetServiceProposalByConversationIdOutcome
import com.loresuelvo.consumer.domain.usecase.serviceproposal.GetServiceProposalByConversationIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ConversationProposalSummaryViewModel @Inject constructor(
    private val getServiceProposalByConversationId: GetServiceProposalByConversationIdUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ConversationProposalSummaryUiState>(
        ConversationProposalSummaryUiState.Loading,
    )
    val uiState: StateFlow<ConversationProposalSummaryUiState> = _uiState.asStateFlow()

    /**
     * Loads the proposal summary for [conversationId]. Re-entrant
     * so the host can re-fire it after a manual retry (mirrors
     * the [ConversationViewModel.load] contract). On a `NotLinked`
     * or `Failure` outcome the state collapses to [ConversationProposalSummaryUiState.Empty]
     * — the chat keeps rendering normally and the summary card
     * stays hidden.
     */
    fun load(conversationId: String) {
        viewModelScope.launch {
            _uiState.update { ConversationProposalSummaryUiState.Loading }
            val outcome = getServiceProposalByConversationId(conversationId)
            val next = when (outcome) {
                is GetServiceProposalByConversationIdOutcome.Linked ->
                    ConversationProposalSummaryUiState.Ready(outcome.proposal)
                is GetServiceProposalByConversationIdOutcome.NotLinked ->
                    ConversationProposalSummaryUiState.Empty
                is GetServiceProposalByConversationIdOutcome.Failure ->
                    ConversationProposalSummaryUiState.Empty
            }
            _uiState.update { next }
        }
    }
}