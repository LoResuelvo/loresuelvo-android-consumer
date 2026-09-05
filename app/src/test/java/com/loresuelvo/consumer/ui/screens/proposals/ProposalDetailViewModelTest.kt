package com.loresuelvo.consumer.ui.screens.proposals

import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalCounterpart
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import io.mockk.coEvery
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [ProposalDetailViewModel].
 *
 *  - Success path with a matching id lands in [ProposalDetailUiState.Ready]
 *    carrying the matching proposal.
 *  - Success path without a matching id lands in [ProposalDetailUiState.Error]
 *    carrying a 404 (`Server`-typed failure so the screen can
 *    surface the retry CTA).
 *  - Network failure propagates verbatim so the screen renders the
 *    "no internet" copy.
 *  - Server failure propagates verbatim.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProposalDetailViewModelTest {

    private val serviceProposalRepository = mockk<ServiceProposalRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun proposal(id: String, status: ServiceProposalStatus = ServiceProposalStatus.Pending): ServiceProposal =
        ServiceProposal(
            id = id,
            conversationId = "100",
            status = status,
            counterpart = ServiceProposalCounterpart(
                id = "1",
                name = "Juan",
                surname = "Pérez",
                categoryName = "Plomería",
                profilePhotoUrl = null,
            ),
            description = "Fuga en el lavamanos",
            amountCents = 1500000L,
            scheduledOnEpochMillis = 1_792_074_600_000L,
            createdOnEpochMillis = 1_788_434_364_640L,
        )

    @Test
    fun success_with_matching_id_transitions_to_Ready_with_the_proposal() = runTest {
        val proposals = listOf(
            proposal(id = "1"),
            proposal(id = "2", status = ServiceProposalStatus.Accepted),
        )
        coEvery { serviceProposalRepository.getServiceProposals() } returns
            ServiceProposalsOutcome.Success(proposals)

        val viewModel = ProposalDetailViewModel(serviceProposalRepository)
        viewModel.load("2")

        val state = viewModel.uiState.value
        assertTrue("expected Ready, was $state", state is ProposalDetailUiState.Ready)
        val ready = state as ProposalDetailUiState.Ready
        assertEquals("2", ready.proposal.id)
        assertEquals(ServiceProposalStatus.Accepted, ready.proposal.status)
    }

    @Test
    fun success_without_matching_id_transitions_to_Error_with_404() = runTest {
        coEvery { serviceProposalRepository.getServiceProposals() } returns
            ServiceProposalsOutcome.Success(listOf(proposal(id = "1")))

        val viewModel = ProposalDetailViewModel(serviceProposalRepository)
        viewModel.load("missing")

        val state = viewModel.uiState.value
        assertTrue("expected Error, was $state", state is ProposalDetailUiState.Error)
        val error = (state as ProposalDetailUiState.Error).failure
        assertTrue(error is ServiceProposalsOutcome.Failure.Server)
        error as ServiceProposalsOutcome.Failure.Server
        assertEquals(404, error.code)
    }

    @Test
    fun network_failure_propagates_verbatim() = runTest {
        coEvery { serviceProposalRepository.getServiceProposals() } returns
            ServiceProposalsOutcome.Failure.Network(IOException("dns"))

        val viewModel = ProposalDetailViewModel(serviceProposalRepository)
        viewModel.load("1")

        val state = viewModel.uiState.value
        assertTrue(state is ProposalDetailUiState.Error)
        val error = (state as ProposalDetailUiState.Error).failure
        assertTrue(error is ServiceProposalsOutcome.Failure.Network)
    }

    @Test
    fun server_failure_propagates_verbatim() = runTest {
        coEvery { serviceProposalRepository.getServiceProposals() } returns
            ServiceProposalsOutcome.Failure.Server(code = 503, message = "boom")

        val viewModel = ProposalDetailViewModel(serviceProposalRepository)
        viewModel.load("1")

        val state = viewModel.uiState.value
        assertTrue(state is ProposalDetailUiState.Error)
        val error = (state as ProposalDetailUiState.Error).failure
        assertTrue(error is ServiceProposalsOutcome.Failure.Server)
        error as ServiceProposalsOutcome.Failure.Server
        assertEquals(503, error.code)
    }
}
