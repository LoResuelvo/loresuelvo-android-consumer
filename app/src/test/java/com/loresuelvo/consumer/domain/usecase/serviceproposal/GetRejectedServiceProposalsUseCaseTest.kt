package com.loresuelvo.consumer.domain.usecase.serviceproposal

import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalCounterpart
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [GetRejectedServiceProposalsUseCase].
 *
 * Mirrors [GetPendingServiceProposalsUseCaseTest] and
 * [GetAcceptedServiceProposalsUseCaseTest]: the use case is a
 * thin orchestrator that filters the repository result down to
 * [ServiceProposalStatus.Rejected] entries. Failures propagate
 * verbatim so the VM can branch on the typed failure.
 *
 * This use case powers the "Rechazadas" filter chip on the
 * MisServicios surface (US-54 scenario 07-VSP). The 05-VSP commit
 * that wires it into the VM also benefits from this base because
 * the same DI graph binds the rejected repo through the use case.
 */
class GetRejectedServiceProposalsUseCaseTest {

    private val repository = mockk<ServiceProposalRepository>()
    private val useCase = GetRejectedServiceProposalsUseCase(repository)

    private fun proposal(
        id: String,
        status: ServiceProposalStatus,
    ): ServiceProposal = ServiceProposal(
        id = id,
        conversationId = null,
        status = status,
        counterpart = ServiceProposalCounterpart(
            id = "100",
            name = "Juan",
            surname = "Pérez",
            categoryName = "Plomería",
            profilePhotoUrl = null,
        ),
        description = "irrelevant",
        amountCents = 15000L,
        scheduledOnEpochMillis = 1_700_000_000_000L,
        createdOnEpochMillis = 1_699_999_000_000L,
    )

    @Test
    fun delegates_to_repository_and_returns_only_rejected_proposals() = runTest {
        val mixed = listOf(
            proposal(id = "1", status = ServiceProposalStatus.Pending),
            proposal(id = "2", status = ServiceProposalStatus.Accepted),
            proposal(id = "3", status = ServiceProposalStatus.Rejected),
            proposal(id = "4", status = ServiceProposalStatus.Rejected),
        )
        coEvery { repository.getServiceProposals() } returns
            ServiceProposalsOutcome.Success(mixed)

        val outcome = useCase()

        assertTrue("expected Success, was $outcome", outcome is ServiceProposalsOutcome.Success)
        val rejected = (outcome as ServiceProposalsOutcome.Success).proposals
        assertEquals(listOf("3", "4"), rejected.map { it.id })
        assertTrue(rejected.all { it.status == ServiceProposalStatus.Rejected })
        coVerify(exactly = 1) { repository.getServiceProposals() }
    }

    @Test
    fun empty_repository_response_yields_empty_rejected_list() = runTest {
        coEvery { repository.getServiceProposals() } returns
            ServiceProposalsOutcome.Success(emptyList())

        val outcome = useCase()

        assertTrue(outcome is ServiceProposalsOutcome.Success)
        assertEquals(emptyList<ServiceProposal>(), (outcome as ServiceProposalsOutcome.Success).proposals)
    }

    @Test
    fun propagates_network_failure_unchanged() = runTest {
        coEvery { repository.getServiceProposals() } returns
            ServiceProposalsOutcome.Failure.Network(IOException("dns"))

        val outcome = useCase()

        assertTrue(
            "expected Failure.Network, was $outcome",
            outcome is ServiceProposalsOutcome.Failure.Network,
        )
    }

    @Test
    fun propagates_server_failure_unchanged() = runTest {
        coEvery { repository.getServiceProposals() } returns
            ServiceProposalsOutcome.Failure.Server(code = 500, message = "boom")

        val outcome = useCase()

        assertTrue(
            "expected Failure.Server, was $outcome",
            outcome is ServiceProposalsOutcome.Failure.Server,
        )
        assertEquals(500, (outcome as ServiceProposalsOutcome.Failure.Server).code)
    }
}
