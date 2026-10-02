package com.loresuelvo.consumer.ui.screens.serviceagreement

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

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
    ServiceAgreementScreen(
        state = state,
        actions = ServiceAgreementActions(
            onRetry = onRetry,
            onConfirm = onConfirm,
            onCancel = onCancel,
            onReturnHome = onReturnHome,
            onPaidAlready = onPaidAlready,
        ),
        modifier = modifier,
    )
}
