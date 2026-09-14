package com.loresuelvo.consumer.ui.screens.proposals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loresuelvo.consumer.domain.payment.CheckoutSessionOutcome
import com.loresuelvo.consumer.domain.usecase.payment.StartServiceProposalCheckoutUseCase
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the [ProposalDetailScreen] (US-54 scenario
 * 08-VSP). Loads a single proposal by id from the
 * [ServiceProposalRepository] and exposes a [ProposalDetailUiState]
 * for the screen to render.
 */
@HiltViewModel
class ProposalDetailViewModel @Inject constructor(
    private val serviceProposalRepository: ServiceProposalRepository,
    private val startServiceProposalCheckout: StartServiceProposalCheckoutUseCase,
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<ProposalDetailUiState>(ProposalDetailUiState.Idle)

    val uiState: StateFlow<ProposalDetailUiState> =
        _uiState.asStateFlow()

    /**
     * Emits the Mercado Pago checkout URL when the checkout
     * session is successfully created or reused.
     *
     * The UI/Activity is responsible for opening this URL
     * in a Custom Tab.
     */
    private val _checkoutUrl = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
    )

    val checkoutUrl: SharedFlow<String> =
        _checkoutUrl.asSharedFlow()

    /**
     * Emits checkout failures so the host can show the
     * corresponding error to the user.
     */
    private val _checkoutError = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
    )

    val checkoutError: SharedFlow<String> =
        _checkoutError.asSharedFlow()

    private var loadJob: Job? = null
    private var paymentJob: Job? = null

    fun load(proposalId: String) {
        if (proposalId.isBlank()) {
            reset()
            return
        }

        loadJob?.cancel()

        loadJob = viewModelScope.launch {
            _uiState.update {
                ProposalDetailUiState.Loading
            }

            val outcome = serviceProposalRepository.getServiceProposals()

            _uiState.update {
                when (outcome) {
                    is ServiceProposalsOutcome.Success -> {
                        val match = outcome.proposals
                            .firstOrNull { it.id == proposalId }

                        if (match != null) {
                            ProposalDetailUiState.Ready(match)
                        } else {
                            ProposalDetailUiState.Error(
                                ServiceProposalsOutcome.Failure.Server(
                                    code = 404,
                                    message = "Proposal $proposalId not found",
                                ),
                            )
                        }
                    }

                    is ServiceProposalsOutcome.Failure ->
                        ProposalDetailUiState.Error(outcome)
                }
            }
        }
    }

    /**
     * Starts or reuses the checkout session for the proposal.
     *
     * When successful, the checkout URL is emitted and the
     * UI/Activity opens it in a Custom Tab.
     */
    // TODO: Create strings for each error at strings.xml
    fun payNow(proposalId: String) {
        val serviceProposalId = proposalId.toIntOrNull()

        if (serviceProposalId == null) {
            viewModelScope.launch {
                _checkoutError.emit("No se pudo identificar la solicitud.")
            }
            return
        }

        paymentJob?.cancel()

        paymentJob = viewModelScope.launch {
            when (
                val outcome = startServiceProposalCheckout(
                    serviceProposalId,
                )
            ) {
                is CheckoutSessionOutcome.Created -> {
                    val checkoutUrl =
                        outcome.intent.checkoutSession?.url

                    if (!checkoutUrl.isNullOrBlank()) {
                        _checkoutUrl.emit(checkoutUrl)
                    } else {
                        _checkoutError.emit(
                            "No se pudo obtener el enlace de pago.",
                        )
                    }
                }

                is CheckoutSessionOutcome.Network -> {
                    _checkoutError.emit(
                        "No pudimos conectarnos con el servidor. Revisá tu conexión e intentá nuevamente.",
                    )
                }

                is CheckoutSessionOutcome.Server -> {
                    _checkoutError.emit(
                        outcome.message.ifBlank {
                            "No pudimos iniciar el pago. Intentá nuevamente."
                        },
                    )
                }

                is CheckoutSessionOutcome.AlreadyPaid -> {
                    _checkoutError.emit(
                        outcome.message.ifBlank {
                            "Esta solicitud ya fue pagada."
                        },
                    )
                }
            }
        }
    }

    fun reset() {
        loadJob?.cancel()
        paymentJob?.cancel()
        _uiState.value = ProposalDetailUiState.Idle
    }
}