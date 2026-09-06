package com.loresuelvo.consumer.ui.screens.misservicios

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import com.loresuelvo.consumer.ui.components.proposalcard.ProposalCard
import com.loresuelvo.consumer.ui.screens.proposals.ProposalDetailScreen
import com.loresuelvo.consumer.ui.screens.proposals.ProposalDetailUiState
import com.loresuelvo.consumer.ui.theme.SubtitleGray

/**
 * State-driven surface for the "Mis Servicios" screen
 * (`Route.MisServicios`), the consumer-facing list of every
 * service proposal regardless of status (US-54 scenario 03-VSP).
 *
 * Renders:
 *  - A horizontal row of filter chips at the top (US-54 scenario
 *    05-VSP / 06-VSP / 07-VSP) — the active chip is driven by
 *    `state.selectedStatusFilter`.
 *  - A vertical list of `ProposalCard`s for the current filter.
 *  - The standard Loading / Empty / Error states for the list.
 *  - A modal bottom sheet with the full proposal detail when the
 *    consumer taps a card (US-54 scenario 08-VSP). The sheet
 *    uses the dedicated [ProposalDetailScreen]; tapping its "Ver
 *    conversación" CTA calls [onViewConversation] which the host
 *    `LoResuelvoNav` resolves into a navigation to
 *    `Route.Conversation`.
 *
 * The host wraps the screen in a Hilt-managed
 * [com.loresuelvo.consumer.ui.screens.proposals.ProposalDetailViewModel]
 * via `hiltViewModel()` so the sheet's round trip fires when the
 * consumer taps a card. When the bottom sheet is dismissed
 * (`onDismiss`) the sheet-local VM keeps the proposal cached for
 * the next open so a second tap doesn't refetch.
 */
@Composable
fun MisServiciosScreen(
    state: MisServiciosUiState,
    detailState: ProposalDetailUiState,
    onFilterSelected: (com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus?) -> Unit = {},
    onRetryClick: () -> Unit = {},
    onProposalSelected: (proposalId: String) -> Unit = {},
    onDetailRetry: () -> Unit = {},
    onViewConversation: (conversationId: String) -> Unit = {},
    onDetailDismiss: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag(MIS_SERVICIOS_SCREEN_TAG),
    ) {
        FilterChipsRow(
            selected = state.selectedStatusFilter,
            onFilterSelected = onFilterSelected,
        )
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            when (state) {
                is MisServiciosUiState.Loading -> LoadingState()
                is MisServiciosUiState.Ready -> {
                    if (state.proposals.isEmpty()) {
                        EmptyState()
                    } else {
                        ProposalsList(
                            proposals = state.proposals,
                            onProposalSelected = onProposalSelected,
                        )
                    }
                }
                is MisServiciosUiState.Error -> ErrorState(
                    failure = state.failure,
                    onRetryClick = onRetryClick,
                )
            }
        }
    }

    // Proposal detail bottom sheet — surfaced when the consumer
    // taps any card. The host (LoResuelvoNav → MisServiciosRoute)
    // feeds the local detail VM with the chosen proposalId via
    // `onProposalSelected`, which kicks the round trip.
    DetailSheet(
        detailState = detailState,
        onRetry = onDetailRetry,
        onViewConversation = onViewConversation,
        onDismiss = onDetailDismiss,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailSheet(
    detailState: ProposalDetailUiState,
    onRetry: () -> Unit,
    onViewConversation: (conversationId: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(detailState) {
        visible = detailState !is ProposalDetailUiState.Loading
    }
    if (!visible) return
    ModalBottomSheet(
        onDismissRequest = {
            visible = false
            onDismiss()
        },
        sheetState = sheetState,
    ) {
        ProposalDetailScreen(
            state = detailState,
            onRetry = onRetry,
            onViewConversation = onViewConversation,
            onDismiss = {
                visible = false
                onDismiss()
            },
        )
    }
}

@Composable
private fun FilterChipsRow(
    selected: com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus?,
    onFilterSelected: (com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus?) -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(MIS_SERVICIOS_FILTER_CHIPS_TAG),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Inline chips to avoid an extra import block for the
        // small handful of variants we need here.
        FilterChipInline(
            label = stringResource(R.string.mis_servicios_filter_all),
            selected = selected == null,
            onClick = { onFilterSelected(null) },
            tag = MIS_SERVICIOS_FILTER_CHIP_TAG_PREFIX + "all",
        )
        FilterChipInline(
            label = stringResource(R.string.mis_servicios_filter_pending),
            selected = selected == com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus.Pending,
            onClick = { onFilterSelected(com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus.Pending) },
            tag = MIS_SERVICIOS_FILTER_CHIP_TAG_PREFIX + "pending",
        )
        FilterChipInline(
            label = stringResource(R.string.mis_servicios_filter_accepted),
            selected = selected == com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus.Accepted,
            onClick = { onFilterSelected(com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus.Accepted) },
            tag = MIS_SERVICIOS_FILTER_CHIP_TAG_PREFIX + "accepted",
        )
        FilterChipInline(
            label = stringResource(R.string.mis_servicios_filter_rejected),
            selected = selected == com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus.Rejected,
            onClick = { onFilterSelected(com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus.Rejected) },
            tag = MIS_SERVICIOS_FILTER_CHIP_TAG_PREFIX + "rejected",
        )
    }
}

@Composable
private fun FilterChipInline(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String,
) {
    androidx.compose.material3.FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label) },
        modifier = Modifier.testTag(tag),
    )
}

@Composable
private fun LoadingState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.testTag(MIS_SERVICIOS_LOADING_TAG),
        )
        Text(
            text = stringResource(R.string.mis_servicios_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = SubtitleGray,
        )
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .padding(horizontal = 32.dp)
            .testTag(MIS_SERVICIOS_EMPTY_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.mis_servicios_empty_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.mis_servicios_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = SubtitleGray,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ProposalsList(
    proposals: List<ServiceProposal>,
    onProposalSelected: (proposalId: String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(MIS_SERVICIOS_LIST_TAG),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = proposals,
            key = { it.id },
        ) { proposal ->
            ProposalCard(
                proposal = proposal,
                onViewClicked = { onProposalSelected(proposal.id) },
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
    }
}

@Composable
private fun ErrorState(
    failure: ServiceProposalsOutcome.Failure,
    onRetryClick: () -> Unit,
) {
    val message = when (failure) {
        is ServiceProposalsOutcome.Failure.Network ->
            stringResource(R.string.mis_servicios_error_network)
        is ServiceProposalsOutcome.Failure.Server ->
            stringResource(R.string.mis_servicios_error_server)
    }
    Column(
        modifier = Modifier
            .padding(horizontal = 32.dp)
            .testTag(MIS_SERVICIOS_ERROR_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxSize(),
        )
        Button(
            onClick = onRetryClick,
            modifier = Modifier.testTag(MIS_SERVICIOS_ERROR_RETRY_TAG),
        ) {
            Text(
                text = stringResource(R.string.mis_servicios_error_retry),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

/**
 * Compose testTags for the MisServicios screen and its inner slots.
 * Pinning the locators (rather than literal Spanish labels) keeps
 * the Compose tests locale-independent — the test resolves the
 * string from the activity's resources via `getString(R.string.*)`.
 */
const val MIS_SERVICIOS_SCREEN_TAG: String = "mis-servicios-screen"
const val MIS_SERVICIOS_LOADING_TAG: String = "mis-servicios-loading"
const val MIS_SERVICIOS_EMPTY_TAG: String = "mis-servicios-empty"
const val MIS_SERVICIOS_LIST_TAG: String = "mis-servicios-list"
const val MIS_SERVICIOS_ERROR_TAG: String = "mis-servicios-error"
const val MIS_SERVICIOS_ERROR_RETRY_TAG: String = "mis-servicios-error-retry"
const val MIS_SERVICIOS_FILTER_CHIPS_TAG: String = "mis-servicios-filter-chips"
const val MIS_SERVICIOS_FILTER_CHIP_TAG_PREFIX: String = "mis-servicios-filter-chip-"
