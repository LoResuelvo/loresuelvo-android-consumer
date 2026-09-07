package com.loresuelvo.consumer.ui.screens.workorder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import com.loresuelvo.consumer.domain.workorder.WorkOrder
import com.loresuelvo.consumer.ui.theme.SubtitleGray
import com.loresuelvo.consumer.ui.util.CurrencyFormatter
import com.loresuelvo.consumer.ui.util.EstimatedDurationFormatter
import com.loresuelvo.consumer.ui.util.ScheduledDateFormatter

/**
 * Consumer work-order detail screen (US-54 scenario 16-VSP).
 * Mirrors [com.loresuelvo.consumer.ui.screens.proposals.ProposalDetailScreen]
 * structurally — a vertical stack of label / value rows — but
 * the data source is the [WorkOrder] type instead of the raw
 * [com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal].
 * The screen is stateless: every visible value is sourced from
 * [WorkOrderUiState] and the only user action (the "Reintentar"
 * CTA on the error branch) is delegated via [onRetry].
 *
 *  - [WorkOrderUiState.Loading] → centred spinner.
 *  - [WorkOrderUiState.Ready] → top bar + the full work-order
 *    layout (provider, category, amount, scheduled date,
 *    estimated time on site, description, status).
 *  - [WorkOrderUiState.NotFound] → not-found copy.
 *  - [WorkOrderUiState.Error] → typed copy + retry button.
 *
 * Compose testTags are exported as `WORK_ORDER_*` constants
 * so the instrumented suite can target each row without
 * depending on the localised copy.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkOrderScreen(
    state: WorkOrderUiState,
    onRetry: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize().testTag(WORK_ORDER_SCREEN_TAG),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.work_order_title),
                        modifier = Modifier.testTag(WORK_ORDER_TITLE_TAG),
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
                is WorkOrderUiState.Loading -> LoadingState()
                is WorkOrderUiState.Ready -> ReadyState(state.workOrder)
                is WorkOrderUiState.NotFound -> NotFoundState()
                is WorkOrderUiState.Error -> ErrorState(state.failure, onRetry)
            }
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
        CircularProgressIndicator(modifier = Modifier.testTag(WORK_ORDER_LOADING_TAG))
        Text(
            text = stringResource(R.string.work_order_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = SubtitleGray,
        )
    }
}

@Composable
private fun ReadyState(workOrder: WorkOrder) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .testTag(WORK_ORDER_READY_TAG),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DetailRow(
            label = stringResource(R.string.work_order_provider),
            value = workOrder.providerName,
            valueTestTag = WORK_ORDER_PROVIDER_TAG,
        )
        DetailRow(
            label = stringResource(R.string.work_order_category_name),
            value = workOrder.categoryName,
            valueTestTag = WORK_ORDER_CATEGORY_TAG,
        )
        DetailRow(
            label = stringResource(R.string.work_order_amount),
            value = CurrencyFormatter.formatAmount(workOrder.amountCents),
            valueTestTag = WORK_ORDER_AMOUNT_TAG,
        )
        DetailRow(
            label = stringResource(R.string.work_order_date),
            value = ScheduledDateFormatter.formatScheduled(workOrder.scheduledOnEpochMillis),
            valueTestTag = WORK_ORDER_DATE_TAG,
        )
        workOrder.estimatedDurationMinutes?.let { minutes ->
            DetailRow(
                label = stringResource(R.string.work_order_estimated_duration),
                value = EstimatedDurationFormatter.formatDuration(minutes),
                valueTestTag = WORK_ORDER_ESTIMATED_DURATION_TAG,
            )
        }
        DetailRow(
            label = stringResource(R.string.work_order_description),
            value = workOrder.description,
            valueTestTag = WORK_ORDER_DESCRIPTION_TAG,
        )
        DetailRow(
            label = stringResource(R.string.work_order_status),
            value = statusLabel(workOrder.status),
            valueTestTag = WORK_ORDER_STATUS_TAG,
        )
    }
}

@Composable
private fun NotFoundState() {
    Column(
        modifier = Modifier
            .padding(horizontal = 32.dp)
            .testTag(WORK_ORDER_NOT_FOUND_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.work_order_not_found),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ErrorState(failure: ServiceProposalsOutcome.Failure, onRetry: () -> Unit) {
    val message = when (failure) {
        is ServiceProposalsOutcome.Failure.Network ->
            stringResource(R.string.work_order_error_network)
        is ServiceProposalsOutcome.Failure.Server ->
            stringResource(R.string.work_order_error_server)
    }
    Column(
        modifier = Modifier
            .padding(horizontal = 32.dp)
            .testTag(WORK_ORDER_ERROR_TAG),
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
            onClick = onRetry,
            modifier = Modifier.testTag(WORK_ORDER_ERROR_RETRY_TAG),
        ) {
            Text(stringResource(R.string.work_order_error_retry))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, valueTestTag: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = SubtitleGray,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.testTag(valueTestTag),
        )
    }
}

@Composable
private fun statusLabel(status: ServiceProposalStatus): String = when (status) {
    ServiceProposalStatus.Pending ->
        stringResource(R.string.work_order_status_pending)
    ServiceProposalStatus.Accepted ->
        stringResource(R.string.work_order_status_accepted)
    ServiceProposalStatus.Rejected ->
        stringResource(R.string.work_order_status_rejected)
}

const val WORK_ORDER_SCREEN_TAG: String = "work-order-screen"
const val WORK_ORDER_TITLE_TAG: String = "work-order-title"
const val WORK_ORDER_LOADING_TAG: String = "work-order-loading"
const val WORK_ORDER_READY_TAG: String = "work-order-ready"
const val WORK_ORDER_NOT_FOUND_TAG: String = "work-order-not-found"
const val WORK_ORDER_ERROR_TAG: String = "work-order-error"
const val WORK_ORDER_ERROR_RETRY_TAG: String = "work-order-error-retry"
const val WORK_ORDER_PROVIDER_TAG: String = "work-order-provider"
const val WORK_ORDER_CATEGORY_TAG: String = "work-order-category"
const val WORK_ORDER_AMOUNT_TAG: String = "work-order-amount"
const val WORK_ORDER_DATE_TAG: String = "work-order-date"
const val WORK_ORDER_ESTIMATED_DURATION_TAG: String = "work-order-estimated-duration"
const val WORK_ORDER_DESCRIPTION_TAG: String = "work-order-description"
const val WORK_ORDER_STATUS_TAG: String = "work-order-status"