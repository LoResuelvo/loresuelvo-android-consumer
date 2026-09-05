package com.loresuelvo.consumer.ui.screens.proposals

import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome

/**
 * UDF state for the [ProposalDetailScreen] (US-54 scenario 08-VSP).
 *
 *  - [Loading] — initial fetch in flight.
 *  - [Ready] — proposal loaded; carries the full [ServiceProposal]
 *    so the screen can render every field (foto, nombre y
 *    apellido, rubro, monto, fecha, descripción, estado).
 *  - [Error] — fetch failed; the carried failure subtype lets the
 *    screen render network vs server strings distinctly.
 *
 * `Ready.notFound` is represented as `Error(Server(404, "..."))`
 * to keep the sealed surface narrow — there is no separate
 * `NotFound` variant because the navigation contract treats 404
 * the same as any other server failure (retryable).
 */
sealed interface ProposalDetailUiState {
    data object Loading : ProposalDetailUiState
    data class Ready(val proposal: ServiceProposal) : ProposalDetailUiState
    data class Error(val failure: ServiceProposalsOutcome.Failure) : ProposalDetailUiState
}
