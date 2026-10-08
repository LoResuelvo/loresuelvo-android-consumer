package com.loresuelvo.consumer.domain.usecase.workorder

import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import com.loresuelvo.consumer.domain.turno.Turno
import com.loresuelvo.consumer.domain.turno.TurnoCounterpart
import com.loresuelvo.consumer.domain.turno.TurnoStatus
import com.loresuelvo.consumer.domain.turno.TurnosOutcome
import com.loresuelvo.consumer.domain.turno.TurnosRepository
import com.loresuelvo.consumer.domain.usecase.turno.GetTurnosUseCase
import com.loresuelvo.consumer.domain.workorder.GetWorkOrderOutcome
import com.loresuelvo.consumer.domain.workorder.SubmitWorkOrderReviewOutcome
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetail
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailCounterpart
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailRepository
import kotlinx.coroutines.test.runTest
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetWorkOrderDetailUseCaseTest {

    @Test
    fun returns_found_when_work_order_matches() = runTest {
        val useCase = useCase(
            FakeWorkOrderDetailRepository(detail = workOrder("wo-1")),
        )

        val outcome = useCase("wo-1", provider())

        assertTrue("expected Found, was $outcome", outcome is GetWorkOrderOutcome.Found)
        val found = (outcome as GetWorkOrderOutcome.Found).workOrder
        assertEquals("wo-1", found.proposalId)
        assertEquals("Plomería", found.provider.categoryName)
        assertEquals("Fuga en el lavamanos", found.description)
        assertEquals(1_500_000L, found.amountCents)
        assertEquals(90, found.estimatedDurationMinutes)
        assertEquals(TurnoStatus.Pending, found.status)
        assertEquals("Carlos López", "${found.provider.name} ${found.provider.surname}")
    }

    @Test
    fun returns_not_found_when_id_does_not_match() = runTest {
        val useCase = useCase(
            FakeWorkOrderDetailRepository(detail = workOrder("wo-1")),
        )

        assertEquals(GetWorkOrderOutcome.NotFound, useCase("wo-999", provider()))
    }

    @Test
    fun returns_not_found_when_repository_has_no_work_order() = runTest {
        val useCase = useCase(FakeWorkOrderDetailRepository())

        assertEquals(GetWorkOrderOutcome.NotFound, useCase("wo-1", provider()))
    }

    @Test
    fun returns_failure_when_repository_surfaces_a_failure() = runTest {
        val failure = ServiceProposalsOutcome.Failure.Server(500, "down for maintenance")
        val useCase = useCase(
            FakeWorkOrderDetailRepository(failure = failure),
        )

        val result = useCase("wo-1", provider())

        assertEquals(GetWorkOrderOutcome.Failure(failure), result)
    }

    @Test
    fun work_order_without_estimated_duration_surfaces_null() = runTest {
        val detail = workOrder().copy(estimatedDurationMinutes = null)
        val useCase = useCase(FakeWorkOrderDetailRepository(detail = detail))

        val outcome = useCase("wo-1", provider())

        assertTrue("expected Found, was $outcome", outcome is GetWorkOrderOutcome.Found)
        assertEquals(null, (outcome as GetWorkOrderOutcome.Found).workOrder.estimatedDurationMinutes)
    }

    @Test
    fun resolves_provider_from_the_exact_current_work_order() = runTest {
        val expectedProvider = WorkOrderDetailCounterpart(
            id = "20",
            name = "Actualizada",
            surname = "Prestadora",
            categoryName = "Plomería",
            profilePhotoUrl = null,
        )
        val turnosRepository = RecordingTurnosRepository(
            TurnosOutcome.Success(
                listOf(
                    turno("99", provider("21", "Otra", "Persona", "Electricidad")),
                    turno("88", expectedProvider),
                ),
            ),
        )
        val detailRepository = RecordingWorkOrderDetailRepository(
            GetWorkOrderOutcome.Found(workOrder("88", counterpart = expectedProvider)),
        )
        val useCase = GetWorkOrderDetailUseCase(
            detailRepository,
            GetTurnosUseCase(turnosRepository),
        )

        val outcome = useCase("88")

        assertEquals(1, turnosRepository.requestCount)
        assertEquals("88", detailRepository.requestedId)
        assertEquals(expectedProvider, detailRepository.requestedProvider)
        assertTrue("expected Found, was $outcome", outcome is GetWorkOrderOutcome.Found)
    }

    @Test
    fun propagates_turnos_network_failure_without_requesting_detail() = runTest {
        val cause = IOException("offline")
        val turnosRepository = RecordingTurnosRepository(TurnosOutcome.Failure.Network(cause))
        val detailRepository = RecordingWorkOrderDetailRepository(
            GetWorkOrderOutcome.Found(workOrder("88")),
        )
        val useCase = GetWorkOrderDetailUseCase(
            detailRepository,
            GetTurnosUseCase(turnosRepository),
        )

        val outcome = useCase("88")

        assertEquals(
            GetWorkOrderOutcome.Failure(ServiceProposalsOutcome.Failure.Network(cause)),
            outcome,
        )
        assertEquals(0, detailRepository.requestCount)
    }

    @Test
    fun propagates_turnos_server_failure_without_requesting_detail() = runTest {
        val failure = TurnosOutcome.Failure.Server(503, "down for maintenance")
        val detailRepository = RecordingWorkOrderDetailRepository(
            GetWorkOrderOutcome.Found(workOrder("88")),
        )
        val useCase = GetWorkOrderDetailUseCase(
            detailRepository,
            GetTurnosUseCase(RecordingTurnosRepository(failure)),
        )

        val outcome = useCase("88")

        assertEquals(
            GetWorkOrderOutcome.Failure(ServiceProposalsOutcome.Failure.Server(503, "down for maintenance")),
            outcome,
        )
        assertEquals(0, detailRepository.requestCount)
    }

    @Test
    fun returns_not_found_when_turnos_do_not_contain_the_work_order() = runTest {
        val detailRepository = RecordingWorkOrderDetailRepository(
            GetWorkOrderOutcome.Found(workOrder("88")),
        )
        val useCase = GetWorkOrderDetailUseCase(
            detailRepository,
            GetTurnosUseCase(
                RecordingTurnosRepository(
                    TurnosOutcome.Success(
                        listOf(turno("99", provider("21", "Otra", "Persona", "Electricidad"))),
                    ),
                ),
            ),
        )

        assertEquals(GetWorkOrderOutcome.NotFound, useCase("88"))
        assertEquals(0, detailRepository.requestCount)
    }

    @Test
    fun uses_supplied_provider_without_loading_turnos() = runTest {
        val expectedProvider = provider("20", "Actualizada", "Prestadora", "Plomería")
        val turnosRepository = RecordingTurnosRepository(TurnosOutcome.Success(emptyList()))
        val detailRepository = RecordingWorkOrderDetailRepository(
            GetWorkOrderOutcome.Found(workOrder("88", counterpart = expectedProvider)),
        )
        val useCase = GetWorkOrderDetailUseCase(
            detailRepository,
            GetTurnosUseCase(turnosRepository),
        )

        val outcome = useCase("88", expectedProvider)

        assertTrue("expected Found, was $outcome", outcome is GetWorkOrderOutcome.Found)
        assertEquals(0, turnosRepository.requestCount)
        assertEquals("88", detailRepository.requestedId)
        assertEquals(expectedProvider, detailRepository.requestedProvider)
    }

    private fun useCase(repository: WorkOrderDetailRepository): GetWorkOrderDetailUseCase =
        GetWorkOrderDetailUseCase(
            repository,
            GetTurnosUseCase(RecordingTurnosRepository(TurnosOutcome.Success(emptyList()))),
        )

    private fun workOrder(
        proposalId: String = "wo-1",
        description: String = "Fuga en el lavamanos",
        amountCents: Long = 1_500_000L,
        scheduledOnEpochMillis: Long = 1_792_074_600_000L,
        acceptedOnEpochMillis: Long = 1_788_434_364_640L,
        counterpart: WorkOrderDetailCounterpart = provider(),
    ): WorkOrderDetail = WorkOrderDetail(
        proposalId = proposalId,
        provider = counterpart,
        description = description,
        amountCents = amountCents,
        scheduledOnEpochMillis = scheduledOnEpochMillis,
        acceptedOnEpochMillis = acceptedOnEpochMillis,
        paidOnEpochMillis = null,
        status = TurnoStatus.Pending,
        completionReport = null,
        review = null,
        estimatedDurationMinutes = 90,
    )

    private fun provider(
        id: String = "100",
        name: String = "Carlos",
        surname: String = "López",
        categoryName: String = "Plomería",
    ): WorkOrderDetailCounterpart = WorkOrderDetailCounterpart(
        id = id,
        name = name,
        surname = surname,
        categoryName = categoryName,
        profilePhotoUrl = null,
    )

    private fun turno(id: String, provider: WorkOrderDetailCounterpart): Turno = Turno(
        id = id,
        serviceProposalId = "proposal-$id",
        status = TurnoStatus.Confirmed,
        counterpart = TurnoCounterpart(
            id = provider.id,
            name = provider.name,
            surname = provider.surname,
            categoryName = provider.categoryName,
            profilePhotoUrl = provider.profilePhotoUrl,
        ),
        description = "Work order $id",
        amountCents = 0L,
        scheduledOnEpochMillis = 0L,
    )

    private class RecordingTurnosRepository(
        private val outcome: TurnosOutcome,
    ) : TurnosRepository {
        var requestCount = 0
            private set

        override suspend fun getTurnos(): TurnosOutcome {
            requestCount += 1
            return outcome
        }
    }

    private class RecordingWorkOrderDetailRepository(
        private val outcome: GetWorkOrderOutcome,
    ) : WorkOrderDetailRepository {
        var requestCount = 0
            private set
        var requestedId: String? = null
            private set
        var requestedProvider: WorkOrderDetailCounterpart? = null
            private set

        override suspend fun getWorkOrderDetail(
            workOrderId: String,
            provider: WorkOrderDetailCounterpart?,
        ): GetWorkOrderOutcome {
            requestCount += 1
            requestedId = workOrderId
            requestedProvider = provider
            return if (provider == null) {
                GetWorkOrderOutcome.Failure(
                    ServiceProposalsOutcome.Failure.Server(0, "provider metadata missing"),
                )
            } else {
                outcome
            }
        }

        override suspend fun submitReview(
            workOrderId: String,
            rating: Int,
            description: String,
        ): SubmitWorkOrderReviewOutcome =
            SubmitWorkOrderReviewOutcome.Server(0, "submitReview not exercised")
    }

    private class FakeWorkOrderDetailRepository(
        private val detail: WorkOrderDetail? = null,
        private val failure: ServiceProposalsOutcome.Failure? = null,
    ) : WorkOrderDetailRepository {
        override suspend fun getWorkOrderDetail(
            workOrderId: String,
            provider: WorkOrderDetailCounterpart?,
        ): GetWorkOrderOutcome {
            if (provider == null) {
                return GetWorkOrderOutcome.Failure(
                    ServiceProposalsOutcome.Failure.Server(0, "provider metadata missing"),
                )
            }
            failure?.let { return GetWorkOrderOutcome.Failure(it) }
            return detail
                ?.takeIf { it.proposalId == workOrderId }
                ?.let { GetWorkOrderOutcome.Found(it) }
                ?: GetWorkOrderOutcome.NotFound
        }

        override suspend fun submitReview(
            workOrderId: String,
            rating: Int,
            description: String,
        ): SubmitWorkOrderReviewOutcome =
            SubmitWorkOrderReviewOutcome.Server(0, "submitReview not exercised by this test")
    }
}
