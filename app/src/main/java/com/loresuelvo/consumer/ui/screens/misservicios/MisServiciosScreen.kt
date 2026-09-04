package com.loresuelvo.consumer.ui.screens.misservicios

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import com.loresuelvo.consumer.ui.theme.SubtitleGray

/**
 * State-driven surface for the "Mis Servicios" screen
 * (`Route.MisServicios`), the consumer-facing list of every
 * service proposal regardless of status (US-54 scenario 03-VSP).
 *
 * Stateless: every visible value is sourced from
 * [MisServiciosUiState] and every user action is delegated to a
 * callback. The Hilt bridge (`MisServiciosRoute` in
 * `LoResuelvoNav`) is the only place that instantiates the
 * [MisServiciosViewModel] via `hiltViewModel()`.
 *
 * State rendering:
 *  - [MisServiciosUiState.Loading] → centred spinner.
 *  - [MisServiciosUiState.Ready] with empty list → empty-state
 *    card ("no tienes propuestas aún").
 *  - [MisServiciosUiState.Ready] with non-empty list → vertical
 *    `LazyColumn` of placeholder [ProposalRow] cards. Each row
 *    carries: prestador name, rubro, monto, fecha, descripción
 *    corta y el estado como badge. The visually rich card
 *    (formatted currency, friendly date, profile photo with
 *    fallback) is the responsibility of scenario 08-VSP; the
 *    placeholder here uses raw `Long` / `epoch millis` so the
 *    surface renders deterministically before that lands.
 *  - [MisServiciosUiState.Error] → centred card with a typed
 *    copy (network / server / unauthorized) and a "Reintentar"
 *    button that triggers [onRetryClick].
 *
 * The status filter chip row sits above the list / loading /
 * error surface (US-54 scenario 05-VSP onwards). The active chip
 * is rendered with [FilterChip] default selected colors; the
 * screen keeps the chip highlighted across Loading and Error
 * transitions because [selectedStatusFilter] lives in every
 * state variant.
 *
 * The status badge colours follow the semantic palette already
 * documented on `home_section_*` — Pending = warning, Accepted =
 * success, Rejected = error. These three `Color` values are
 * reached through `MaterialTheme.colorScheme` so the surface
 * picks up the active theme without a hard-coded palette.
 */
@Composable
fun MisServiciosScreen(
    state: MisServiciosUiState,
    onFilterSelected: (ServiceProposalStatus?) -> Unit = {},
    onRetryClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            // 08-UXUI: see HomeScreen — the outer `Scaffold` no
            // longer consumes the top inset, so each bottom-nav
            // screen must apply `statusBarsPadding()` itself.
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
                        ProposalsList(proposals = state.proposals)
                    }
                }
                is MisServiciosUiState.Error -> ErrorState(
                    failure = state.failure,
                    onRetryClick = onRetryClick,
                )
            }
        }
    }
}

@Composable
private fun FilterChipsRow(
    selected: ServiceProposalStatus?,
    onFilterSelected: (ServiceProposalStatus?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onFilterSelected(null) },
            label = { Text(text = stringResource(R.string.mis_servicios_filter_all)) },
            modifier = Modifier.testTag(MIS_SERVICIOS_FILTER_CHIP_TAG_PREFIX + "all"),
        )
        FilterChip(
            selected = selected == ServiceProposalStatus.Pending,
            onClick = { onFilterSelected(ServiceProposalStatus.Pending) },
            label = { Text(text = stringResource(R.string.mis_servicios_filter_pending)) },
            modifier = Modifier.testTag(MIS_SERVICIOS_FILTER_CHIP_TAG_PREFIX + "pending"),
        )
        FilterChip(
            selected = selected == ServiceProposalStatus.Accepted,
            onClick = { onFilterSelected(ServiceProposalStatus.Accepted) },
            label = { Text(text = stringResource(R.string.mis_servicios_filter_accepted)) },
            modifier = Modifier.testTag(MIS_SERVICIOS_FILTER_CHIP_TAG_PREFIX + "accepted"),
        )
        FilterChip(
            selected = selected == ServiceProposalStatus.Rejected,
            onClick = { onFilterSelected(ServiceProposalStatus.Rejected) },
            label = { Text(text = stringResource(R.string.mis_servicios_filter_rejected)) },
            modifier = Modifier.testTag(MIS_SERVICIOS_FILTER_CHIP_TAG_PREFIX + "rejected"),
        )
    }
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
private fun ProposalsList(proposals: List<ServiceProposal>) {
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
            ProposalRow(proposal = proposal)
        }
    }
}

@Composable
private fun ProposalRow(proposal: ServiceProposal) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag(MIS_SERVICIOS_ROW_TAG_PREFIX + proposal.id),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "${proposal.counterpart.name} ${proposal.counterpart.surname}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = proposal.counterpart.categoryName,
            style = MaterialTheme.typography.bodySmall,
            color = SubtitleGray,
        )
        Text(
            text = proposal.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "$" + proposal.amountCents.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "ID " + proposal.id,
            style = MaterialTheme.typography.labelSmall,
            color = SubtitleGray,
        )
        StatusBadge(status = proposal.status)
    }
}

@Composable
private fun StatusBadge(status: ServiceProposalStatus) {
    val (label, color) = when (status) {
        ServiceProposalStatus.Pending -> stringResource(R.string.mis_servicios_status_pending) to
            MaterialTheme.colorScheme.tertiary
        ServiceProposalStatus.Accepted -> stringResource(R.string.mis_servicios_status_accepted) to
            MaterialTheme.colorScheme.primary
        ServiceProposalStatus.Rejected -> stringResource(R.string.mis_servicios_status_rejected) to
            MaterialTheme.colorScheme.error
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.testTag(MIS_SERVICIOS_BADGE_TAG),
    )
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
            modifier = Modifier.fillMaxWidth(),
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
const val MIS_SERVICIOS_BADGE_TAG: String = "mis-servicios-status-badge"
const val MIS_SERVICIOS_ROW_TAG_PREFIX: String = "mis-servicios-row-"
const val MIS_SERVICIOS_FILTER_CHIP_TAG_PREFIX: String = "mis-servicios-filter-chip-"
