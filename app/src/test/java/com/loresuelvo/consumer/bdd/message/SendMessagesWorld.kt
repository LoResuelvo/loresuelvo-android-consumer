package com.loresuelvo.consumer.bdd.message

import com.loresuelvo.consumer.domain.category.CategoriesOutcome
import com.loresuelvo.consumer.domain.category.Category
import com.loresuelvo.consumer.domain.category.CategoryRepository
import com.loresuelvo.consumer.domain.provider.Provider
import com.loresuelvo.consumer.domain.provider.ProviderRepository
import com.loresuelvo.consumer.domain.provider.ProvidersOutcome
import com.loresuelvo.consumer.domain.usecase.category.GetCategoriesUseCase
import com.loresuelvo.consumer.domain.usecase.provider.GetProvidersByCategoryUseCase
import com.loresuelvo.consumer.ui.professional.ProfessionalsUiState
import com.loresuelvo.consumer.ui.professional.ProfessionalsViewModel
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
 * Per-scenario world for the US-17 "Start a conversation with a
 * provider" BDD specs. Wires the [ProfessionalsViewModel] against
 * an in-memory [ProviderRepository] + [CategoryRepository] so the
 * scenarios can drive the search results list and assert on the
 * observed [ProfessionalsUiState].
 *
 * The world is self-contained — it does NOT share state with the
 * search-providers BDD world's `SearchProvidersCucumberWorld`. The
 * shared `Given` steps (login, providers, etc.) live in the search
 * glue package and operate on the search world's VM; this world
 * drives a SEPARATE [ProfessionalsViewModel] instance so the
 * messaging BDD scenarios observe their own state transitions.
 *
 * Hard-coded provider + category fixtures match the Background of
 * the upstream `search-providers.feature` so the scenarios test
 * the same fixture data. As scenarios 02-IC onwards are landed,
 * the helpers will gain `openContact`, `submit`, etc. (mirroring
 * `ContactProviderWorld`).
 */
class SendMessagesWorld : AutoCloseable {

    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(dispatcher + supervisorJob)

    private val fakeProviderRepo = FakeProviderRepository()
    private val fakeCategoryRepo = FakeCategoryRepository()
    private lateinit var viewModel: ProfessionalsViewModel

    private val observedUiStates = mutableListOf<ProfessionalsUiState>()

    private val knownProviders: Map<String, Provider> = mapOf(
        "Juan Pérez" to Provider(
            id = 1,
            name = "Juan",
            surname = "Pérez",
            categoryId = 1,
            categoryName = "Plomería",
            profilePhotoUrl = null,
        ),
        "Pedro Dib" to Provider(
            id = 2,
            name = "Pedro",
            surname = "Dib",
            categoryId = 1,
            categoryName = "Plomería",
            profilePhotoUrl = null,
        ),
    )

    private val knownCategories: Map<String, Category> = mapOf(
        "Plomería" to Category(id = 1, name = "Plomería"),
        "Electricidad" to Category(id = 2, name = "Electricidad"),
        "Gas" to Category(id = 3, name = "Gas"),
    )

    private var started = false

    fun startScenario() {
        if (started) return
        started = true

        Dispatchers.setMain(dispatcher)

        viewModel = ProfessionalsViewModel(
            getProviders = GetProvidersByCategoryUseCase(fakeProviderRepo),
            getCategories = GetCategoriesUseCase(fakeCategoryRepo),
        )

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.uiState.collect { observedUiStates += it }
        }

        scheduler.advanceUntilIdle()
    }

    /**
     * Loads the providers that belong to [categoryName] and drives
     * the VM through [ProfessionalsViewModel.loadProviders]. The
     * state transitions to `Ready` with the matching list.
     */
    fun loadProvidersForCategory(categoryName: String) {
        val category = knownCategories[categoryName]
            ?: error("Unknown category: $categoryName (BDD has ${knownCategories.keys})")
        fakeProviderRepo.setSeed(
            knownProviders.values.filter { it.categoryId == category.id },
        )
        viewModel.loadProviders(category.id, category.name)
        scheduler.advanceUntilIdle()
    }

    fun lastUiState(): ProfessionalsUiState = observedUiStates.last()

    fun observedStates(): List<ProfessionalsUiState> = observedUiStates.toList()

    override fun close() {
        supervisorJob.cancel()
        Dispatchers.resetMain()
    }

    private class FakeProviderRepository : ProviderRepository {
        private val providers = mutableListOf<Provider>()

        fun setSeed(providers: List<Provider>) {
            this.providers.clear()
            this.providers.addAll(providers)
        }

        override suspend fun getProvidersByCategory(categoryId: Int): ProvidersOutcome =
            ProvidersOutcome.Success(providers.filter { it.categoryId == categoryId })
    }

    private class FakeCategoryRepository : CategoryRepository {
        override suspend fun getCategories(): CategoriesOutcome =
            CategoriesOutcome.Success(emptyList())
    }
}
