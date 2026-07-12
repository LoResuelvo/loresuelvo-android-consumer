package com.loresuelvo.consumer.bdd.providers.search

import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.auth.User
import com.loresuelvo.consumer.domain.category.Category
import com.loresuelvo.consumer.domain.category.CategoryRepository
import com.loresuelvo.consumer.domain.category.CategoriesOutcome
import com.loresuelvo.consumer.domain.provider.Provider
import com.loresuelvo.consumer.domain.provider.ProvidersOutcome
import com.loresuelvo.consumer.domain.provider.ProviderRepository
import com.loresuelvo.consumer.domain.usecase.category.GetCategoriesUseCase
import com.loresuelvo.consumer.domain.usecase.provider.GetProvidersByCategoryUseCase
import com.loresuelvo.consumer.ui.auth.CompleteProfileSessionStoreStub
import com.loresuelvo.consumer.ui.professional.ProfessionalsUiState
import com.loresuelvo.consumer.ui.professional.ProfessionalsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * Per-scenario world for the search-providers BDD specs. Owns a
 * [StandardTestDispatcher] shared by the [ProfessionalsViewModel] and
 * the observation scope, so step defs can deterministically drive and
 * inspect the flow without Hilt, Compose, or a backend.
 *
 * The world is reconstructed per scenario by Cucumber JVM (one step
 * def instance per scenario); no state leaks across scenarios.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CucumberWorld : AutoCloseable {

    private val scheduler: TestCoroutineScheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(dispatcher + supervisorJob)

    private lateinit var providerRepo: FakeProviderRepository
    private lateinit var categoryRepo: FakeCategoryRepository
    private lateinit var sessionStore: AuthSessionStore
    private lateinit var viewModel: ProfessionalsViewModel

    private val observedUiStates: MutableList<ProfessionalsUiState> = mutableListOf()

    /**
     * Categories defined by the Background table. Populated by the
     * `Given the following categories exist:` step.
     */
    private val categoriesByName = mutableMapOf<String, Category>()
    private val categoriesById = mutableMapOf<Int, Category>()

    /**
     * Provider seed from the Background table, plus any per-scenario
     * overrides.
     */
    private val providers = mutableListOf<Provider>()

    /**
     * The category the user is currently inspecting (set by `Given I
     * am on the providers list for category "X"` or by a tap step).
     */
    private var currentCategoryId: Int? = null
    private var currentCategoryName: String? = null

    private var started: Boolean = false

    fun startScenario() {
        if (started) return
        started = true

        Dispatchers.setMain(dispatcher)

        val session = AuthSession(
            user = User(
                displayName = "Ana",
                firstName = "Ana",
                lastName = "Pérez",
                email = "ana@example.com",
            ),
            accessToken = "test-token",
        )
        sessionStore = StubSessionStore(session)

        providerRepo = FakeProviderRepository()
        categoryRepo = FakeCategoryRepository(categoriesById.values.toList())

        viewModel = ProfessionalsViewModel(
            getProviders = GetProvidersByCategoryUseCase(providerRepo),
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
            categoriesByName[name] = Category(id = id, name = name)
            categoryRepo.set(categoriesById.values.toList())
        }
        scheduler.advanceUntilIdle()
    }

    fun loadProviders(table: List<Map<String, String>>) {
        providers.clear()
        for (row in table) {
            val id = row.getValue("id")
            val name = row.getValue("name")
            val surname = row.getValue("surname")
            val categoryName = row.getValue("category_name")
            val categoryId = row.getValue("category_id").toInt()
            providers += Provider(
                id = id,
                name = name,
                surname = surname,
                categoryId = categoryId,
                categoryName = categoryName,
                profilePhotoUrl = null,
            )
        }
        providerRepo.setSeed(providers.toList())
    }

    fun visitProvidersFor(categoryName: String) {
        val cat = categoriesByName.getValue(categoryName)
        currentCategoryId = cat.id
        currentCategoryName = cat.name
        viewModel.loadProviders(cat.id, cat.name)
        scheduler.advanceUntilIdle()
    }

    fun tapCategoryCard(categoryName: String) {
        // The Home tap lands on the providers list for that category.
        visitProvidersFor(categoryName)
    }

    fun observeEmptyForCategory(categoryName: String) {
        val cat = categoriesByName.getValue(categoryName)
        providerRepo.setOverrideForCategory(cat.id, emptyList())
    }

    fun configureNetworkFailure() {
        providerRepo.setFailure(
            ProvidersOutcome.Failure.Network(IllegalStateException("network"))
        )
    }

    fun currentCategoryName(): String? = currentCategoryName

    fun expectedEmptyMessage(): String =
        "No se encontraron profesionales especializados en esta categoría"

    fun expectedErrorMessage(): String =
        "No pudimos cargar los profesionales. Revisá tu conexión e intentá de nuevo."

    fun lastUiState(): ProfessionalsUiState = observedUiStates.last()

    fun observedStates(): List<ProfessionalsUiState> = observedUiStates.toList()

    override fun close() {
        supervisorJob.cancel()
        Dispatchers.resetMain()
    }

    /**
     * Minimal AuthSessionStore stub: the BDD journey for this feature
     * doesn't need the real EncryptedSharedPreferences; only `getSession`
     * is touched by the consumer-home nav path.
     */
    private class StubSessionStore(
        initial: AuthSession?,
    ) : AuthSessionStore {
        private val flow = MutableStateFlow(initial)
        override val sessionFlow = flow
        override fun getSession(): AuthSession? = flow.value
        override fun saveSession(session: AuthSession) { flow.value = session }
        override fun clearSession() { flow.value = null }
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
