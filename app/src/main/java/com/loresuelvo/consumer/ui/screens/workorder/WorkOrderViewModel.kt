package com.loresuelvo.consumer.ui.screens.workorder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loresuelvo.consumer.domain.usecase.workorder.GetWorkOrderByProposalIdUseCase
import com.loresuelvo.consumer.domain.workorder.GetWorkOrderOutcome
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the consumer work-order detail screen
 * ([WorkOrderScreen], US-54 scenario 16-VSP). Looks up the
 * [com.loresuelvo.consumer.domain.workorder.WorkOrder] tied to
 * the originating proposal id via
 * [GetWorkOrderByProposalIdUseCase] and exposes a sealed
 * [WorkOrderUiState].
 *
 * The host
 * ([com.loresuelvo.consumer.ui.navigation.WorkOrderRoute])
 * feeds the proposal id into [load] on first composition and on
 * manual retry from the [WorkOrderUiState.Error] surface. The
 * VM is Hilt-scoped to the route entry, so navigating to a
 * different work order triggers a fresh instance and a fresh
 * round trip.
 */
@HiltViewModel
class WorkOrderViewModel @Inject constructor(
    private val getWorkOrderByProposalId: GetWorkOrderByProposalIdUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<WorkOrderUiState>(WorkOrderUiState.Loading)
    val uiState: StateFlow<WorkOrderUiState> = _uiState.asStateFlow()

    /**
     * Loads the work order for [proposalId]. Re-entrant so the
     * host can re-fire on retry (mirrors the
     * [com.loresuelvo.consumer.ui.screens.chat.ConversationViewModel.load]
     * contract).
     */
    fun load(proposalId: String) {
        viewModelScope.launch {
            _uiState.update { WorkOrderUiState.Loading }
            val outcome = getWorkOrderByProposalId(proposalId)
            val next = when (outcome) {
                is GetWorkOrderOutcome.Found -> WorkOrderUiState.Ready(outcome.workOrder)
                is GetWorkOrderOutcome.NotFound -> WorkOrderUiState.NotFound
                is GetWorkOrderOutcome.Failure -> WorkOrderUiState.Error(outcome.failure)
            }
            _uiState.update { next }
        }
    }
}