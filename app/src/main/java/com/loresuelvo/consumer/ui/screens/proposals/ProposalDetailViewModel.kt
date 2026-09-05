package com.loresuelvo.consumer.ui.screens.proposals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the [ProposalDetailScreen] (US-54 scenario
 * 08-VSP). Loads a single proposal by id from the
 * [ServiceProposalRepository] and exposes a [ProposalDetailUiState]
 * for the screen to render.
 *
 * The fetch is one-shot: the screen mounts the VM and the VM
 * kicks off `load(proposalId)` synchronously. If the consumer
 * later requests the same VM instance for a different id (rare;
 * the only realistic path is `BottomSheet` reuse), [load] can be
 * called again to re-fetch.
 */
@HiltViewModel
class ProposalDetailViewModel @Inject constructor(
    private val serviceProposalRepository: ServiceProposalRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProposalDetailUiState>(ProposalDetailUiState.Loading)
    val uiState: StateFlow<ProposalDetailUiState> = _uiState.asStateFlow()

    fun load(proposalId: String) {
        viewModelScope.launch {
            _uiState.update { ProposalDetailUiState.Loading }
            val outcome = serviceProposalRepository.getServiceProposals()
            _uiState.update {
                when (outcome) {
                    is ServiceProposalsOutcome.Success -> {
                        val match = outcome.proposals.firstOrNull { it.id == proposalId }
                        if (match != null) {
                            ProposalDetailUiState.Ready(match)
                        } else {
                            ProposalDetailUiState.Error(
                                ServiceProposalsOutcome.Failure.Server(
                                    code = 404,
                                    message = "Proposal $proposalId not found",
                                ),
                            )
                        }
                    }
                    is ServiceProposalsOutcome.Failure ->
                        ProposalDetailUiState.Error(outcome)
                }
            }
        }
    }
}
