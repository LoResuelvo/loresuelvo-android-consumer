package com.loresuelvo.consumer.ui.screens.workorderdetail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.loresuelvo.consumer.ui.components.images.FullScreenImageViewer
import com.loresuelvo.consumer.ui.screens.professional.ProviderAvatar
import com.loresuelvo.consumer.ui.theme.SubtitleGray
import com.loresuelvo.consumer.ui.util.CurrencyFormatter
import com.loresuelvo.consumer.ui.util.EstimatedDurationFormatter
import com.loresuelvo.consumer.ui.util.ScheduledDateFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkOrderDetailScreen(
    state: WorkOrderDetailUiState,
    actions: WorkOrderDetailActions = WorkOrderDetailActions(),
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = actions.onDismiss)
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
    )

    ModalBottomSheet(
        onDismissRequest = actions.onDismiss,
        sheetState = sheetState,
        modifier = modifier.testTag(WORK_ORDER_SCREEN_TAG),
    ) {
        var lightboxPhoto by remember { mutableStateOf<CompletionReportPhoto?>(null) }

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            when (state) {
                is WorkOrderDetailUiState.Loading -> LoadingState()
                is WorkOrderDetailUiState.Ready -> ReadyState(
                    workOrder = state.workOrder,
                    composer = state.composer,
                    onPhotoClick = { lightboxPhoto = it },
                    actions = actions,
                )
                is WorkOrderDetailUiState.NotFound -> NotFoundState()
                is WorkOrderDetailUiState.Error -> ErrorState(state.failure, actions.onRetry)
            }
            lightboxPhoto?.let { photo ->
                FullScreenImageViewer(
                    imageUrl = photo.url,
                    imageName = photo.originalName,
                    onDismiss = { lightboxPhoto = null },
                )
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
private fun ReadyState(
    workOrder: WorkOrderDetail,
    composer: ReviewComposerState,
    onPhotoClick: (CompletionReportPhoto) -> Unit,
    actions: WorkOrderDetailActions,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .testTag(WORK_ORDER_READY_TAG),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.work_order_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.testTag(WORK_ORDER_TITLE_TAG),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProviderAvatar(
                name = workOrder.provider.name,
                profilePhotoUrl = workOrder.provider.profilePhotoUrl,
                size = 56.dp,
                testTag = WORK_ORDER_PROVIDER_AVATAR_TAG,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${workOrder.provider.name} ${workOrder.provider.surname}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag(WORK_ORDER_PROVIDER_TAG),
                )
                Text(
                    text = workOrder.provider.categoryName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SubtitleGray,
                    modifier = Modifier.testTag(WORK_ORDER_CATEGORY_TAG),
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

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
        workOrder.paidOnEpochMillis?.let { paidOn ->
            DetailRow(
                label = stringResource(R.string.work_order_paid_on),
                value = ScheduledDateFormatter.formatScheduled(paidOn),
                valueTestTag = WORK_ORDER_PAID_ON_TAG,
            )
        }

        if (workOrder.status == TurnoStatus.AwaitingPayment) {
            Button(
                onClick = actions.onPayNow,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(WORK_ORDER_PAY_NOW_TAG),
            ) {
                Text(stringResource(R.string.work_order_pay_now_cta))
            }
        }

        if (workOrder.completionReport != null &&
            workOrder.status != TurnoStatus.Confirmed
        ) {
            workOrder.completionReport?.let { report ->
                CompletionReportSection(
                    report = report,
                    onPhotoClick = onPhotoClick,
                )
            }
        }

        if (workOrder.status == TurnoStatus.Paid &&
            workOrder.review == null &&
            composer is ReviewComposerState.Hidden
        ) {
            OutlinedButton(
                onClick = actions.review.onOpen,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(WORK_ORDER_RATE_CTA_TAG),
            ) {
                Text(stringResource(R.string.work_order_rate_cta))
            }
        }

        if (composer is ReviewComposerState.Editing) {
            ReviewComposerSection(
                composer = composer,
                actions = actions.review,
            )
        }

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
private fun ReviewComposerSection(
    composer: ReviewComposerState.Editing,
    actions: WorkOrderDetailActions.ReviewActions,
) {
    val overflow = composer.descriptionDraft.length > REVIEW_DESCRIPTION_MAX_LENGTH
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(WORK_ORDER_RATE_COMPOSER_TAG),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.work_order_rate_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.testTag(WORK_ORDER_RATE_TITLE_TAG),
        )
        Text(
            text = stringResource(R.string.work_order_rate_stars_label),
            style = MaterialTheme.typography.labelMedium,
            color = SubtitleGray,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            (1..5).forEach { star ->
                val filled = composer.ratingDraft != null && star <= (composer.ratingDraft ?: 0)
                IconButton(
                    onClick = { actions.onRatingChange(star) },
                    modifier = Modifier.testTag(WORK_ORDER_RATE_STAR_TAG_PREFIX + star),
                ) {
                    Icon(
                        imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = star.toString(),
                        tint = if (filled) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
        OutlinedTextField(
            value = composer.descriptionDraft,
            onValueChange = actions.onDescriptionChange,
            label = { Text(stringResource(R.string.work_order_rate_comment_label)) },
            placeholder = { Text(stringResource(R.string.work_order_rate_comment_hint)) },
            singleLine = false,
            minLines = 3,
            maxLines = 6,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(WORK_ORDER_RATE_COMMENT_TAG),
        )
        Text(
            text = stringResource(
                R.string.work_order_rate_char_counter,
                composer.descriptionDraft.length,
                REVIEW_DESCRIPTION_MAX_LENGTH,
            ),
            style = MaterialTheme.typography.labelSmall,
            color = if (overflow) MaterialTheme.colorScheme.error else SubtitleGray,
            modifier = Modifier.testTag(WORK_ORDER_RATE_CHAR_COUNTER_TAG),
        )
        if (composer.error != null) {
            val message = stringResource(
                when (composer.error) {
                    is com.loresuelvo.consumer.domain.workorder.SubmitWorkOrderReviewOutcome.Network ->
                        R.string.work_order_rate_error_network
                    is com.loresuelvo.consumer.domain.workorder.SubmitWorkOrderReviewOutcome.Server ->
                        R.string.work_order_rate_error_server
                    is com.loresuelvo.consumer.domain.workorder.SubmitWorkOrderReviewOutcome.AlreadyReviewed ->
                        R.string.work_order_rate_error_already_reviewed
                    is com.loresuelvo.consumer.domain.workorder.SubmitWorkOrderReviewOutcome.Submitted ->
                        // The composer never renders in this branch
                        // (success flips the state to Hidden), but
                        // the `when` must be exhaustive.
                        R.string.work_order_rate_error_server
                },
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag(WORK_ORDER_RATE_ERROR_TAG),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = actions.onCancel,
                modifier = Modifier
                    .weight(1f)
                    .testTag(WORK_ORDER_RATE_CANCEL_TAG),
                enabled = !composer.submitting,
            ) {
                Text(stringResource(android.R.string.cancel))
            }
            Button(
                onClick = actions.onSubmit,
                modifier = Modifier
                    .weight(1f)
                    .testTag(WORK_ORDER_RATE_SUBMIT_TAG),
                enabled = composer.canSubmit && !overflow,
            ) {
                Text(stringResource(R.string.work_order_rate_submit))
            }
        }
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
        ServiceProposalsOutcome.Failure.AccessDenied ->
            stringResource(R.string.service_access_denied)
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
        if (failure != ServiceProposalsOutcome.Failure.AccessDenied) {
            Button(
                onClick = onRetry,
                modifier = Modifier.testTag(WORK_ORDER_ERROR_RETRY_TAG),
            ) {
                Text(stringResource(R.string.work_order_error_retry))
            }
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
const val WORK_ORDER_PROVIDER_AVATAR_TAG: String = "work-order-provider-avatar"
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
const val WORK_ORDER_RATE_CTA_TAG: String = "work-order-rate-cta"
const val WORK_ORDER_RATE_COMPOSER_TAG: String = "work-order-rate-composer"
const val WORK_ORDER_RATE_TITLE_TAG: String = "work-order-rate-title"
const val WORK_ORDER_RATE_STAR_TAG_PREFIX: String = "work-order-rate-star-"
const val WORK_ORDER_RATE_COMMENT_TAG: String = "work-order-rate-comment"
const val WORK_ORDER_RATE_CHAR_COUNTER_TAG: String = "work-order-rate-char-counter"
const val WORK_ORDER_RATE_SUBMIT_TAG: String = "work-order-rate-submit"
const val WORK_ORDER_RATE_CANCEL_TAG: String = "work-order-rate-cancel"
const val WORK_ORDER_RATE_ERROR_TAG: String = "work-order-rate-error"
const val REVIEW_DESCRIPTION_MAX_LENGTH: Int = 500
