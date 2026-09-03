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
 * Unit tests for [GetPendingServiceProposalsUseCase].
 *
 * The use case is a thin orchestrator: it must delegate to the
 * [ServiceProposalRepository] port and filter the returned list
 * down to [ServiceProposalStatus.Pending] entries, propagating
 * any failure branch verbatim. It must NOT swallow failures into
 * an empty `Success`.
 *
 * The BDD scenario 01-VSP asserts the "filter to pending" branch
 * through the ViewModel; this test pins the use case contract in
 * isolation so a regression in the VM does not mask a use-case
 * regression (and vice versa).
 */
class GetPendingServiceProposalsUseCaseTest {

    private val repository = mockk<ServiceProposalRepository>()
    private val useCase = GetPendingServiceProposalsUseCase(repository)

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
    fun delegates_to_repository_and_returns_only_pending_proposals() = runTest {
        val mixed = listOf(
            proposal(id = "1", status = ServiceProposalStatus.Pending),
            proposal(id = "2", status = ServiceProposalStatus.Accepted),
            proposal(id = "3", status = ServiceProposalStatus.Pending),
            proposal(id = "4", status = ServiceProposalStatus.Rejected),
            proposal(id = "5", status = ServiceProposalStatus.Pending),
        )
        coEvery { repository.getServiceProposals() } returns
            ServiceProposalsOutcome.Success(mixed)

        val outcome = useCase()

        assertTrue("expected Success, was $outcome", outcome is ServiceProposalsOutcome.Success)
        val pending = (outcome as ServiceProposalsOutcome.Success).proposals
        assertEquals(listOf("1", "3", "5"), pending.map { it.id })
        assertTrue(pending.all { it.status == ServiceProposalStatus.Pending })
        coVerify(exactly = 1) { repository.getServiceProposals() }
    }

    @Test
    fun empty_repository_response_yields_empty_pending_list() = runTest {
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
        assertTrue((outcome as ServiceProposalsOutcome.Failure.Network).cause is IOException)
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
