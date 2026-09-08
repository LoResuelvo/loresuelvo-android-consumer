package com.loresuelvo.consumer.ui.screens.proposals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
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
 *
 * **Why the [loadJob] cancellation?** Before the bug fix the VM
 * launched a fresh `viewModelScope` coroutine on every call —
 * three rapid "Ver Solicitud" taps on Home fired three parallel
 * `getServiceProposals()` round trips. Whichever completed last
 * won the [uiState] slot, so a transient failure on the second
 * round trip (rate-limit / 401 / 5xx) overwrote a successful
 * `Ready` with `Error` and the screen flashed "No pudimos cargar
 * la propuesta". Cancelling the prior job keeps at most one
 * round trip in flight and pins the contract to "the most recent
 * tap wins".
 *
 * **Why [reset]?** Before this fix the `onDismiss` callback on
 * the home / MisServicios sheets called `load("")` to clear the
 * VM. That drove the round-trip back into a state where
 * `proposalId = ""` matched no proposal and the VM emitted
 * [ProposalDetailUiState.Error]. The `LaunchedEffect` on the
 * sheet's `visible` boolean computed `visible = !isLoading`, so a
 * fresh `Error` re-opened the sheet — the user dismissed a card
 * and saw it pop back open with "No pudimos cargar la propuesta.
 * Reintentá", over and over. [reset] drives the VM back to
 * [ProposalDetailUiState.Idle] instead, which the sheet treats
 * as "stay hidden", so dismiss is truly dismiss.
 */
@HiltViewModel
class ProposalDetailViewModel @Inject constructor(
    private val serviceProposalRepository: ServiceProposalRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProposalDetailUiState>(ProposalDetailUiState.Idle)
    val uiState: StateFlow<ProposalDetailUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun load(proposalId: String) {
        if (proposalId.isBlank()) {
            // Defensive: a blank id used to drive the VM into
            // `Error(404, "Proposal  not found")`, which the
            // bottom-sheet `LaunchedEffect` treated as "show" — see
            // the [reset] KDoc for the full failure mode. Keep
            // the guard so a future host cannot reintroduce it.
            reset()
            return
        }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
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

    /**
     * Cancels any in-flight round trip and drives the VM back to
     * [ProposalDetailUiState.Idle] — the "dismiss" entry point
     * used by the host when the consumer closes the bottom sheet.
     * Idempotent; safe to call from `onDismissRequest` even
     * when no load was ever triggered.
     */
    fun reset() {
        loadJob?.cancel()
        _uiState.value = ProposalDetailUiState.Idle
    }
}
