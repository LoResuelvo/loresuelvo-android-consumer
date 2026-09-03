package com.loresuelvo.consumer.ui.screens.home

import com.loresuelvo.consumer.domain.category.CategoriesOutcome
import com.loresuelvo.consumer.domain.category.Category
import com.loresuelvo.consumer.domain.category.CategoryRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalCounterpart
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalRepository
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import com.loresuelvo.consumer.domain.usecase.category.GetCategoriesUseCase
import com.loresuelvo.consumer.domain.usecase.serviceproposal.GetPendingServiceProposalsUseCase
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
 * Unit tests for [HomeViewModel] covering the US-54 scenario 01-VSP
 * branches: the "pending proposals" surface renders independently
 * of the categories surface (success / empty / network failure /
 * server failure) while preserving the categories outcome that
 * was already in place before US-54.
 *
 * Mirrors `MessagesListViewModelTest`: each test wraps in
 * [runTest], configures the stubs with `coEvery`, builds the VM,
 * and lets `runTest` drain the launched coroutines on teardown.
 * The VM is constructed **inside** each test (after the stubs)
 * because its `init { loadCategories(); loadPendingServiceProposals() }`
 * launches coroutines that would dispatch on the unstubbed
 * repository if the construction happened earlier.
 *
 * `Dispatchers.Main` is bound to an [UnconfinedTestDispatcher] so
 * the launched coroutines start executing eagerly without needing
 * an explicit `advanceUntilIdle()` (runTest's own scheduler still
 * tracks them and verifies they finish). This matches the
 * `MessagesListViewModelTest` pattern exactly.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val categoryRepository = mockk<CategoryRepository>()
    private val serviceProposalRepository = mockk<ServiceProposalRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(): HomeViewModel = HomeViewModel(
        getCategories = GetCategoriesUseCase(categoryRepository),
        getPendingServiceProposals = GetPendingServiceProposalsUseCase(serviceProposalRepository),
    )

    private fun pendingProposal(
        id: String,
        providerName: String = "Juan",
    ): ServiceProposal = ServiceProposal(
        id = id,
        conversationId = "100",
        status = ServiceProposalStatus.Pending,
        counterpart = ServiceProposalCounterpart(
            id = "1",
            name = providerName,
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
    fun categories_and_pending_proposals_both_succeed_lands_in_Ready() = runTest {
        coEvery { categoryRepository.getCategories() } returns CategoriesOutcome.Success(
            listOf(
                Category(id = 1, name = "Plomería"),
                Category(id = 2, name = "Electricidad"),
            ),
        )
        coEvery { serviceProposalRepository.getServiceProposals() } returns
            ServiceProposalsOutcome.Success(
                listOf(
                    pendingProposal(id = "1", providerName = "Carlos"),
                    pendingProposal(id = "2", providerName = "Ana"),
                ),
            )

        val viewModel = buildViewModel()

        val state = viewModel.uiState.value
        assertTrue("expected Ready, got ${state::class.simpleName}", state is HomeUiState.Ready)
        val ready = state as HomeUiState.Ready
        assertTrue(ready.categories is CategoriesState.Ready)
        assertEquals(
            listOf("Electricidad", "Plomería"),
            (ready.categories as CategoriesState.Ready).items.map { it.name },
        )
        val pending = ready.pendingServiceProposals
        assertTrue(pending is ServiceProposalsState.Ready)
        assertEquals(
            listOf("1", "2"),
            (pending as ServiceProposalsState.Ready).items.map { it.id },
        )
        assertEquals(
            listOf("Carlos", "Ana"),
            pending.items.map { it.counterpart.name },
        )
    }

    @Test
    fun pending_proposals_empty_list_renders_Ready_with_empty_items() = runTest {
        coEvery { categoryRepository.getCategories() } returns
            CategoriesOutcome.Success(listOf(Category(id = 1, name = "Plomería")))
        coEvery { serviceProposalRepository.getServiceProposals() } returns
            ServiceProposalsOutcome.Success(emptyList())

        val viewModel = buildViewModel()

        val state = viewModel.uiState.value
        assertTrue(state is HomeUiState.Ready)
        val pending = (state as HomeUiState.Ready).pendingServiceProposals
        assertTrue(
            "expected Ready(empty), got $pending",
            pending is ServiceProposalsState.Ready &&
                (pending as ServiceProposalsState.Ready).items.isEmpty(),
        )
    }

    @Test
    fun pending_proposals_network_failure_does_not_break_categories() = runTest {
        coEvery { categoryRepository.getCategories() } returns
            CategoriesOutcome.Success(listOf(Category(id = 1, name = "Plomería")))
        coEvery { serviceProposalRepository.getServiceProposals() } returns
            ServiceProposalsOutcome.Failure.Network(IOException("dns"))

        val viewModel = buildViewModel()

        val state = viewModel.uiState.value
        assertTrue("expected Ready, got ${state::class.simpleName}", state is HomeUiState.Ready)
        val ready = state as HomeUiState.Ready
        assertTrue(ready.categories is CategoriesState.Ready)
        assertEquals(ServiceProposalsState.Error, ready.pendingServiceProposals)
    }

    @Test
    fun pending_proposals_server_failure_does_not_break_categories() = runTest {
        coEvery { categoryRepository.getCategories() } returns
            CategoriesOutcome.Success(listOf(Category(id = 1, name = "Plomería")))
        coEvery { serviceProposalRepository.getServiceProposals() } returns
            ServiceProposalsOutcome.Failure.Server(code = 500, message = "boom")

        val viewModel = buildViewModel()

        val state = viewModel.uiState.value
        assertTrue(state is HomeUiState.Ready)
        assertEquals(
            ServiceProposalsState.Error,
            (state as HomeUiState.Ready).pendingServiceProposals,
        )
    }

    @Test
    fun categories_failure_still_surfaces_pending_proposals() = runTest {
        coEvery { categoryRepository.getCategories() } returns
            CategoriesOutcome.Failure.Server(code = 500, message = "boom")
        coEvery { serviceProposalRepository.getServiceProposals() } returns
            ServiceProposalsOutcome.Success(listOf(pendingProposal(id = "1")))

        val viewModel = buildViewModel()

        val state = viewModel.uiState.value
        assertTrue("expected Error, got ${state::class.simpleName}", state is HomeUiState.Error)
        val error = state as HomeUiState.Error
        assertEquals(CategoriesState.Error, error.categories)
        assertTrue(
            "expected Ready, got ${error.pendingServiceProposals}",
            error.pendingServiceProposals is ServiceProposalsState.Ready,
        )
    }
}
