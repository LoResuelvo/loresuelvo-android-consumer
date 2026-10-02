package com.loresuelvo.consumer.ui.screens.misservicios

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus
import com.loresuelvo.consumer.ui.screens.proposals.ProposalDetailUiState

@Composable
fun MisServiciosScreen(
    state: MisServiciosUiState,
    detailState: ProposalDetailUiState,
    onFilterSelected: (ServiceProposalStatus?) -> Unit = {},
    onRetryClick: () -> Unit = {},
    onProposalSelected: (String) -> Unit = {},
    onDetailRetry: () -> Unit = {},
    onViewConversation: (String) -> Unit = {},
    onPayNow: (String) -> Unit = {},
    onDetailDismiss: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    MisServiciosScreen(
        state = state,
        detailState = detailState,
        actions = MisServiciosScreenActions(
            filters = MisServiciosScreenActions.Filters(onSelected = onFilterSelected),
            proposals = MisServiciosScreenActions.Proposals(
                onRetry = onRetryClick,
                onSelected = onProposalSelected,
            ),
            detail = MisServiciosScreenActions.Detail(
                onRetry = onDetailRetry,
                onViewConversation = onViewConversation,
                onPayNow = onPayNow,
                onDismiss = onDetailDismiss,
            ),
        ),
        modifier = modifier,
    )
}
