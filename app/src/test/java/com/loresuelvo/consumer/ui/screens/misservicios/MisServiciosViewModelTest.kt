package com.loresuelvo.consumer.ui.screens.misservicios

import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalCounterpart
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import com.loresuelvo.consumer.domain.usecase.serviceproposal.GetAcceptedServiceProposalsUseCase
import com.loresuelvo.consumer.domain.usecase.serviceproposal.GetAllServiceProposalsUseCase
import com.loresuelvo.consumer.domain.usecase.serviceproposal.GetPendingServiceProposalsUseCase
import com.loresuelvo.consumer.domain.usecase.serviceproposal.GetRejectedServiceProposalsUseCase
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [MisServiciosViewModel]. Mirrors
 * `MessagesListViewModelTest`: the VM is constructed **inside**
 * each test (after `coEvery` stubs the round trip) because its
 * `init { load() }` launches a coroutine that, with
 * `StandardTestDispatcher`, would sit on the scheduler until
 * pumped. `UnconfinedTestDispatcher` inside `runTest` runs the
 * coroutine eagerly so the post-`init` state is observable
 * synchronously.
 *
 * Coverage:
 *  - Round trip succeeds with items → `Ready(items)` ordered by
 *    recency (US-54 scenario 04-VSP).
 *  - Round trip succeeds with empty list → `Ready(empty)`.
 *  - Round trip fails Network → `Error(failure)`.
 *  - Round trip fails Server → `Error(failure)`.
 *  - Manual `load()` after failure re-fires the use case
 *    (retry path).
 *  - Filter by status routes the round trip through the matching
 *    per-status use case (US-54 scenario 05-VSP / 06-VSP /
 *    07-VSP). `selectedStatusFilter` survives the route change
 *    and survives `Error → load()` (retry).
 *  - `selectedStatusFilter = null` (= "Todos") routes through
 *    `GetAllServiceProposalsUseCase`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MisServiciosViewModelTest {

    private val serviceProposalRepository = mockk<ServiceProposalRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(): MisServiciosViewModel = MisServiciosViewModel(
        getAllServiceProposals = GetAllServiceProposalsUseCase(serviceProposalRepository),
        getPendingServiceProposals = GetPendingServiceProposalsUseCase(serviceProposalRepository),
        getAcceptedServiceProposals = GetAcceptedServiceProposalsUseCase(serviceProposalRepository),
        getRejectedServiceProposals = GetRejectedServiceProposalsUseCase(serviceProposalRepository),
    )

    private fun proposal(
        id: String,
        status: ServiceProposalStatus = ServiceProposalStatus.Pending,
        amountCents: Long = 1500000L,
        createdOnEpochMillis: Long = 1_788_434_364_640L,
    ): ServiceProposal = ServiceProposal(
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
        amountCents = amountCents,
        scheduledOnEpochMillis = 1_792_074_600_000L,
        createdOnEpochMillis = createdOnEpochMillis,
    )

    private val mixedSeed: List<ServiceProposal> = listOf(
        proposal(id = "1", status = ServiceProposalStatus.Pending),
        proposal(id = "2", status = ServiceProposalStatus.Accepted),
        proposal(id = "3", status = ServiceProposalStatus.Rejected),
    )

    @Test
    fun success_with_items_transitions_to_Ready_with_the_list_ordered_by_recency() = runTest {
        // The seed is NOT in chronological order on purpose, so
        // the assertion pins that the VM observes the list after
        // the use case's `sortedByDescending { createdOnEpochMillis }`.
        // If a future change drops the sort, this test fails.
        val proposals = listOf(
            proposal(id = "middle", createdOnEpochMillis = 1_700_000_000_000L),
            proposal(id = "newest", createdOnEpochMillis = 1_700_000_500_000L),
            proposal(id = "oldest", createdOnEpochMillis = 1_699_999_500_000L),
        )
        coEvery { serviceProposalRepository.getServiceProposals() } returns
            ServiceProposalsOutcome.Success(proposals)

        val viewModel = buildViewModel()

        val state = viewModel.uiState.value
        assertTrue(
            "expected MisServiciosUiState.Ready, was $state",
            state is MisServiciosUiState.Ready,
        )
        val ready = state as MisServiciosUiState.Ready
        assertEquals(
            listOf("newest", "middle", "oldest"),
            ready.proposals.map { it.id },
        )
    }

    @Test
    fun success_with_empty_list_transitions_to_Ready_with_emptyList() = runTest {
        coEvery { serviceProposalRepository.getServiceProposals() } returns
            ServiceProposalsOutcome.Success(emptyList())

        val viewModel = buildViewModel()

        val state = viewModel.uiState.value
        assertTrue(state is MisServiciosUiState.Ready)
        assertEquals(
            emptyList<ServiceProposal>(),
            (state as MisServiciosUiState.Ready).proposals,
        )
    }

    @Test
    fun failure_Network_transitions_to_Error_carrying_the_failure() = runTest {
        val cause = IOException("dns")
        coEvery { serviceProposalRepository.getServiceProposals() } returns
            ServiceProposalsOutcome.Failure.Network(cause)

        val viewModel = buildViewModel()

        val state = viewModel.uiState.value
        assertTrue(
            "expected MisServiciosUiState.Error, was $state",
            state is MisServiciosUiState.Error,
        )
        val failure = (state as MisServiciosUiState.Error).failure
        assertTrue(failure is ServiceProposalsOutcome.Failure.Network)
        assertEquals(cause, (failure as ServiceProposalsOutcome.Failure.Network).cause)
    }

    @Test
    fun failure_Server_transitions_to_Error_carrying_code_and_message() = runTest {
        coEvery { serviceProposalRepository.getServiceProposals() } returns
            ServiceProposalsOutcome.Failure.Server(code = 503, message = "unavailable")

        val viewModel = buildViewModel()

        val state = viewModel.uiState.value
        assertTrue(state is MisServiciosUiState.Error)
        val failure = (state as MisServiciosUiState.Error).failure
        assertTrue(failure is ServiceProposalsOutcome.Failure.Server)
        failure as ServiceProposalsOutcome.Failure.Server
        assertEquals(503, failure.code)
        assertEquals("unavailable", failure.message)
    }

    @Test
    fun load_re_fires_use_case_after_failure() = runTest {
        coEvery { serviceProposalRepository.getServiceProposals() } returns
            ServiceProposalsOutcome.Failure.Server(500, "boom") andThen
            ServiceProposalsOutcome.Success(listOf(proposal(id = "99")))

        val viewModel = buildViewModel()
        assertTrue(viewModel.uiState.value is MisServiciosUiState.Error)

        viewModel.load()
        val state = viewModel.uiState.value
        assertTrue(state is MisServiciosUiState.Ready)
        assertEquals(
            listOf("99"),
            (state as MisServiciosUiState.Ready).proposals.map { it.id },
        )
    }

    // ---- Filter by status (US-54 scenarios 05/06/07-VSP) ------

    @Test
    fun default_filter_is_null_and_shows_all_proposals() = runTest {
        coEvery { serviceProposalRepository.getServiceProposals() } returns
            ServiceProposalsOutcome.Success(mixedSeed)

        val viewModel = buildViewModel()

        val state = viewModel.uiState.value
        assertTrue(state is MisServiciosUiState.Ready)
        val ready = state as MisServiciosUiState.Ready
        assertEquals(null, ready.selectedStatusFilter)
        assertEquals(
            "expected all three mixed-status proposals when no filter is selected",
            listOf("1", "2", "3"),
            ready.proposals.map { it.id },
        )
    }

    @Test
    fun filter_by_pending_returns_only_pending_proposals() = runTest {
        // The GetPending use case filters server-side, but the
        // fake repository is what the VM ultimately calls. Stub it
        // to return only the Pending entries so the test pins
        // "filter by Pending routes through the Pending use case".
        coEvery { serviceProposalRepository.getServiceProposals() } returnsMany listOf(
            ServiceProposalsOutcome.Success(listOf(proposal(id = "1"))),
            ServiceProposalsOutcome.Success(mixedSeed),
        )

        val viewModel = buildViewModel()
        viewModel.onFilterSelected(ServiceProposalStatus.Pending)

        val state = viewModel.uiState.value
        assertTrue(state is MisServiciosUiState.Ready)
        val ready = state as MisServiciosUiState.Ready
        assertEquals(ServiceProposalStatus.Pending, ready.selectedStatusFilter)
        assertEquals(listOf("1"), ready.proposals.map { it.id })
    }

    @Test
    fun filter_by_accepted_returns_only_accepted_proposals() = runTest {
        coEvery { serviceProposalRepository.getServiceProposals() } returnsMany listOf(
            ServiceProposalsOutcome.Success(listOf(proposal(id = "2", status = ServiceProposalStatus.Accepted))),
            ServiceProposalsOutcome.Success(mixedSeed),
        )

        val viewModel = buildViewModel()
        viewModel.onFilterSelected(ServiceProposalStatus.Accepted)

        val state = viewModel.uiState.value
        assertTrue(state is MisServiciosUiState.Ready)
        val ready = state as MisServiciosUiState.Ready
        assertEquals(ServiceProposalStatus.Accepted, ready.selectedStatusFilter)
        assertEquals(listOf("2"), ready.proposals.map { it.id })
    }

    @Test
    fun filter_by_rejected_returns_only_rejected_proposals() = runTest {
        coEvery { serviceProposalRepository.getServiceProposals() } returnsMany listOf(
            ServiceProposalsOutcome.Success(listOf(proposal(id = "3", status = ServiceProposalStatus.Rejected))),
            ServiceProposalsOutcome.Success(mixedSeed),
        )

        val viewModel = buildViewModel()
        viewModel.onFilterSelected(ServiceProposalStatus.Rejected)

        val state = viewModel.uiState.value
        assertTrue(state is MisServiciosUiState.Ready)
        val ready = state as MisServiciosUiState.Ready
        assertEquals(ServiceProposalStatus.Rejected, ready.selectedStatusFilter)
        assertEquals(listOf("3"), ready.proposals.map { it.id })
    }

    @Test
    fun clearing_filter_returns_every_proposal() = runTest {
        coEvery { serviceProposalRepository.getServiceProposals() } returnsMany listOf(
            ServiceProposalsOutcome.Success(listOf(proposal(id = "1"))),
            ServiceProposalsOutcome.Success(listOf(proposal(id = "2"))),
            ServiceProposalsOutcome.Success(mixedSeed),
        )

        val viewModel = buildViewModel()
        viewModel.onFilterSelected(ServiceProposalStatus.Pending)
        viewModel.onFilterSelected(null) // "Todos"

        val state = viewModel.uiState.value
        assertTrue(state is MisServiciosUiState.Ready)
        val ready = state as MisServiciosUiState.Ready
        assertEquals(null, ready.selectedStatusFilter)
        assertEquals(listOf("1", "2", "3"), ready.proposals.map { it.id })
    }

    @Test
    fun filter_persists_across_retry() = runTest {
        // Three responses so each VM-initiated round trip is
        // deterministic: init.load() → Success, filter change →
        // Failure, retry → Success. The filter must survive
        // Loading → Ready → Loading → Error → Loading → Ready
        // without ever reverting to null.
        coEvery { serviceProposalRepository.getServiceProposals() } returnsMany listOf(
            ServiceProposalsOutcome.Success(mixedSeed),
            ServiceProposalsOutcome.Failure.Server(500, "boom"),
            ServiceProposalsOutcome.Success(listOf(proposal(id = "1"))),
        )

        val viewModel = buildViewModel()
        // init load() consumed the first Success; state is Ready(null).
        assertTrue(viewModel.uiState.value is MisServiciosUiState.Ready)
        assertEquals(null, (viewModel.uiState.value as MisServiciosUiState.Ready).selectedStatusFilter)

        viewModel.onFilterSelected(ServiceProposalStatus.Pending)
        // Second call returned Failure; state is Error(Pending).
        val errorState = viewModel.uiState.value
        assertTrue("expected Error, got $errorState", errorState is MisServiciosUiState.Error)
        assertEquals(
            ServiceProposalStatus.Pending,
            (errorState as MisServiciosUiState.Error).selectedStatusFilter,
        )

        viewModel.load() // retry
        val readyState = viewModel.uiState.value
        assertTrue(readyState is MisServiciosUiState.Ready)
        assertEquals(
            ServiceProposalStatus.Pending,
            (readyState as MisServiciosUiState.Ready).selectedStatusFilter,
        )
    }
}
