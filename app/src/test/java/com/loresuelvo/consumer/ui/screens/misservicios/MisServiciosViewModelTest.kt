package com.loresuelvo.consumer.ui.screens.misservicios

import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalCounterpart
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import com.loresuelvo.consumer.domain.usecase.serviceproposal.GetAllServiceProposalsUseCase
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
 *  - Round trip succeeds with items → `Ready(items)`.
 *  - Round trip succeeds with empty list → `Ready(empty)` (the
 *    screen renders the empty card; per `MessagesListUiState`'s
 *    rationale, "no proposals" is a presentation concern, not a
 *    separate state).
 *  - Round trip fails Network → `Error(failure)`.
 *  - Round trip fails Server → `Error(failure)`.
 *  - Manual `load()` after failure re-fires the use case
 *    (retry path).
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
    )

    private fun proposal(
        id: String,
        status: ServiceProposalStatus = ServiceProposalStatus.Pending,
        amountCents: Long = 1500000L,
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
        createdOnEpochMillis = 1_788_434_364_640L,
    )

    @Test
    fun success_with_items_transitions_to_Ready_with_the_list() = runTest {
        val proposals = listOf(
            proposal(id = "1"),
            proposal(id = "2", status = ServiceProposalStatus.Accepted),
            proposal(id = "3", status = ServiceProposalStatus.Rejected),
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
            listOf("1", "2", "3"),
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
}
