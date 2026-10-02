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