package com.loresuelvo.consumer.ui.screens.workorderdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loresuelvo.consumer.domain.payment.CheckoutSessionOutcome
import com.loresuelvo.consumer.domain.usecase.payment.StartWorkOrderCheckoutUseCase
import com.loresuelvo.consumer.domain.usecase.workorder.GetWorkOrderDetailUseCase
import com.loresuelvo.consumer.domain.workorder.GetWorkOrderOutcome
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the consumer work-order detail screen
 * ([WorkOrderDetailScreen], US-54 scenario 16-VSP, US-27
 * `visualize-turns-detail`). Looks up the
 * [com.loresuelvo.consumer.domain.workorder.WorkOrderDetail]
 * tied to the work-order id via
 * [GetWorkOrderDetailUseCase] and exposes a sealed
 * [WorkOrderDetailUiState].
 *
 * The host
 * ([com.loresuelvo.consumer.ui.navigation.WorkOrderDetailRoute])
 * feeds the work-order id into [load] on first composition and
 * on manual retry from the [WorkOrderDetailUiState.Error]
 * surface. The VM is Hilt-scoped to the route entry, so
 * navigating to a different work order triggers a fresh
 * instance and a fresh round trip.
 *
 * **Pay flow** (US-27 scenario 09-VTD): tapping the
 * "Pagar saldo restante" CTA calls [payNow], which delegates to
 * [StartWorkOrderCheckoutUseCase] and surfaces the resulting
 * checkout URL on [checkoutUrl] (the route handler opens it in
 * a Custom Tab) or an error on [payError].
 */
@HiltViewModel
class WorkOrderDetailViewModel @Inject constructor(
    private val getWorkOrderDetail: GetWorkOrderDetailUseCase,
    private val startWorkOrderCheckout: StartWorkOrderCheckoutUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<WorkOrderDetailUiState>(WorkOrderDetailUiState.Loading)
    val uiState: StateFlow<WorkOrderDetailUiState> = _uiState.asStateFlow()

    private val _checkoutUrl = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val checkoutUrl: SharedFlow<String> = _checkoutUrl.asSharedFlow()

    private val _payError = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val payError: SharedFlow<String> = _payError.asSharedFlow()

    /**
     * Loads the work order for [workOrderId]. Re-entrant so the
     * host can re-fire on retry (mirrors the
     * [com.loresuelvo.consumer.ui.screens.chat.ConversationViewModel.load]
     * contract). If the consumer entered from a list that already
     * carried the provider metadata, that fallback is forwarded to
     * the repository so we do not attempt to deserialize a
     * non-existent provider field from the dedicated detail JSON.
     */
    fun load(workOrderId: String, provider: com.loresuelvo.consumer.domain.workorder.WorkOrderDetailCounterpart? = null) {
        viewModelScope.launch {
            _uiState.update { WorkOrderDetailUiState.Loading }
            val outcome = getWorkOrderDetail(workOrderId, provider)
            val next = when (outcome) {
                is GetWorkOrderOutcome.Found -> WorkOrderDetailUiState.Ready(outcome.workOrder)
                is GetWorkOrderOutcome.NotFound -> WorkOrderDetailUiState.NotFound
                is GetWorkOrderOutcome.Failure -> WorkOrderDetailUiState.Error(outcome.failure)
            }
            _uiState.update { next }
        }
    }

    /**
     * Starts a Mercado Pago checkout session for the work
     * order's remaining balance. Emits the resulting checkout
     * URL on [checkoutUrl] (the route handler opens it in a
     * Custom Tab) or surfaces an error message on [payError].
     *
     * The intent id is taken from the work-order id (the route
     * arg). US-27 scenario 09-VTD asserts the call lands in
     * [CheckoutSessionOutcome.Created]; [Network] / [Server] /
     * [AlreadyPaid] surface an error message that the screen
     * consumes via [payError].
     */
    fun payNow(workOrderId: String) {
        viewModelScope.launch {
            val outcome = startWorkOrderCheckout(workOrderId.toIntOrNull() ?: return@launch)
            when (outcome) {
                is CheckoutSessionOutcome.Created -> {
                    val url = outcome.intent.checkoutSession?.url
                    if (!url.isNullOrBlank()) {
                        _checkoutUrl.emit(url)
                    } else {
                        _payError.emit("No se pudo obtener el enlace de pago.")
                    }
                }
                is CheckoutSessionOutcome.Network ->
                    _payError.emit(
                        "No pudimos conectarnos con el servidor. Revisá tu conexión e intentá nuevamente.",
                    )
                is CheckoutSessionOutcome.Server ->
                    _payError.emit(
                        outcome.message.ifBlank { "No pudimos iniciar el pago. Intentá nuevamente." },
                    )
                is CheckoutSessionOutcome.AlreadyPaid ->
                    _payError.emit(
                        outcome.message.ifBlank { "Esta orden ya fue pagada." },
                    )
            }
        }
    }
}