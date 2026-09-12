package com.loresuelvo.consumer.ui.screens.paymentresult

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.ui.theme.SubtitleGray

/**
 * Renders the post-redirect payment outcome (US-21 confirm-
 * agreement flow + US-28 service-balance flow). Driven by the
 * [PaymentResultViewModel]'s polling loop; the screen is
 * stateless and renders whatever the latest [PaymentResultUiState]
 * says.
 *
 * "Approved" and "Rejected" are terminal — the user gets a
 * clear message and a CTA to go home. "Processing" / "Polling"
 * keeps the spinner visible and an explicit "still checking"
 * copy so the user knows the app is waiting on the bank.
 */
@Composable
fun PaymentResultScreen(
    state: PaymentResultUiState,
    onRetry: () -> Unit,
    onReturnHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .testTag(PAYMENT_RESULT_SCREEN_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when (state) {
            is PaymentResultUiState.Loading -> LoadingBlock()
            is PaymentResultUiState.Polling -> PollingBlock()
            is PaymentResultUiState.Approved -> ApprovedBlock(onReturnHome = onReturnHome)
            is PaymentResultUiState.Rejected -> RejectedBlock(
                onRetry = onRetry,
                onReturnHome = onReturnHome,
            )
            is PaymentResultUiState.NotFound -> NotFoundBlock(onReturnHome = onReturnHome)
            is PaymentResultUiState.NetworkError -> ErrorBlock(
                message = stringResource(R.string.payment_result_error_network),
                onRetry = onRetry,
            )
            is PaymentResultUiState.ServerError -> ErrorBlock(
                message = state.message.ifBlank {
                    stringResource(R.string.payment_result_error_server)
                },
                onRetry = onRetry,
            )
        }
    }
}

@Composable
private fun LoadingBlock() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(PAYMENT_RESULT_LOADING_TAG),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(40.dp))
        Spacer(Modifier.size(12.dp))
        Text(
            text = stringResource(R.string.payment_result_loading),
            color = SubtitleGray,
        )
    }
}

@Composable
private fun PollingBlock() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(PAYMENT_RESULT_POLLING_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator(modifier = Modifier.size(40.dp))
        Text(
            text = stringResource(R.string.payment_result_processing),
            color = SubtitleGray,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.payment_result_processing_hint),
            color = SubtitleGray,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ApprovedBlock(onReturnHome: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(PAYMENT_RESULT_APPROVED_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.payment_result_approved_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.payment_result_approved_body),
            color = SubtitleGray,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onReturnHome,
            modifier = Modifier.testTag(PAYMENT_RESULT_APPROVED_HOME_TAG),
        ) {
            Text(stringResource(R.string.payment_result_return_home))
        }
    }
}

@Composable
private fun RejectedBlock(onRetry: () -> Unit, onReturnHome: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(PAYMENT_RESULT_REJECTED_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.payment_result_rejected_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.payment_result_rejected_body),
            color = SubtitleGray,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.testTag(PAYMENT_RESULT_REJECTED_RETRY_TAG),
        ) {
            Text(stringResource(R.string.payment_result_retry))
        }
        TextButton(
            onClick = onReturnHome,
            modifier = Modifier.testTag(PAYMENT_RESULT_REJECTED_HOME_TAG),
        ) {
            Text(stringResource(R.string.payment_result_return_home))
        }
    }
}

@Composable
private fun NotFoundBlock(onReturnHome: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(PAYMENT_RESULT_NOT_FOUND_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.payment_result_not_found),
            color = SubtitleGray,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onReturnHome,
            modifier = Modifier.testTag(PAYMENT_RESULT_NOT_FOUND_HOME_TAG),
        ) {
            Text(stringResource(R.string.payment_result_return_home))
        }
    }
}

@Composable
private fun ErrorBlock(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(PAYMENT_RESULT_ERROR_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = message,
            color = SubtitleGray,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.testTag(PAYMENT_RESULT_ERROR_RETRY_TAG),
        ) {
            Text(stringResource(R.string.payment_result_error_retry))
        }
    }
}

const val PAYMENT_RESULT_SCREEN_TAG: String = "payment-result-screen"
const val PAYMENT_RESULT_LOADING_TAG: String = "payment-result-loading"
const val PAYMENT_RESULT_POLLING_TAG: String = "payment-result-polling"
const val PAYMENT_RESULT_APPROVED_TAG: String = "payment-result-approved"
const val PAYMENT_RESULT_APPROVED_HOME_TAG: String = "payment-result-approved-home"
const val PAYMENT_RESULT_REJECTED_TAG: String = "payment-result-rejected"
const val PAYMENT_RESULT_REJECTED_RETRY_TAG: String = "payment-result-rejected-retry"
const val PAYMENT_RESULT_REJECTED_HOME_TAG: String = "payment-result-rejected-home"
const val PAYMENT_RESULT_NOT_FOUND_TAG: String = "payment-result-not-found"
const val PAYMENT_RESULT_NOT_FOUND_HOME_TAG: String = "payment-result-not-found-home"
const val PAYMENT_RESULT_ERROR_TAG: String = "payment-result-error"
const val PAYMENT_RESULT_ERROR_RETRY_TAG: String = "payment-result-error-retry"
