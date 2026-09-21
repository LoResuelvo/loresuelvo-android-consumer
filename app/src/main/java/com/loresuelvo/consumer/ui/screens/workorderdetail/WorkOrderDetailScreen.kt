package com.loresuelvo.consumer.ui.screens.workorderdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import com.loresuelvo.consumer.domain.turno.TurnoStatus
import com.loresuelvo.consumer.domain.workorder.CompletionReport
import com.loresuelvo.consumer.domain.workorder.CompletionReportPhoto
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetail
import com.loresuelvo.consumer.domain.workorder.WorkOrderReview
import com.loresuelvo.consumer.ui.theme.SubtitleGray
import com.loresuelvo.consumer.ui.util.CurrencyFormatter
import com.loresuelvo.consumer.ui.util.EstimatedDurationFormatter
import com.loresuelvo.consumer.ui.util.ScheduledDateFormatter

/**
 * Consumer work-order detail screen (US-54 scenario 16-VSP,
 * US-27 `visualize-turns-detail`). Mirrors
 * [com.loresuelvo.consumer.ui.screens.proposals.ProposalDetailScreen]
 * structurally — a vertical stack of label / value rows — but
 * the data source is the [com.loresuelvo.consumer.domain.workorder.WorkOrderDetail]
 * type instead of the raw
 * [com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal].
 * The screen is stateless: every visible value is sourced from
 * [WorkOrderDetailUiState] and the only user action (the
 * "Reintentar" CTA on the error branch) is delegated via
 * [onRetry].
 *
 *  - [WorkOrderDetailUiState.Loading] → centred spinner.
 *  - [WorkOrderDetailUiState.Ready] → top bar + the full work-order
 *    layout (provider, category, amount, scheduled date,
 *    estimated time on site, description, status).
 *  - [WorkOrderDetailUiState.NotFound] → not-found copy.
 *  - [WorkOrderDetailUiState.Error] → typed copy + retry button.
 *
 * Compose testTags are exported as `WORK_ORDER_*` constants
 * so the instrumented suite can target each row without
 * depending on the localised copy. US-27 keeps the tag names
 * to avoid touching the instrumented suite that already
 * targets them; a future refactor commit can align them with
 * the new screen / route name.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkOrderDetailScreen(
    state: WorkOrderDetailUiState,
    onRetry: () -> Unit,
    onBackClick: () -> Unit,
    onPayNow: () -> Unit = {},
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
                is WorkOrderDetailUiState.Loading -> LoadingState()
                is WorkOrderDetailUiState.Ready -> ReadyState(state.workOrder, onPayNow)
                is WorkOrderDetailUiState.NotFound -> NotFoundState()
                is WorkOrderDetailUiState.Error -> ErrorState(state.failure, onRetry)
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
private fun ReadyState(workOrder: WorkOrderDetail, onPayNow: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .testTag(WORK_ORDER_READY_TAG),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // US-27 scenario 09-VTD: the "Pagar saldo restante" CTA
        // is the primary action when the work order is in
        // `awaiting_payment`. Renders above the counterpart row
        // so it's the first thing the consumer sees; uses the
        // filled `Button` tonal so it stands out from the rest
        // of the surface.
        if (workOrder.status == TurnoStatus.AwaitingPayment) {
            Button(
                onClick = onPayNow,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(WORK_ORDER_PAY_NOW_TAG),
            ) {
                Text(stringResource(R.string.work_order_pay_now_cta))
            }
        }
        DetailRow(
            label = stringResource(R.string.work_order_provider),
            value = "${workOrder.provider.name} ${workOrder.provider.surname}",
            valueTestTag = WORK_ORDER_PROVIDER_TAG,
        )
        DetailRow(
            label = stringResource(R.string.work_order_category_name),
            value = workOrder.provider.categoryName,
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
        // US-27 `visualize-turns-detail`: the "Fecha en que se
        // saldó el pago" row is only present once the consumer
        // clears the remaining balance (state == `paid`). The
        // `ScheduledDateFormatter` formats the same way the
        // "Fecha y hora" row does, so the consumer reads them
        // consistently.
        workOrder.paidOnEpochMillis?.let { paidOn ->
            DetailRow(
                label = stringResource(R.string.work_order_paid_on),
                value = ScheduledDateFormatter.formatScheduled(paidOn),
                valueTestTag = WORK_ORDER_PAID_ON_TAG,
            )
        }
        // US-27 scenarios 04-VTD / 05-VTD: the "Evidencia de
        // finalización" section only renders when the provider
        // filed a completion report (state in `awaiting_payment`
        // / `paid`). Photos render as a horizontal row of
        // thumbnails; tapping one opens the lightbox (the
        // lightbox hook is wired up in a follow-up commit).
        workOrder.completionReport?.let { report ->
            CompletionReportSection(
                report = report,
                onPhotoClick = { /* TODO(US-27 06-VTD): open lightbox */ },
            )
        }
        // US-27 scenarios 07-VTD / 08-VTD: the "Reseña" section
        // only renders when the consumer filed a review
        // (state == `paid`).
        workOrder.review?.let { review ->
            ReviewSection(review = review)
        }
    }
}

@Composable
private fun CompletionReportSection(
    report: CompletionReport,
    onPhotoClick: (CompletionReportPhoto) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.work_order_evidence_section_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.testTag(WORK_ORDER_EVIDENCE_SECTION_TAG),
        )
        Text(
            text = report.description,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag(WORK_ORDER_EVIDENCE_DESCRIPTION_TAG),
        )
        Text(
            text = stringResource(R.string.work_order_evidence_reported_on) +
                " " + ScheduledDateFormatter.formatScheduled(report.reportedOnEpochMillis),
            style = MaterialTheme.typography.bodySmall,
            color = SubtitleGray,
        )
        if (report.images.isNotEmpty()) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .testTag(WORK_ORDER_EVIDENCE_PHOTOS_ROW_TAG),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                report.images.forEach { photo ->
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clickable { onPhotoClick(photo) }
                            .testTag(WORK_ORDER_EVIDENCE_PHOTO_TAG_PREFIX + photo.fileId),
                    ) {
                        coil3.compose.SubcomposeAsyncImage(
                            model = photo.url,
                            contentDescription = photo.originalName,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewSection(review: WorkOrderReview) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.work_order_review_section_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.testTag(WORK_ORDER_REVIEW_SECTION_TAG),
        )
        Text(
            text = stringResource(R.string.work_order_review_rating) + ": ${review.rating}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag(WORK_ORDER_REVIEW_RATING_TAG),
        )
        Text(
            text = review.description,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag(WORK_ORDER_REVIEW_DESCRIPTION_TAG),
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
private fun statusLabel(status: TurnoStatus): String = when (status) {
    TurnoStatus.Pending ->
        stringResource(R.string.turno_status_pending)
    TurnoStatus.Confirmed ->
        stringResource(R.string.turno_status_confirmed)
    TurnoStatus.AwaitingPayment ->
        stringResource(R.string.turno_status_awaiting_payment)
    TurnoStatus.Paid ->
        stringResource(R.string.turno_status_paid)
    TurnoStatus.Finished ->
        stringResource(R.string.turno_status_finished)
    TurnoStatus.Cancelled ->
        stringResource(R.string.turno_status_cancelled)
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
const val WORK_ORDER_PAID_ON_TAG: String = "work-order-paid-on"
const val WORK_ORDER_PAY_NOW_TAG: String = "work-order-pay-now"
const val WORK_ORDER_EVIDENCE_SECTION_TAG: String = "work-order-evidence-section"
const val WORK_ORDER_EVIDENCE_DESCRIPTION_TAG: String = "work-order-evidence-description"
const val WORK_ORDER_EVIDENCE_REPORTED_ON_TAG: String = "work-order-evidence-reported-on"
const val WORK_ORDER_EVIDENCE_PHOTOS_ROW_TAG: String = "work-order-evidence-photos-row"
const val WORK_ORDER_EVIDENCE_PHOTO_TAG_PREFIX: String = "work-order-evidence-photo-"
const val WORK_ORDER_REVIEW_SECTION_TAG: String = "work-order-review-section"
const val WORK_ORDER_REVIEW_RATING_TAG: String = "work-order-review-rating"
const val WORK_ORDER_REVIEW_DESCRIPTION_TAG: String = "work-order-review-description"