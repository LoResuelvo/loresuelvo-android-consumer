package com.loresuelvo.consumer.ui.screens.paymentresult

import com.loresuelvo.consumer.domain.payment.PaymentIntentStatus

/**
 * UDF state for the post-redirect payment-result screen
 * (US-21 confirm-agreement flow + US-28 service-balance flow).
 * Drives the polling loop that asks the backend
 * `GET /payment-intents/{id}` for the latest status until the
 * payment is `Paid` or `Rejected`. The polling stops as soon as
 * the status is terminal.
 *
 * The terminal states are terminal for the *consumer* — the
 * backend may continue processing the payment (e.g. webhook
 * arriving slightly later than our poll), but the consumer-side
 * view has nothing to wait for.
 */
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
