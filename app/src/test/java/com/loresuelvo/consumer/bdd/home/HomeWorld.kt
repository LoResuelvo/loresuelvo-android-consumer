package com.loresuelvo.consumer.bdd.home

import com.loresuelvo.consumer.domain.category.Category
import com.loresuelvo.consumer.domain.category.CategoryRepository
import com.loresuelvo.consumer.domain.category.CategoriesOutcome
import com.loresuelvo.consumer.domain.usecase.category.GetCategoriesUseCase
import com.loresuelvo.consumer.ui.screens.home.CategoriesState
import com.loresuelvo.consumer.ui.screens.home.HomeUiState
import com.loresuelvo.consumer.ui.screens.home.HomeViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * Per-scenario world for the consumer-home BDD specs. Owns a
 * [StandardTestDispatcher] shared by the [HomeViewModel] and the
 * observation scope, so step defs can deterministically drive and
 * inspect the flow without Hilt, Compose, or a backend.
 *
 * The world is reconstructed per scenario by Cucumber JVM (one step
 * def instance per scenario); no state leaks across scenarios.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HomeWorld : AutoCloseable {

    private val scheduler: TestCoroutineScheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(dispatcher + supervisorJob)

    private lateinit var categoryRepo: FakeCategoryRepository
    private lateinit var viewModel: HomeViewModel

    private val observedUiStates: MutableList<HomeUiState> = mutableListOf()

    /**
     * Categories defined by the Background table. Populated by the
     * `Given the backend exposes the following categories:` step.
     */
    private val categoriesById = mutableMapOf<Int, Category>()

    private var started: Boolean = false

    fun startScenario() {
        if (started) return
        started = true

        Dispatchers.setMain(dispatcher)

        categoryRepo = FakeCategoryRepository(categoriesById.values.toList())

        viewModel = HomeViewModel(
            getCategories = GetCategoriesUseCase(categoryRepo),
        )

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.uiState.collect { observedUiStates += it }
        }

        scheduler.advanceUntilIdle()
    }

    fun loadCategories(table: List<Map<String, String>>) {
        for (row in table) {
            val id = row.getValue("id").toInt()
            val name = row.getValue("name")
            categoriesById[id] = Category(id = id, name = name)
        }
        categoryRepo.set(categoriesById.values.toList())
    }

    fun openHome() {
        // The VM fetches categories in `init`; the test setup also
        // seeds the fake repo before this call, so a manual reload is
        // not strictly required. We trigger one anyway so the flow is
        // explicit and robust against future VM changes.
        viewModel.loadCategories()
        scheduler.advanceUntilIdle()
    }

    fun lastUiState(): HomeUiState = observedUiStates.last()

    fun visibleCategoryNames(): List<String> {
        val state = lastUiState()
        val items = (state as? HomeUiState.Ready)
            ?.categories
            ?.let { it as? CategoriesState.Ready }
            ?.items
            ?: error("state must be Ready with a populated categories list, was $state")
        return items.map { it.name }
    }

    override fun close() {
        supervisorJob.cancel()
        Dispatchers.resetMain()
    }

    private class FakeCategoryRepository(
        initial: List<Category>,
    ) : CategoryRepository {
        private var current = initial
        override suspend fun getCategories(): CategoriesOutcome =
            CategoriesOutcome.Success(current)
        fun set(categories: List<Category>) {
            current = categories
        }
    }
}