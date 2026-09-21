package com.loresuelvo.consumer.ui.screens.workorderdetail

import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetail

/**
 * UDF state for the work-order detail screen (US-54 scenario
 * 16-VSP). Modelled as a sealed hierarchy so the screen renders
 * exactly one branch without boolean flags — mirrors
 * [com.loresuelvo.consumer.ui.screens.proposals.ProposalDetailUiState].
 *
 *  - [Loading] — initial fetch in flight.
 *  - [Ready] — work order loaded; the screen renders every
 *    pinned field (monto, fecha, descripción, duración estimada,
 *    estado) plus the provider's name and category.
 *  - [NotFound] — no proposal matches the given id; the screen
 *    renders a not-found copy.
 *  - [Error] — fetch failed; the typed failure lets the screen
 *    render network vs server strings distinctly and offer a
 *    retry CTA.
 */
sealed interface WorkOrderDetailUiState {
    data object Loading : WorkOrderDetailUiState
    data class Ready(val workOrder: WorkOrderDetail) : WorkOrderDetailUiState
    data object NotFound : WorkOrderDetailUiState
    data class Error(val failure: ServiceProposalsOutcome.Failure) : WorkOrderDetailUiState
}