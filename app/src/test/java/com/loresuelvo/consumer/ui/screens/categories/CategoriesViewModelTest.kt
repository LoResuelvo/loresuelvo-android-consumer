package com.loresuelvo.consumer.ui.screens.categories

import com.loresuelvo.consumer.domain.category.CategoriesOutcome
import com.loresuelvo.consumer.domain.category.Category
import com.loresuelvo.consumer.domain.category.CategoryRepository
import com.loresuelvo.consumer.domain.usecase.category.GetCategoriesUseCase
import com.loresuelvo.consumer.ui.screens.home.CategoriesState
import io.mockk.coEvery
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [CategoriesViewModel] — the fine-grained twin
 * of the BDD scenario 02-UXUI. Covers the success / failure /
 * filter / retry-across-failure flows without Hilt.
 *
 * Scheduler note: a single [TestCoroutineScheduler] drives both
 * the VM's `viewModelScope` (via `Dispatchers.Main` set to a
 * [StandardTestDispatcher] backed by the same scheduler) and
 * the explicit `pump()` calls below. `runTest` would create a
 * second scheduler and the launched coroutine would never
 * drain.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CategoriesViewModelTest {

    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)
    private val repository = mockk<CategoryRepository>()
    private lateinit var useCase: GetCategoriesUseCase
    private lateinit var viewModel: CategoriesViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        useCase = GetCategoriesUseCase(repository)
        viewModel = CategoriesViewModel(useCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun pump() {
        testScheduler.advanceUntilIdle()
    }

    @Test
    fun success_emits_Ready_with_categories_sorted_alphabetically() {
        coEvery { repository.getCategories() } returns CategoriesOutcome.Success(
            listOf(
                Category(id = 1, name = "Plomería"),
                Category(id = 2, name = "Albañilería"),
                Category(id = 3, name = "Gas"),
            ),
        )

        pump()

        val state = viewModel.uiState.value
        assertTrue("expected Ready, got ${state::class.simpleName}", state is CategoriesUiState.Ready)
        val ready = state as CategoriesUiState.Ready
        assertEquals(
            listOf("Albañilería", "Gas", "Plomería"),
            (ready.categories as CategoriesState.Ready).items.map { it.name },
        )
        assertEquals("", ready.searchQuery)
    }

    @Test
    fun network_failure_emits_Error() {
        coEvery { repository.getCategories() } returns
            CategoriesOutcome.Failure.Network(IOException("dns"))

        pump()

        assertTrue(
            "expected Error, got ${viewModel.uiState.value::class.simpleName}",
            viewModel.uiState.value is CategoriesUiState.Error,
        )
    }

    @Test
    fun server_failure_emits_Error() {
        coEvery { repository.getCategories() } returns
            CategoriesOutcome.Failure.Server(code = 500, message = "boom")

        pump()

        assertTrue(
            "expected Error, got ${viewModel.uiState.value::class.simpleName}",
            viewModel.uiState.value is CategoriesUiState.Error,
        )
    }

    @Test
    fun onSearchQueryChange_filters_case_insensitively_and_restores_on_blank() {
        coEvery { repository.getCategories() } returns CategoriesOutcome.Success(
            listOf(
                Category(id = 1, name = "Plomería"),
                Category(id = 2, name = "Pintura"),
                Category(id = 3, name = "Plomería Industrial"),
            ),
        )
        pump()

        viewModel.onSearchQueryChange("plome")
        val filtered = viewModel.uiState.value as CategoriesUiState.Ready
        assertEquals(
            listOf("Plomería", "Plomería Industrial"),
            (filtered.categories as CategoriesState.Ready).items.map { it.name },
        )
        assertEquals("plome", filtered.searchQuery)

        // Clear -> the canonical dataset must be restored (this
        // is the regression guard for the "filtered state
        // shadows the original list" bug).
        viewModel.onSearchQueryChange("")
        val cleared = viewModel.uiState.value as CategoriesUiState.Ready
        assertEquals(3, (cleared.categories as CategoriesState.Ready).items.size)
        assertEquals("", cleared.searchQuery)
    }

    @Test
    fun searchQuery_is_preserved_across_loadCategories_retry() {
        coEvery { repository.getCategories() } returns
            CategoriesOutcome.Failure.Network(IOException("first call"))
        pump()
        viewModel.onSearchQueryChange("pin")
        assertEquals(
            "pin",
            (viewModel.uiState.value as CategoriesUiState.Error).searchQuery,
        )

        coEvery { repository.getCategories() } returns CategoriesOutcome.Success(
            listOf(
                Category(id = 1, name = "Pintura"),
                Category(id = 2, name = "Plomería"),
            ),
        )
        viewModel.loadCategories()
        pump()

        val ready = viewModel.uiState.value as CategoriesUiState.Ready
        assertEquals("pin", ready.searchQuery)
        assertEquals(
            listOf("Pintura"),
            (ready.categories as CategoriesState.Ready).items.map { it.name },
        )
    }
}
