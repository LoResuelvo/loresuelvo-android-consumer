package com.loresuelvo.consumer.ui.screens.turnos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.turno.Turno
import com.loresuelvo.consumer.domain.turno.TurnoStatus
import com.loresuelvo.consumer.domain.turno.TurnosOutcome
import com.loresuelvo.consumer.domain.auth.CalendarConnectionStatus
import com.loresuelvo.consumer.ui.components.turnocard.TurnoCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TurnosScreen(
    state: TurnosUiState,
    onRetryClick: () -> Unit,
    onTurnoCardClick: (turnoId: String) -> Unit = {},
    onFilterSelected: (TurnoStatus?) -> Unit = {},
    onQueryChanged: (String) -> Unit = {},
    onCalendarConnectionClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var showCalendarBanner by rememberSaveable { mutableStateOf(true) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag(TURNOS_SCREEN_TAG),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.turnos_title),
                        modifier = Modifier.testTag(TURNOS_TITLE_TAG),
                    )
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            when (state) {
                is TurnosUiState.Loading -> LoadingState()
                is TurnosUiState.Ready ->
                    ReadyState(
                        turnos = state.turnos,
                        searchQuery = state.searchQuery,
                        selectedStatusFilter = state.selectedStatusFilter,
                        onQueryChanged = onQueryChanged,
                        onFilterClick = { showFilters = true },
                        calendarConnectionStatus = state.calendarConnectionStatus,
                        showCalendarBanner = showCalendarBanner,
                        onDismissCalendarBanner = { showCalendarBanner = false },
                        onTurnoCardClick = onTurnoCardClick,
                        onCalendarConnectionClick = onCalendarConnectionClick,
                    )
                is TurnosUiState.Error -> ErrorState(
                    failure = state.failure,
                    onRetryClick = onRetryClick,
                )
            }
        }
    }

    if (showFilters) {
        ModalBottomSheet(
            onDismissRequest = { showFilters = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            TurnosFiltersSheet(
                selected = state.selectedStatusFilter,
                onFilterSelected = { selected ->
                    onFilterSelected(selected)
                    showFilters = false
                },
                onClearFilters = {
                    onFilterSelected(null)
                    showFilters = false
                },
            )
        }
    }
}

@Composable
private fun ReadyState(
    turnos: List<Turno>,
    searchQuery: String,
    selectedStatusFilter: TurnoStatus?,
    onQueryChanged: (String) -> Unit,
    onFilterClick: () -> Unit,
    calendarConnectionStatus: CalendarConnectionStatus,
    showCalendarBanner: Boolean,
    onDismissCalendarBanner: () -> Unit,
    onTurnoCardClick: (turnoId: String) -> Unit,
    onCalendarConnectionClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (showCalendarBanner && calendarConnectionStatus != CalendarConnectionStatus.CONNECTED) {
            CalendarSyncBanner(
                status = calendarConnectionStatus,
                onConnectionClick = onCalendarConnectionClick,
                onDismiss = onDismissCalendarBanner,
            )
        }
        TurnosListControls(
            query = searchQuery,
            hasActiveFilter = selectedStatusFilter != null,
            onQueryChanged = onQueryChanged,
            onFilterClick = onFilterClick,
        )
        if (turnos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                if (searchQuery.isBlank()) {
                    EmptyState()
                } else {
                    SearchEmptyState(onClearSearch = { onQueryChanged("") })
                }
            }
        } else {
            ReadyList(turnos, onTurnoCardClick)
        }
    }
}

@Composable
private fun CalendarSyncBanner(
    status: CalendarConnectionStatus,
    onConnectionClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (status == CalendarConnectionStatus.UNKNOWN) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .testTag(CALENDAR_SYNC_BANNER_TAG),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = when (status) {
                        CalendarConnectionStatus.CONNECTED -> stringResource(R.string.turnos_calendar_connected)
                        CalendarConnectionStatus.DISCONNECTED -> stringResource(R.string.turnos_calendar_disconnected)
                        CalendarConnectionStatus.ACTION_REQUIRED -> stringResource(R.string.turnos_calendar_action_required)
                        CalendarConnectionStatus.UNKNOWN -> return@Column
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag(CALENDAR_SYNC_DISMISS_TAG),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.turnos_calendar_dismiss),
                    )
                }
            }
            if (status != CalendarConnectionStatus.CONNECTED) {
                Button(
                    onClick = onConnectionClick,
                    modifier = Modifier.testTag(CALENDAR_SYNC_ACTION_TAG),
                ) {
                    Text(
                        stringResource(
                            if (status == CalendarConnectionStatus.ACTION_REQUIRED) {
                                R.string.turnos_calendar_reauthorize
                            } else {
                                R.string.turnos_calendar_connect
                            },
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun TurnosListControls(
    query: String,
    hasActiveFilter: Boolean,
    onQueryChanged: (String) -> Unit,
    onFilterClick: () -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .testTag(TURNOS_CONTROLS_TAG),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            modifier = Modifier
                .weight(1f)
                .testTag(TURNOS_SEARCH_TAG),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
            singleLine = true,
            placeholder = {
                Text(stringResource(R.string.turnos_search_placeholder))
            },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { onQueryChanged("") }) {
                        Icon(
                            imageVector = Icons.Outlined.Clear,
                            contentDescription = stringResource(R.string.turnos_search_clear_content_description),
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
                .testTag(TURNOS_FILTER_BUTTON_TAG),
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
                modifier = Modifier.testTag(TURNOS_FILTER_BUTTON_ACTION_TAG),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = stringResource(R.string.turnos_filter_content_description),
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
private fun TurnosFiltersSheet(
    selected: TurnoStatus?,
    onFilterSelected: (TurnoStatus?) -> Unit,
    onClearFilters: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .testTag(TURNOS_FILTER_SHEET_TAG),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.turnos_filter_title),
            style = MaterialTheme.typography.titleLarge,
        )
        TurnoFilterChip(
            label = stringResource(R.string.mis_servicios_filter_all),
            selected = selected == null,
            onClick = { onFilterSelected(null) },
            tag = TURNOS_FILTER_CHIP_TAG_PREFIX + "all",
        )
        TurnoStatus.entries.forEach { status ->
            TurnoFilterChip(
                label = turnoStatusLabel(status),
                selected = selected == status,
                onClick = { onFilterSelected(status) },
                tag = TURNOS_FILTER_CHIP_TAG_PREFIX + status.name.lowercase(),
            )
        }
        if (selected != null) {
            TextButton(
                onClick = onClearFilters,
                modifier = Modifier.testTag(TURNOS_CLEAR_FILTERS_TAG),
            ) {
                Text(stringResource(R.string.turnos_clear_filters))
            }
        }
    }
}

@Composable
private fun TurnoFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String,
) {
    androidx.compose.material3.FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = Modifier.testTag(tag),
    )
}

@Composable
private fun turnoStatusLabel(status: TurnoStatus): String = stringResource(
    when (status) {
        TurnoStatus.Pending -> R.string.turno_status_pending
        TurnoStatus.Confirmed -> R.string.turno_status_confirmed
        TurnoStatus.AwaitingPayment -> R.string.turno_status_awaiting_payment
        TurnoStatus.Paid -> R.string.turno_status_paid
        TurnoStatus.Finished -> R.string.turno_status_finished
        TurnoStatus.Cancelled -> R.string.turno_status_cancelled
    },
)

@Composable
private fun SearchEmptyState(onClearSearch: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .testTag(TURNOS_SEARCH_EMPTY_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.turnos_search_empty_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.turnos_search_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onClearSearch) {
            Text(stringResource(R.string.turnos_clear_search))
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator(modifier = Modifier.testTag(TURNOS_LOADING_TAG))
        Text(
            text = stringResource(R.string.turnos_loading),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .padding(horizontal = 32.dp)
            .testTag(TURNOS_EMPTY_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.turnos_empty_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.turnos_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ErrorState(
    failure: TurnosOutcome.Failure,
    onRetryClick: () -> Unit,
) {
    val message = when (failure) {
        is TurnosOutcome.Failure.Network ->
            stringResource(R.string.turnos_error_network)
        is TurnosOutcome.Failure.Server ->
            stringResource(R.string.turnos_error_server)
    }
    Column(
        modifier = Modifier
            .padding(horizontal = 32.dp)
            .testTag(TURNOS_ERROR_TAG),
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
            modifier = Modifier.testTag(TURNOS_ERROR_RETRY_TAG),
        ) {
            Text(stringResource(R.string.turnos_error_retry))
        }
    }
}

@Composable
private fun ReadyList(
    turnos: List<Turno>,
    onTurnoCardClick: (turnoId: String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(TURNOS_LIST_TAG),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = turnos, key = { it.id }) { turno ->
            TurnoCard(
                turno = turno,
                onDetailsClick = { onTurnoCardClick(turno.id) },
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
    }
}

const val TURNOS_SCREEN_TAG: String = "turnos-screen"
const val TURNOS_TITLE_TAG: String = "turnos-title"
const val TURNOS_LOADING_TAG: String = "turnos-loading"
const val TURNOS_LIST_TAG: String = "turnos-list"
const val TURNOS_EMPTY_TAG: String = "turnos-empty"
const val TURNOS_ERROR_TAG: String = "turnos-error"
const val TURNOS_ERROR_RETRY_TAG: String = "turnos-error-retry"
const val CALENDAR_SYNC_BANNER_TAG: String = "turnos-calendar-sync-banner"
const val CALENDAR_SYNC_ACTION_TAG: String = "turnos-calendar-sync-action"
const val CALENDAR_SYNC_DISMISS_TAG: String = "turnos-calendar-sync-dismiss"
const val TURNOS_CONTROLS_TAG: String = "turnos-controls"
const val TURNOS_SEARCH_TAG: String = "turnos-search"
const val TURNOS_FILTER_BUTTON_TAG: String = "turnos-filter-button"
const val TURNOS_FILTER_BUTTON_ACTION_TAG: String = "turnos-filter-button-action"
const val TURNOS_FILTER_SHEET_TAG: String = "turnos-filter-sheet"
const val TURNOS_FILTER_CHIP_TAG_PREFIX: String = "turnos-filter-chip-"
const val TURNOS_CLEAR_FILTERS_TAG: String = "turnos-clear-filters"
const val TURNOS_SEARCH_EMPTY_TAG: String = "turnos-search-empty"
