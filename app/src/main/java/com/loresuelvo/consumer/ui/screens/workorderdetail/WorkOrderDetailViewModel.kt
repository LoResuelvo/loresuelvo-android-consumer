package com.loresuelvo.consumer.ui.screens.workorderdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loresuelvo.consumer.domain.usecase.workorder.GetWorkOrderDetailUseCase
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
 */
@HiltViewModel
class WorkOrderDetailViewModel @Inject constructor(
    private val getWorkOrderDetail: GetWorkOrderDetailUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<WorkOrderDetailUiState>(WorkOrderDetailUiState.Loading)
    val uiState: StateFlow<WorkOrderDetailUiState> = _uiState.asStateFlow()

    /**
     * Loads the work order for [workOrderId]. Re-entrant so the
     * host can re-fire on retry (mirrors the
     * [com.loresuelvo.consumer.ui.screens.chat.ConversationViewModel.load]
     * contract).
     */
    fun load(workOrderId: String) {
        viewModelScope.launch {
            _uiState.update { WorkOrderDetailUiState.Loading }
            val outcome = getWorkOrderDetail(workOrderId)
            val next = when (outcome) {
                is GetWorkOrderOutcome.Found -> WorkOrderDetailUiState.Ready(outcome.workOrder)
                is GetWorkOrderOutcome.NotFound -> WorkOrderDetailUiState.NotFound
                is GetWorkOrderOutcome.Failure -> WorkOrderDetailUiState.Error(outcome.failure)
            }
            _uiState.update { next }
        }
    }
}