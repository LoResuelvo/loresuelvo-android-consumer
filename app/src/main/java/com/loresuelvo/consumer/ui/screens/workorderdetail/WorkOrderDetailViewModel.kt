package com.loresuelvo.consumer.ui.screens.workorderdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loresuelvo.consumer.domain.payment.CheckoutSessionOutcome
import com.loresuelvo.consumer.domain.usecase.payment.StartWorkOrderCheckoutUseCase
import com.loresuelvo.consumer.domain.usecase.workorder.GetWorkOrderDetailUseCase
import com.loresuelvo.consumer.domain.usecase.workorder.RateProviderUseCase
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
 *
 * **Rate flow** (US-30 `calify-provider-service`, scenarios
 * 01-CT / 02-CT): when the work order is `paid` and the cached
 * [com.loresuelvo.consumer.domain.workorder.WorkOrderDetail.review]
 * is `null`, the screen surfaces a "Calificar servicio" CTA.
 * Tapping it calls [openReviewComposer], which flips the
 * composer state to [ReviewComposerState.Editing]. The submit /
 * cancel / rating-input flow is delegated through
 * [RateProviderUseCase] (commit W: wire submit) and renders the
 * success / error states inside the composer — see
 * [ReviewComposerState] for the full UDF contract.
 */
@HiltViewModel
class WorkOrderDetailViewModel @Inject constructor(
    private val getWorkOrderDetail: GetWorkOrderDetailUseCase,
    private val startWorkOrderCheckout: StartWorkOrderCheckoutUseCase,
    private val rateProvider: RateProviderUseCase,
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

    /**
     * Opens the rating composer for the work order currently in
     * [WorkOrderDetailUiState.Ready] (US-30 scenario 02-CT). The
     * host wires this to the "Calificar servicio" CTA which only
     * renders when `status == paid && review == null` so this
     * method assumes a paid work order is loaded — if not, it
     * silently no-ops to keep the screen stateless.
     */
    fun openReviewComposer() {
        _uiState.update { current ->
            if (current !is WorkOrderDetailUiState.Ready) return@update current
            current.copy(
                composer = ReviewComposerState.Editing(),
            )
        }
    }

    /**
     * Collapses the composer back to [ReviewComposerState.Hidden]
     * without submitting (used by the optional "Cancelar" button
     * inside the form). Wired in commit W (submit handler).
     */
    @Suppress("unused") // exercised by the submit-handler commit
    fun cancelReviewComposer() {
        _uiState.update { current ->
            if (current !is WorkOrderDetailUiState.Ready) return@update current
            current.copy(composer = ReviewComposerState.Hidden)
        }
    }

    /**
     * Records a draft rating (1..5) inside the composer; the host
     * wires it to the star-row tap handler. Wired in commit W
     * (submit handler).
     */
    @Suppress("unused") // exercised by the submit-handler commit
    fun onRatingChange(rating: Int) {
        _uiState.update { current ->
            if (current !is WorkOrderDetailUiState.Ready) return@update current
            val composer = current.composer as? ReviewComposerState.Editing
                ?: return@update current
            current.copy(
                composer = composer.copy(ratingDraft = rating),
            )
        }
    }

    /**
     * Records a draft description inside the composer (capped at
     * 500 chars by the host). Wired in commit W.
     */
    @Suppress("unused") // exercised by the submit-handler commit
    fun onDescriptionChange(description: String) {
        _uiState.update { current ->
            if (current !is WorkOrderDetailUiState.Ready) return@update current
            val composer = current.composer as? ReviewComposerState.Editing
                ?: return@update current
            current.copy(
                composer = composer.copy(descriptionDraft = description),
            )
        }
    }

    /**
     * Submits the current composer draft through
     * [RateProviderUseCase] (US-30 scenario 05-CT). On
     * [com.loresuelvo.consumer.domain.workorder.SubmitWorkOrderReviewOutcome.Submitted]
     * / [AlreadyReviewed] the VM merges the new review into
     * [WorkOrderDetailUiState.Ready.workOrder] and collapses the
     * composer; on [Network] / [Server] it leaves the composer
     * open with the typed failure stamped on
     * [ReviewComposerState.Editing.error]. Wired in commit W.
     */
    @Suppress("unused") // exercised by the submit-handler commit
    fun submitReview() {
        val state = _uiState.value as? WorkOrderDetailUiState.Ready ?: return
        val composer = state.composer as? ReviewComposerState.Editing ?: return
        val rating = composer.ratingDraft ?: return
        if (composer.submitting) return
        val workOrderId = state.workOrder.proposalId

        _uiState.update {
            state.copy(
                composer = composer.copy(submitting = true, error = null),
            )
        }
        viewModelScope.launch {
            val outcome = rateProvider(
                workOrderId = workOrderId,
                rating = rating,
                description = composer.descriptionDraft,
            )
            _uiState.update { current ->
                if (current !is WorkOrderDetailUiState.Ready) return@update current
                val editing = current.composer as? ReviewComposerState.Editing
                    ?: return@update current
                when (outcome) {
                    is com.loresuelvo.consumer.domain.workorder.SubmitWorkOrderReviewOutcome.Submitted -> current.copy(
                        workOrder = current.workOrder.copy(review = outcome.review),
                        composer = ReviewComposerState.Hidden,
                    )
                    is com.loresuelvo.consumer.domain.workorder.SubmitWorkOrderReviewOutcome.AlreadyReviewed -> current.copy(
                        // Treat 409 like a successful submit: the
                        // backend already has a review on file, so
                        // collapse the composer and let the
                        // consumer re-load if they want the latest
                        // review text. The next `load()` will pull
                        // the persisted review from the wire.
                        composer = ReviewComposerState.Hidden,
                    )
                    is com.loresuelvo.consumer.domain.workorder.SubmitWorkOrderReviewOutcome.Network,
                    is com.loresuelvo.consumer.domain.workorder.SubmitWorkOrderReviewOutcome.Server -> current.copy(
                        composer = editing.copy(
                            submitting = false,
                            error = outcome,
                        ),
                    )
                }
            }
        }
    }
}