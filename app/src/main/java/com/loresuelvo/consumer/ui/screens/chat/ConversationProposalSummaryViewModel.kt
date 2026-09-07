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

/**
 * ViewModel for the proposal-summary card on the consumer ↔
 * provider conversation screen (US-54 scenario 14-VSP). Looks up
 * the [com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal]
 * tied to the conversation id via
 * [GetServiceProposalByConversationIdUseCase] and exposes a
 * sealed [ConversationProposalSummaryUiState].
 *
 * **Why a dedicated VM, not the existing
 * [ProposalDetailViewModel]?** The proposal detail screen is
 * driven by a modal bottom sheet mounted from MisServicios with a
 * proposal id; the conversation summary is driven by a
 * conversation id, has a different lifecycle (re-mounted on every
 * conversation entry), and renders a different layout (compact
 * card, no photo, no CTA). Sharing the VM would conflate two
 * distinct surfaces.
 *
 * **Failure handling.** A [GetServiceProposalByConversationIdOutcome.Failure]
 * collapses to [ConversationProposalSummaryUiState.Empty] rather
 * than surfacing an error card: the proposal summary is opt-in
 * and the chat must keep working even when the proposal fetch
 * drops. The BDD pins this contract by checking the chat renders
 * normally on failure.
 *
 * The host ([com.loresuelvo.consumer.ui.navigation.ConversationRoute])
 * feeds the conversation id into [load] on first composition. The
 * VM is Hilt-scoped to the route entry, so navigating to a
 * different conversation triggers a fresh instance and a fresh
 * round trip.
 */
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