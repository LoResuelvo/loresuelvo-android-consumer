package com.loresuelvo.consumer.ui.professional

import com.loresuelvo.consumer.domain.category.CategoriesOutcome
import com.loresuelvo.consumer.domain.category.CategoryRepository
import com.loresuelvo.consumer.domain.provider.Provider
import com.loresuelvo.consumer.domain.provider.ProviderRepository
import com.loresuelvo.consumer.domain.provider.ProvidersOutcome
import com.loresuelvo.consumer.domain.usecase.category.GetCategoriesUseCase
import com.loresuelvo.consumer.domain.usecase.provider.GetProvidersByCategoryUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfessionalsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun rating_sort_orders_providers_from_highest_to_lowest() = runTest(dispatcher) {
        val viewModel = viewModelWith(
            providers = listOf(
                provider("Juan", "Pérez", 4.8, 12),
                provider("Pedro", "Díaz", 3.9, 5),
                provider("Ana", "Gómez", 4.5, 8),
            ),
        )

        viewModel.loadProviders(1, "Plomería")
        advanceUntilIdle()
        viewModel.onSortOrderChanged(ProviderSortOrder.RatingDescending)

        val state = viewModel.uiState.value as ProfessionalsUiState.Ready
        assertEquals(listOf("Juan", "Ana", "Pedro"), state.providers.map { it.name })
    }

    @Test
    fun minimum_rating_filter_excludes_unrated_and_lower_rated_providers() = runTest(dispatcher) {
        val viewModel = viewModelWith(
            providers = listOf(
                provider("Juan", "Pérez", 4.8, 12),
                provider("Pedro", "Díaz", 3.9, 5),
                provider("Sin", "Reseñas", 0.0, 0),
            ),
        )

        viewModel.loadProviders(1, "Plomería")
        advanceUntilIdle()
        viewModel.onMinimumRatingChanged(4.0)

        val state = viewModel.uiState.value as ProfessionalsUiState.Ready
        assertEquals(listOf("Juan"), state.providers.map { it.name })
        assertEquals(4.0, requireNotNull(state.minimumRating), 0.0)
    }

    @Test
    fun clearing_rating_filter_restores_all_providers() = runTest(dispatcher) {
        val viewModel = viewModelWith(
            providers = listOf(
                provider("Juan", "Pérez", 4.8, 12),
                provider("Pedro", "Díaz", 3.9, 5),
            ),
        )

        viewModel.loadProviders(1, "Plomería")
        advanceUntilIdle()
        viewModel.onMinimumRatingChanged(4.0)
        viewModel.clearRatingFilter()

        val state = viewModel.uiState.value as ProfessionalsUiState.Ready
        assertEquals(listOf("Juan", "Pedro"), state.providers.map { it.name })
        assertEquals(null, state.minimumRating)
    }

    @Test
    fun rating_filter_exposes_specific_empty_state_when_no_provider_matches() = runTest(dispatcher) {
        val viewModel = viewModelWith(
            providers = listOf(provider("Juan", "Pérez", 4.8, 12)),
        )

        viewModel.loadProviders(1, "Plomería")
        advanceUntilIdle()
        viewModel.onMinimumRatingChanged(5.0)

        val state = viewModel.uiState.value
        assertTrue(state is ProfessionalsUiState.FilteredEmpty)
        assertEquals(5.0, (state as ProfessionalsUiState.FilteredEmpty).minimumRating, 0.0)
    }

    private fun viewModelWith(providers: List<Provider>): ProfessionalsViewModel =
        ProfessionalsViewModel(
            getProviders = GetProvidersByCategoryUseCase(FakeProviderRepository(providers)),
            getCategories = GetCategoriesUseCase(FakeCategoryRepository()),
        )

    private fun provider(
        name: String,
        surname: String,
        ratingAverage: Double,
        ratingCount: Int,
    ) = Provider(
        id = name.hashCode(),
        name = name,
        surname = surname,
        categoryId = 1,
        categoryName = "Plomería",
        profilePhotoUrl = null,
        ratingAverage = ratingAverage,
        ratingCount = ratingCount,
    )

    private class FakeProviderRepository(
        private val providers: List<Provider>,
    ) : ProviderRepository {
        override suspend fun getProvidersByCategory(categoryId: Int): ProvidersOutcome =
            ProvidersOutcome.Success(providers)
    }

    private class FakeCategoryRepository : CategoryRepository {
        override suspend fun getCategories(): CategoriesOutcome =
            CategoriesOutcome.Success(emptyList())
    }
}
