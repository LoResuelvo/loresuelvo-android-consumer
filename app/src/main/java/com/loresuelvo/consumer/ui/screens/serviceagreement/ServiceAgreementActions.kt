package com.loresuelvo.consumer.ui.screens.serviceagreement

data class ServiceAgreementActions(
    val onRetry: () -> Unit = {},
    val onConfirm: () -> Unit = {},
    val onCancel: () -> Unit = {},
    val onReturnHome: () -> Unit = {},
    val onPaidAlready: () -> Unit = {},
)
