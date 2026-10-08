package com.loresuelvo.consumer.domain.usecase.workorder

import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import com.loresuelvo.consumer.domain.turno.TurnoCounterpart
import com.loresuelvo.consumer.domain.turno.TurnosOutcome
import com.loresuelvo.consumer.domain.workorder.GetWorkOrderOutcome
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailCounterpart
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailRepository
import com.loresuelvo.consumer.domain.usecase.turno.GetTurnosUseCase
import javax.inject.Inject

class GetWorkOrderDetailUseCase @Inject constructor(
    private val workOrderDetailRepository: WorkOrderDetailRepository,
    private val getTurnos: GetTurnosUseCase,
) {
    suspend operator fun invoke(
        workOrderId: String,
        provider: WorkOrderDetailCounterpart? = null,
    ): GetWorkOrderOutcome {
        val resolvedProvider = provider ?: when (val outcome = getTurnos()) {
            is TurnosOutcome.Success -> outcome.turnos
                .firstOrNull { it.id == workOrderId }
                ?.counterpart
                ?.toWorkOrderDetailCounterpart()
                ?: return GetWorkOrderOutcome.NotFound
            is TurnosOutcome.Failure.Network ->
                return GetWorkOrderOutcome.Failure(
                    ServiceProposalsOutcome.Failure.Network(outcome.cause),
                )
            is TurnosOutcome.Failure.Server ->
                return GetWorkOrderOutcome.Failure(
                    ServiceProposalsOutcome.Failure.Server(outcome.code, outcome.message),
                )
        }

        return workOrderDetailRepository.getWorkOrderDetail(workOrderId, resolvedProvider)
    }
}

private fun TurnoCounterpart.toWorkOrderDetailCounterpart(): WorkOrderDetailCounterpart =
    WorkOrderDetailCounterpart(
        id = id,
        name = name,
        surname = surname,
        categoryName = categoryName,
        profilePhotoUrl = profilePhotoUrl,
    )
