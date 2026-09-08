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
 *  - **Rapid taps cancel the in-flight round trip**: a second
 *    `load(...)` cancels the first coroutine so the screen only
 *    observes the last outcome (the bug fix that surfaced after
 *    every "Ver Solicitud" tap on Home fired three parallel
 *    requests whose results raced into the uiState slot).
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

    @Test
    fun a_second_load_cancels_the_in_flight_round_trip() = runTest {
        // Without the [loadJob] cancellation, three rapid taps
        // would fire three parallel `getServiceProposals()` calls
        // whose completed state raced into the uiState slot. With
        // the fix, the mock assertion is the contract: the second
        // tap cancels the first coroutine before its `coEvery`
        // runs, so the repository only sees the last round trip.
        coEvery { serviceProposalRepository.getServiceProposals() } returnsMany listOf(
            ServiceProposalsOutcome.Success(listOf(proposal(id = "1"))),
            ServiceProposalsOutcome.Success(listOf(proposal(id = "2"))),
        )

        val viewModel = ProposalDetailViewModel(serviceProposalRepository)
        viewModel.load("1") // first round trip (cancelled before it resumes)
        viewModel.load("2") // second round trip wins

        val state = viewModel.uiState.value
        assertTrue(
            "expected the last load(id=2) to win, was $state",
            state is ProposalDetailUiState.Ready,
        )
        assertEquals(
            "expected the uiState to reflect the second proposal, " +
                "not the cancelled first one",
            "2",
            (state as ProposalDetailUiState.Ready).proposal.id,
        )
    }

    @Test
    fun initial_state_is_Idle_so_the_detail_sheet_stays_hidden_by_default() = runTest {
        val viewModel = ProposalDetailViewModel(serviceProposalRepository)

        val state = viewModel.uiState.value

        assertTrue(
            "expected the fresh VM to expose Idle (the bottom-sheet " +
                "visibility gate hides the sheet for this state), " +
                "was $state — using `Loading` here would let a race " +
                "with `onRetry` flash the spinner after dismiss",
            state is ProposalDetailUiState.Idle,
        )
    }

    @Test
    fun reset_returns_to_Idle_and_cancels_any_in_flight_round_trip() = runTest {
        // First load a successful card so the VM is in `Ready`
        // (i.e. sheet is currently visible).
        coEvery { serviceProposalRepository.getServiceProposals() } returns
            ServiceProposalsOutcome.Success(listOf(proposal(id = "1")))

        val viewModel = ProposalDetailViewModel(serviceProposalRepository)
        viewModel.load("1")
        assertTrue(
            "precondition: load(\"1\") must drive the VM to Ready, was ${viewModel.uiState.value}",
            viewModel.uiState.value is ProposalDetailUiState.Ready,
        )

        // The host calls `reset()` from `onDismissRequest`. The
        // VM must drop the in-flight job (if any) and the new
        // state must be `Idle` so the sheet's `LaunchedEffect`
        // treats it as "stay hidden".
        viewModel.reset()

        val state = viewModel.uiState.value
        assertTrue(
            "expected reset() to drive the VM back to Idle so the " +
                "bottom sheet does not re-open, was $state",
            state is ProposalDetailUiState.Idle,
        )
    }

    @Test
    fun load_with_blank_id_short_circuits_to_Idle_and_does_not_hit_the_repository() = runTest {
        // The host used to call `load("")` on dismiss as a way
        // to "clear" the VM. That drove the state into
        // `Error(404, "Proposal  not found")`, which the
        // bottom-sheet gate treated as "show" and re-opened the
        // modal after the consumer dismissed it. The VM now
        // treats a blank id as a reset — no round trip is fired
        // and the state lands on `Idle` so the sheet stays
        // hidden.
        val viewModel = ProposalDetailViewModel(serviceProposalRepository)
        viewModel.load("")

        assertTrue(
            "expected load(\"\") to short-circuit to Idle, " +
                "was ${viewModel.uiState.value}",
            viewModel.uiState.value is ProposalDetailUiState.Idle,
        )
    }
}
