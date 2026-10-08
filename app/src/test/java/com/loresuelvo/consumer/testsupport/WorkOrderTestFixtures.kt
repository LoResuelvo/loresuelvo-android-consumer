package com.loresuelvo.consumer.testsupport

import com.loresuelvo.consumer.domain.turno.Turno
import com.loresuelvo.consumer.domain.turno.TurnoCounterpart
import com.loresuelvo.consumer.domain.turno.TurnoStatus
import com.loresuelvo.consumer.domain.turno.TurnosOutcome
import com.loresuelvo.consumer.domain.turno.TurnosRepository
import com.loresuelvo.consumer.domain.usecase.turno.GetTurnosUseCase
import com.loresuelvo.consumer.domain.usecase.workorder.GetWorkOrderDetailUseCase
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailRepository

fun workOrderDetailUseCaseForTest(
    repository: WorkOrderDetailRepository,
): GetWorkOrderDetailUseCase =
    GetWorkOrderDetailUseCase(
        repository,
        GetTurnosUseCase(TestTurnosRepository()),
    )

class TestTurnosRepository(
    private val outcome: TurnosOutcome = TurnosOutcome.Success(
        commonWorkOrderIds.map(::testTurno),
    ),
) : TurnosRepository {
    override suspend fun getTurnos(): TurnosOutcome = outcome
}

private val commonWorkOrderIds = setOf(
    "wo-1",
    "wo-42",
    "wo-100",
    "wo-101",
    "wo-102",
    "42",
    "missing",
    "not-a-number",
)

private fun testTurno(workOrderId: String): Turno = Turno(
    id = workOrderId,
    serviceProposalId = workOrderId,
    status = TurnoStatus.Confirmed,
    counterpart = TurnoCounterpart(
        id = "test-provider",
        name = "Test",
        surname = "Provider",
        categoryName = "Test service",
        profilePhotoUrl = null,
    ),
    description = "Test work order",
    amountCents = 0L,
    scheduledOnEpochMillis = 0L,
)
