package com.loresuelvo.consumer.ui.screens.serviceagreement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.ui.theme.SubtitleGray
import com.loresuelvo.consumer.ui.util.CurrencyFormatter
import com.loresuelvo.consumer.ui.util.ScheduledDateFormatter
import com.loresuelvo.consumer.ui.util.EstimatedDurationFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Confirmation screen for the US-21 service-agreement flow.
 *
 * Renders the agreement the provider sent (description, amount,
 * scheduled time, estimated duration) and offers a "Confirmar
 * acuerdo" CTA. The CTA is disabled while the checkout session is
 * being created (`StartingCheckout`) so a rapid double-tap cannot
 * re-issue the same POST. A confirmation modal asks the consumer
 * to confirm the intent before triggering the seña.
 *
 * On `CheckoutReady` the host opens the returned
 * `checkout_url` in a Custom Tab and navigates to the
 * `Route.PaymentResult` route keyed on the payment intent id —
 * see [ServiceAgreementViewModel.confirmAgreement] for the
 * `OpenCheckout` event.
 */
@Composable
fun ServiceAgreementScreen(
    state: ServiceAgreementUiState,
    onRetry: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onReturnHome: () -> Unit,
    onPaidAlready: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag(SERVICE_AGREEMENT_SCREEN_TAG),
    ) {
        when (state) {
            is ServiceAgreementUiState.Loading -> LoadingBlock()
            is ServiceAgreementUiState.Ready -> ReadyBlock(
                state = state,
                onTapConfirm = { showConfirmDialog = true },
                onCancel = onCancel,
                onReturnHome = onReturnHome,
            )
            is ServiceAgreementUiState.StartingCheckout -> StartingBlock(
                state = state,
                onReturnHome = onReturnHome,
            )
            is ServiceAgreementUiState.CheckoutReady -> CheckoutReadyBlock(state = state)
            is ServiceAgreementUiState.NetworkError -> ErrorBlock(
                primaryMessage = stringResource(R.string.service_agreement_error_network),
                onRetry = onRetry,
                onReturnHome = onReturnHome,
            )
            is ServiceAgreementUiState.ServerError -> ErrorBlock(
                primaryMessage = state.message.ifBlank {
                    stringResource(R.string.service_agreement_error_server)
                },
                onRetry = onRetry,
                onReturnHome = onReturnHome,
            )
            is ServiceAgreementUiState.AlreadyPaid -> AlreadyPaidBlock(
                message = state.message,
                onReturnHome = onPaidAlready,
            )
        }
        if (showConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                title = { Text(stringResource(R.string.service_agreement_confirm_title)) },
                text = { Text(stringResource(R.string.service_agreement_confirm_body)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showConfirmDialog = false
                            onConfirm()
                        },
                        modifier = Modifier.testTag(SERVICE_AGREEMENT_CONFIRM_BUTTON_TAG),
                    ) {
                        Text(stringResource(R.string.service_agreement_confirm_yes))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmDialog = false }) {
                        Text(stringResource(R.string.service_agreement_confirm_no))
                    }
                },
            )
        }
    }
}

@Composable
private fun LoadingBlock() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp)
            .testTag(SERVICE_AGREEMENT_LOADING_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(R.string.service_agreement_loading),
            color = SubtitleGray,
        )
    }
}

@Composable
private fun ReadyBlock(
    state: ServiceAgreementUiState.Ready,
    onTapConfirm: () -> Unit,
    onCancel: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onReturnHome: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .testTag(SERVICE_AGREEMENT_READY_TAG),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = state.providerName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        ReadOnlyRow(
            label = stringResource(R.string.service_agreement_description),
            value = state.proposal.description,
            testTag = SERVICE_AGREEMENT_DESCRIPTION_TAG,
        )
        ReadOnlyRow(
            label = stringResource(R.string.service_agreement_amount),
            value = CurrencyFormatter.formatAmount(state.proposal.amountCents),
            testTag = SERVICE_AGREEMENT_AMOUNT_TAG,
        )
        ReadOnlyRow(
            label = stringResource(R.string.service_agreement_scheduled),
            value = ScheduledDateFormatter.formatScheduled(state.proposal.scheduledOnEpochMillis),
            testTag = SERVICE_AGREEMENT_SCHEDULED_TAG,
        )
        state.proposal.estimatedDurationMinutes?.let { minutes ->
            ReadOnlyRow(
                label = stringResource(R.string.service_agreement_estimated_duration),
                value = EstimatedDurationFormatter.formatDuration(minutes),
                testTag = SERVICE_AGREEMENT_DURATION_TAG,
            )
        }
        if (state.depositCents != null) {
            ReadOnlyRow(
                label = stringResource(R.string.service_agreement_deposit),
                value = CurrencyFormatter.formatAmount(state.depositCents),
                testTag = SERVICE_AGREEMENT_DEPOSIT_TAG,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.service_agreement_readonly_hint),
            color = SubtitleGray,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onTapConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(SERVICE_AGREEMENT_CONFIRM_CTA_TAG),
        ) {
            Text(
                text = stringResource(R.string.service_agreement_confirm_cta),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        TextButton(
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(SERVICE_AGREEMENT_CANCEL_TAG),
        ) {
            Text(
                text = stringResource(R.string.service_agreement_cancel),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun StartingBlock(
    state: ServiceAgreementUiState.StartingCheckout,
    @Suppress("UNUSED_PARAMETER") onReturnHome: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .testTag(SERVICE_AGREEMENT_STARTING_TAG),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = state.providerName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        ReadOnlyRow(
            label = stringResource(R.string.service_agreement_description),
            value = state.proposal.description,
            testTag = "${SERVICE_AGREEMENT_DESCRIPTION_TAG}_starting",
        )
        ReadOnlyRow(
            label = stringResource(R.string.service_agreement_amount),
            value = CurrencyFormatter.formatAmount(state.proposal.amountCents),
            testTag = "${SERVICE_AGREEMENT_AMOUNT_TAG}_starting",
        )
        ReadOnlyRow(
            label = stringResource(R.string.service_agreement_scheduled),
            value = ScheduledDateFormatter.formatScheduled(state.proposal.scheduledOnEpochMillis),
            testTag = "${SERVICE_AGREEMENT_SCHEDULED_TAG}_starting",
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(modifier = Modifier.testTag(SERVICE_AGREEMENT_PROGRESS_TAG))
            Spacer(Modifier.size(12.dp))
            Text(
                text = stringResource(R.string.service_agreement_starting),
                color = SubtitleGray,
            )
        }
    }
}

@Composable
private fun CheckoutReadyBlock(state: ServiceAgreementUiState.CheckoutReady) {
    // The host navigates to the payment-result route as soon as
    // the VM emits this state. We render a tiny placeholder so the
    // user briefly sees a "preparing your payment…" message; the
    // navigation happens in a `LaunchedEffect` inside the route.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp)
            .testTag(SERVICE_AGREEMENT_READY_NAV_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.service_agreement_redirecting),
            color = SubtitleGray,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ErrorBlock(primaryMessage: String, onRetry: () -> Unit, onReturnHome: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 24.dp)
            .testTag(SERVICE_AGREEMENT_ERROR_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = primaryMessage,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onRetry,
            modifier = Modifier.testTag(SERVICE_AGREEMENT_ERROR_RETRY_TAG),
        ) {
            Text(stringResource(R.string.service_agreement_error_retry))
        }
        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = onReturnHome,
            modifier = Modifier.testTag(SERVICE_AGREEMENT_ERROR_RETURN_TAG),
        ) {
            Text(stringResource(R.string.service_agreement_error_return_home))
        }
    }
}

@Composable
private fun AlreadyPaidBlock(message: String, onReturnHome: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 24.dp)
            .testTag(SERVICE_AGREEMENT_ALREADY_PAID_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = message.ifBlank { stringResource(R.string.service_agreement_already_paid) },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onReturnHome,
            modifier = Modifier.testTag(SERVICE_AGREEMENT_ALREADY_PAID_RETURN_TAG),
        ) {
            Text(stringResource(R.string.service_agreement_return_home))
        }
    }
}

@Composable
private fun ReadOnlyRow(
    label: String,
    value: String,
    testTag: String,
) {
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
            modifier = Modifier.testTag(testTag),
        )
    }
}

const val SERVICE_AGREEMENT_SCREEN_TAG: String = "service-agreement-screen"
const val SERVICE_AGREEMENT_LOADING_TAG: String = "service-agreement-loading"
const val SERVICE_AGREEMENT_READY_TAG: String = "service-agreement-ready"
const val SERVICE_AGREEMENT_STARTING_TAG: String = "service-agreement-starting"
const val SERVICE_AGREEMENT_READY_NAV_TAG: String = "service-agreement-ready-nav"
const val SERVICE_AGREEMENT_DESCRIPTION_TAG: String = "service-agreement-description"
const val SERVICE_AGREEMENT_AMOUNT_TAG: String = "service-agreement-amount"
const val SERVICE_AGREEMENT_SCHEDULED_TAG: String = "service-agreement-scheduled"
const val SERVICE_AGREEMENT_DURATION_TAG: String = "service-agreement-duration"
const val SERVICE_AGREEMENT_DEPOSIT_TAG: String = "service-agreement-deposit"
const val SERVICE_AGREEMENT_CONFIRM_CTA_TAG: String = "service-agreement-confirm-cta"
const val SERVICE_AGREEMENT_CANCEL_TAG: String = "service-agreement-cancel"
const val SERVICE_AGREEMENT_CONFIRM_BUTTON_TAG: String = "service-agreement-confirm-button"
const val SERVICE_AGREEMENT_PROGRESS_TAG: String = "service-agreement-progress"
const val SERVICE_AGREEMENT_ERROR_TAG: String = "service-agreement-error"
const val SERVICE_AGREEMENT_ERROR_RETRY_TAG: String = "service-agreement-error-retry"
const val SERVICE_AGREEMENT_ERROR_RETURN_TAG: String = "service-agreement-error-return"
const val SERVICE_AGREEMENT_ALREADY_PAID_TAG: String = "service-agreement-already-paid"
const val SERVICE_AGREEMENT_ALREADY_PAID_RETURN_TAG: String = "service-agreement-already-paid-return"
