package com.loresuelvo.consumer.ui.screens.misservicios

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
fun MisServiciosScreen(
    state: MisServiciosUiState,
    detailState: ProposalDetailUiState,
    actions: MisServiciosScreenActions = MisServiciosScreenActions(),
    modifier: Modifier = Modifier,
) {
    var showFilters by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(MaterialTheme.colorScheme.background)
            .testTag(MIS_SERVICIOS_SCREEN_TAG),
    ) {
        Text(
            text = stringResource(R.string.mis_servicios_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )
        ServicesListControls(
            query = state.searchQuery,
            hasActiveFilter = state.selectedStatusFilter != null,
            onQueryChanged = actions.filters.onQueryChanged,
            onFilterClick = { showFilters = true },
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            when (state) {
                is MisServiciosUiState.Loading -> LoadingState()
                is MisServiciosUiState.Ready -> {
                    if (state.proposals.isEmpty()) {
                        if (state.searchQuery.isBlank()) {
                            EmptyState()
                        } else {
                            SearchEmptyState(
                                onClearSearch = { actions.filters.onQueryChanged("") },
                            )
                        }
                    } else {
                        ProposalsList(
                            proposals = state.proposals,
                            onProposalSelected = actions.proposals.onSelected,
                        )
                    }
                }
                is MisServiciosUiState.Error -> ErrorState(
                    failure = state.failure,
                    onRetryClick = actions.proposals.onRetry,
                )
            }
        }
    }

    if (showFilters) {
        ModalBottomSheet(
            onDismissRequest = { showFilters = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            FiltersSheet(
                selected = state.selectedStatusFilter,
                onFilterSelected = { selected ->
                    actions.filters.onSelected(selected)
                    showFilters = false
                },
                onClearFilters = {
                    actions.filters.onSelected(null)
                    showFilters = false
                },
            )
        }
    }

    DetailSheet(
        detailState = detailState,
        actions = actions.detail,
    )
}

@Composable
private fun DetailSheet(
    detailState: ProposalDetailUiState,
    actions: MisServiciosScreenActions.Detail,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(detailState) {
        // Loading". A post-dismiss `Error(404)` round trip used to
        // satisfy `!isLoading` and re-open the sheet behind the
        // consumer's back; routing dismiss through
        // [com.loresuelvo.consumer.ui.screens.proposals.ProposalDetailViewModel.reset]
        // keeps the state at
        // [com.loresuelvo.consumer.ui.screens.proposals.ProposalDetailUiState.Idle],
        // which this gate treats as "stay hidden".
        visible = detailState is ProposalDetailUiState.Ready ||
            detailState is ProposalDetailUiState.Error
    }
    if (!visible) return
    ProposalDetailScreen(
        state = detailState,
        onRetry = actions.onRetry,
        onViewConversation = actions.onViewConversation,
        onPayNow = actions.onPayNow,
        onDismiss = {
            visible = false
            actions.onDismiss()
        },
    )
}

@Composable
private fun ServicesListControls(
    query: String,
    hasActiveFilter: Boolean,
    onQueryChanged: (String) -> Unit,
    onFilterClick: () -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .testTag(MIS_SERVICIOS_CONTROLS_TAG),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            modifier = Modifier
                .weight(1f)
                .testTag(MIS_SERVICIOS_SEARCH_TAG),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
            singleLine = true,
            placeholder = {
                Text(stringResource(R.string.mis_servicios_search_placeholder))
            },
            leadingIcon = {
                Icon(Icons.Outlined.Search, contentDescription = null)
            },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { onQueryChanged("") }) {
                        Icon(
                            imageVector = Icons.Outlined.Clear,
                            contentDescription = stringResource(
                                R.string.mis_servicios_search_clear_content_description,
                            ),
                        )
                    }
                }
            } else {
                null
            },
        )
        Surface(
            modifier = Modifier
                .size(56.dp)
                .testTag(MIS_SERVICIOS_FILTER_BUTTON_TAG),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            color = if (hasActiveFilter) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
            border = BorderStroke(
                width = 1.dp,
                color = if (hasActiveFilter) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                },
            ),
        ) {
            IconButton(
                onClick = onFilterClick,
                modifier = Modifier.testTag(MIS_SERVICIOS_FILTER_BUTTON_ACTION_TAG),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = stringResource(
                        R.string.mis_servicios_filter_content_description,
                    ),
                    tint = if (hasActiveFilter) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        }
    }
}

@Composable
private fun FiltersSheet(
    selected: com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus?,
    onFilterSelected: (com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus?) -> Unit,
    onClearFilters: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .testTag(MIS_SERVICIOS_FILTER_SHEET_TAG),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.mis_servicios_filter_title),
            style = MaterialTheme.typography.titleLarge,
        )
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
        if (selected != null) {
            TextButton(
                onClick = onClearFilters,
                modifier = Modifier.testTag(MIS_SERVICIOS_CLEAR_FILTERS_TAG),
            ) {
                Text(stringResource(R.string.mis_servicios_clear_filters))
            }
        }
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
private fun SearchEmptyState(
    onClearSearch: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 32.dp)
            .testTag(MIS_SERVICIOS_SEARCH_EMPTY_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.mis_servicios_search_empty_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.mis_servicios_search_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = SubtitleGray,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onClearSearch) {
            Text(stringResource(R.string.mis_servicios_clear_search))
        }
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
        ServiceProposalsOutcome.Failure.AccessDenied ->
            stringResource(R.string.service_access_denied)
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
            modifier = Modifier.fillMaxWidth(),
        )
        if (failure != ServiceProposalsOutcome.Failure.AccessDenied) {
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
const val MIS_SERVICIOS_CONTROLS_TAG: String = "mis-servicios-controls"
const val MIS_SERVICIOS_SEARCH_TAG: String = "mis-servicios-search"
const val MIS_SERVICIOS_FILTER_BUTTON_TAG: String = "mis-servicios-filter-button"
const val MIS_SERVICIOS_FILTER_BUTTON_ACTION_TAG: String = "mis-servicios-filter-button-action"
const val MIS_SERVICIOS_FILTER_SHEET_TAG: String = "mis-servicios-filter-sheet"
const val MIS_SERVICIOS_CLEAR_FILTERS_TAG: String = "mis-servicios-clear-filters"
const val MIS_SERVICIOS_SEARCH_EMPTY_TAG: String = "mis-servicios-search-empty"
