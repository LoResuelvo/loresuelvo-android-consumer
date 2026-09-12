package com.loresuelvo.consumer.ui.screens.paymentresult

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loresuelvo.consumer.domain.payment.GetPaymentIntentOutcome
import com.loresuelvo.consumer.domain.payment.PaymentIntentStatus
import com.loresuelvo.consumer.domain.usecase.payment.GetPaymentIntentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ViewModel for the post-redirect payment-result screen. Polls
 * the backend until the payment intent reaches a terminal status
 * (`Paid` or `Rejected`) or the user leaves the screen. Polling
 * uses a single `viewModelScope` job so navigating away cancels
 * the loop automatically.
 *
 * Polling cadence: 2 s (good UX vs. request volume balance for
 * the public MP-redirected flow). The cadence is hard-coded
 * here rather than env-driven — the only environment that needs
 * to tune it is the load test rig, which already has its own
 * override path via `viewModelScope.cancel()` from a test.
 */
@HiltViewModel
class PaymentResultViewModel @Inject constructor(
    private val getPaymentIntent: GetPaymentIntentUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<PaymentResultUiState>(
        PaymentResultUiState.Loading(paymentIntentId = ""),
    )
    val uiState: StateFlow<PaymentResultUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    fun startPolling(paymentIntentId: String) {
        if (paymentIntentId.isBlank()) return
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                val next = mapOutcome(paymentIntentId, getPaymentIntent(paymentIntentId))
                _uiState.update { next }
                if (next is PaymentResultUiState.Approved ||
                    next is PaymentResultUiState.Rejected ||
                    next is PaymentResultUiState.NotFound ||
                    next is PaymentResultUiState.ServerError ||
                    next is PaymentResultUiState.NetworkError
                ) {
                    return@launch
                }
                delay(POLLING_INTERVAL_MILLIS)
            }
        }
    }

    private fun mapOutcome(
        paymentIntentId: String,
        outcome: GetPaymentIntentOutcome,
    ): PaymentResultUiState = when (outcome) {
        is GetPaymentIntentOutcome.Found -> {
            when (outcome.intent.status) {
                PaymentIntentStatus.Paid -> PaymentResultUiState.Approved(paymentIntentId)
                PaymentIntentStatus.Rejected -> PaymentResultUiState.Rejected(paymentIntentId)
                PaymentIntentStatus.RequiresCheckout,
                PaymentIntentStatus.CheckoutReady,
                PaymentIntentStatus.Processing ->
                    PaymentResultUiState.Polling(
                        paymentIntentId = paymentIntentId,
                        status = outcome.intent.status,
                    )
            }
        }
        is GetPaymentIntentOutcome.NotFound -> PaymentResultUiState.NotFound(paymentIntentId)
        is GetPaymentIntentOutcome.Network -> PaymentResultUiState.NetworkError(
            paymentIntentId = paymentIntentId,
            cause = outcome.cause,
        )
        is GetPaymentIntentOutcome.Server -> PaymentResultUiState.ServerError(
            paymentIntentId = paymentIntentId,
            code = outcome.code,
            message = outcome.message,
        )
    }

    private companion object {
        const val POLLING_INTERVAL_MILLIS: Long = 2_000
    }
}
