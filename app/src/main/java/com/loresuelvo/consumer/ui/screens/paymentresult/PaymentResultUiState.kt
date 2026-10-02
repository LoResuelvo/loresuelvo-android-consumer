package com.loresuelvo.consumer.ui.screens.paymentresult

import com.loresuelvo.consumer.domain.payment.PaymentIntentStatus

sealed interface PaymentResultUiState {
    val paymentIntentId: String

    data class Loading(
        override val paymentIntentId: String,
    ) : PaymentResultUiState

    data class Polling(
        override val paymentIntentId: String,
        val status: PaymentIntentStatus,
    ) : PaymentResultUiState

    data class Approved(
        override val paymentIntentId: String,
    ) : PaymentResultUiState

    data class Rejected(
        override val paymentIntentId: String,
    ) : PaymentResultUiState

    data class NotFound(
        override val paymentIntentId: String,
    ) : PaymentResultUiState

    data class NetworkError(
        override val paymentIntentId: String,
        val cause: Throwable,
    ) : PaymentResultUiState

    data class ServerError(
        override val paymentIntentId: String,
        val code: Int,
        val message: String,
    ) : PaymentResultUiState
}
