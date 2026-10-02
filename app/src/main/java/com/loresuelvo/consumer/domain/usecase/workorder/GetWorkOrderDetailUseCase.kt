package com.loresuelvo.consumer.domain.usecase.workorder

import com.loresuelvo.consumer.domain.workorder.GetWorkOrderOutcome
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailCounterpart
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailRepository
import javax.inject.Inject

class GetWorkOrderDetailUseCase @Inject constructor(
    private val workOrderDetailRepository: WorkOrderDetailRepository,
) {
    suspend operator fun invoke(
        workOrderId: String,
        provider: WorkOrderDetailCounterpart? = null,
    ): GetWorkOrderOutcome =
        workOrderDetailRepository.getWorkOrderDetail(workOrderId, provider)
}