package com.loresuelvo.consumer.bdd.providers.rating

import com.loresuelvo.consumer.bdd.providers.search.FakeProviderRepository
import com.loresuelvo.consumer.domain.category.Category
import com.loresuelvo.consumer.domain.category.CategoriesOutcome
import com.loresuelvo.consumer.domain.category.CategoryRepository
import com.loresuelvo.consumer.domain.usecase.category.GetCategoriesUseCase
import com.loresuelvo.consumer.domain.provider.Provider
import com.loresuelvo.consumer.domain.usecase.provider.GetProvidersByCategoryUseCase
import com.loresuelvo.consumer.ui.professional.ProfessionalsUiState
import com.loresuelvo.consumer.ui.professional.ProfessionalsViewModel
import com.loresuelvo.consumer.ui.professional.ProviderSortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class ProviderRatingSearchWorld : AutoCloseable {

    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val providerRepository = FakeProviderRepository()
    private val categories = linkedMapOf<String, Category>()
    private val providers = mutableListOf<Provider>()
    private var nextProviderId = 1
    private var nextExpectedPosition = 0
    private lateinit var viewModel: ProfessionalsViewModel
    private lateinit var currentCategory: Category

    fun startScenario() {
        Dispatchers.setMain(dispatcher)
        viewModel = ProfessionalsViewModel(
            getProviders = GetProvidersByCategoryUseCase(providerRepository),
            getCategories = GetCategoriesUseCase(FakeCategoryRepository()),
        )
    }

    fun addCategories(firstName: String, secondName: String) {
        listOf(firstName, secondName).forEachIndexed { index, name ->
            categories[name] = Category(id = index + 1, name = name)
        }
    }

    fun showCategory(categoryName: String) {
        currentCategory = categories.getValue(categoryName)
        loadCurrentCategory()
    }

    fun setProviderRating(
        fullName: String,
        average: Double,
        count: Int,
        categoryName: String = currentCategory.name,
    ) {
        upsertProvider(fullName, categoryName).also { provider ->
            replaceProvider(
                provider.copy(
                    ratingAverage = average,
                    ratingCount = count,
                ),
            )
        }
        providerRepository.setSeed(providers.toList())
        if (::currentCategory.isInitialized) loadCurrentCategory()
    }

    fun removeProviderRating(fullName: String) {
        upsertProvider(fullName, currentCategory.name).also { provider ->
            replaceProvider(provider.copy(ratingAverage = 0.0, ratingCount = 0))
        }
        providerRepository.setSeed(providers.toList())
        if (::currentCategory.isInitialized) loadCurrentCategory()
    }

    fun loadCurrentCategory() {
        viewModel.loadProviders(currentCategory.id, currentCategory.name)
        scheduler.advanceUntilIdle()
    }

    fun selectBestRatingOrder() {
        viewModel.onSortOrderChanged(ProviderSortOrder.RatingDescending)
        scheduler.advanceUntilIdle()
        nextExpectedPosition = 0
    }

    fun selectMinimumRating(rating: Double) {
        viewModel.onMinimumRatingChanged(rating)
        scheduler.advanceUntilIdle()
    }

    fun clearRatingFilter() {
        viewModel.clearRatingFilter()
        scheduler.advanceUntilIdle()
    }

    fun assertNextProvider(fullName: String) {
        val visibleNames = visibleProviderNames()
        check(nextExpectedPosition < visibleNames.size) {
            "No provider remains at position $nextExpectedPosition"
        }
        check(visibleNames[nextExpectedPosition] == fullName) {
            "Expected $fullName at position $nextExpectedPosition, got ${visibleNames[nextExpectedPosition]}"
        }
        nextExpectedPosition++
    }

    fun provider(fullName: String): Provider = readyState().providers.first {
        it.fullName() == fullName
    }

    fun visibleProviderNames(): List<String> = when (val state = viewModel.uiState.value) {
        is ProfessionalsUiState.Ready -> state.providers.map { it.fullName() }
        else -> emptyList()
    }

    fun visibleProvidersCategory(): Set<String> = when (val state = viewModel.uiState.value) {
        is ProfessionalsUiState.Ready -> state.providers.map { it.categoryName }.toSet()
        else -> emptySet()
    }

    fun isFilteredEmpty(): Boolean = viewModel.uiState.value is ProfessionalsUiState.FilteredEmpty

    fun hasMinimumRatingFilter(): Boolean = when (val state = viewModel.uiState.value) {
        is ProfessionalsUiState.Ready -> state.minimumRating != null
        is ProfessionalsUiState.FilteredEmpty -> true
        else -> false
    }

    fun hasAllProvidersVisible(): Boolean =
        visibleProviderNames().toSet() == providers
            .filter { it.categoryId == currentCategory.id }
            .map { it.fullName() }
            .toSet()

    fun preservesRatingOrderAfterClear(): Boolean =
        visibleProviderNames() == providers
            .filter { it.categoryId == currentCategory.id }
            .sortedWith(
                compareByDescending<Provider> { it.ratingAverage }
                    .thenByDescending { it.ratingCount }
                    .thenBy { it.id },
            )
            .map { it.fullName() }

    private fun readyState(): ProfessionalsUiState.Ready =
        viewModel.uiState.value as? ProfessionalsUiState.Ready
            ?: error("Expected Ready state, was ${viewModel.uiState.value}")

    private fun upsertProvider(fullName: String, categoryName: String): Provider {
        providers.firstOrNull { it.fullName() == fullName }?.let { return it }
        val category = categories.getValue(categoryName)
        val parts = fullName.trim().split(" ")
        val provider = Provider(
            id = nextProviderId++,
            name = parts.dropLast(1).joinToString(" "),
            surname = parts.last(),
            categoryId = category.id,
            categoryName = category.name,
            profilePhotoUrl = null,
        )
        providers += provider
        return provider
    }

    private fun replaceProvider(provider: Provider) {
        val index = providers.indexOfFirst { it.id == provider.id }
        providers[index] = provider
    }

    private fun Provider.fullName(): String = "$name $surname"

    override fun close() {
        Dispatchers.resetMain()
    }

    private class FakeCategoryRepository : CategoryRepository {
        override suspend fun getCategories(): CategoriesOutcome =
            CategoriesOutcome.Success(emptyList())
    }
}
