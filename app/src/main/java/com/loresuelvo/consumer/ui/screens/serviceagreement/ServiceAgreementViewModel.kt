package com.loresuelvo.consumer.ui.screens.serviceagreement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loresuelvo.consumer.domain.payment.CheckoutSessionOutcome
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalRepository
import com.loresuelvo.consumer.domain.usecase.payment.GetPaymentIntentUseCase
import com.loresuelvo.consumer.domain.usecase.payment.StartServiceProposalCheckoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ServiceAgreementViewModel @Inject constructor(
    private val serviceProposalRepository: ServiceProposalRepository,
    private val startCheckout: StartServiceProposalCheckoutUseCase,
    @Suppress("unused") private val getPaymentIntent: GetPaymentIntentUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ServiceAgreementUiState>(
        ServiceAgreementUiState.Loading(serviceProposalId = 0),
    )
    val uiState: StateFlow<ServiceAgreementUiState> = _uiState.asStateFlow()

    private val _events = Channel<ServiceAgreementEvent>(Channel.BUFFERED)
    val events: Flow<ServiceAgreementEvent> = _events.receiveAsFlow()

    fun load(serviceProposalId: Int) {
        viewModelScope.launch {
            _uiState.update { ServiceAgreementUiState.Loading(serviceProposalId) }
            val proposals = when (val outcome = serviceProposalRepository.getServiceProposals()) {
                is com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome.Success ->
                    outcome.proposals.firstOrNull { it.id == serviceProposalId.toString() }
                        ?.let { listOf(it) } ?: emptyList()
                else -> null
            }
            if (proposals == null) {
                _uiState.update {
                    ServiceAgreementUiState.NetworkError(
                        serviceProposalId = serviceProposalId,
                        cause = IllegalStateException("Could not load proposals"),
                    )
                }
                return@launch
            }
            val proposal = proposals.firstOrNull()
            if (proposal == null) {
                _uiState.update {
                    ServiceAgreementUiState.ServerError(
                        serviceProposalId = serviceProposalId,
                        code = 404,
                        message = "Service proposal not found",
                    )
                }
                return@launch
            }
            _uiState.update {
                ServiceAgreementUiState.Ready(
                    serviceProposalId = serviceProposalId,
                    proposal = proposal,
                    providerName = "${proposal.counterpart.name} ${proposal.counterpart.surname}",
                    depositCents = null, // shown only when the backend later exposes it
                    currency = "ARS",
                )
            }
        }
    }

    fun confirmAgreement() {
        val current = _uiState.value
        if (current !is ServiceAgreementUiState.Ready) return
        val serviceProposalId = current.serviceProposalId
        _uiState.update {
            ServiceAgreementUiState.StartingCheckout(
                serviceProposalId = serviceProposalId,
                proposal = current.proposal,
                providerName = current.providerName,
                depositCents = current.depositCents,
                currency = current.currency,
            )
        }
        viewModelScope.launch {
            when (val outcome = startCheckout(serviceProposalId)) {
                is CheckoutSessionOutcome.Created -> {
                    val checkoutUrl = outcome.intent.checkoutSession?.url
                    if (checkoutUrl == null) {
                        _uiState.update {
                            ServiceAgreementUiState.ServerError(
                                serviceProposalId = serviceProposalId,
                                code = 500,
                                message = "Checkout URL missing",
                            )
                        }
                        return@launch
                    }
                    _uiState.update {
                        ServiceAgreementUiState.CheckoutReady(
                            serviceProposalId = serviceProposalId,
                            paymentIntentId = outcome.intent.id,
                            checkoutUrl = checkoutUrl,
                        )
                    }
                    _events.trySend(
                        ServiceAgreementEvent.OpenCheckout(
                            checkoutUrl = checkoutUrl,
                            paymentIntentId = outcome.intent.id,
                        )
                    )
                }
                is CheckoutSessionOutcome.Network -> _uiState.update {
                    ServiceAgreementUiState.NetworkError(
                        serviceProposalId = serviceProposalId,
                        cause = outcome.cause,
                    )
                }
                is CheckoutSessionOutcome.Server -> _uiState.update {
                    ServiceAgreementUiState.ServerError(
                        serviceProposalId = serviceProposalId,
                        code = outcome.code,
                        message = outcome.message,
                    )
                }
                is CheckoutSessionOutcome.AlreadyPaid -> _uiState.update {
                    ServiceAgreementUiState.AlreadyPaid(
                        serviceProposalId = serviceProposalId,
                        message = outcome.message,
                    )
                }
            }
        }
    }

    fun cancel() {
        _events.trySend(ServiceAgreementEvent.Cancelled)
    }
}
