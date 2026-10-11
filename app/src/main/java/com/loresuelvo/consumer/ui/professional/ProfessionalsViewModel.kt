package com.loresuelvo.consumer.ui.professional

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loresuelvo.consumer.domain.provider.Provider
import com.loresuelvo.consumer.domain.provider.ProvidersOutcome
import com.loresuelvo.consumer.domain.usecase.category.GetCategoriesUseCase
import com.loresuelvo.consumer.domain.usecase.provider.GetProvidersByCategoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UDF ViewModel for the providers-by-category screen. Drives the
 * `GET /providers?category_id=…` call through
 * [GetProvidersByCategoryUseCase] and maps the typed
 * [ProvidersOutcome] into the sealed [ProfessionalsUiState].
 *
 *  - `Success` with a non-empty list -> [ProfessionalsUiState.Ready].
 *  - `Success` with an empty list  -> [ProfessionalsUiState.Empty].
 *  - `Failure` (network/server)    -> [ProfessionalsUiState.Error].
 *
 * `categoryName` is stored alongside the state so the screen can
 * render the header (and the error / empty messages) without
 * re-deriving it from nav args.
 */
@HiltViewModel
class ProfessionalsViewModel @Inject constructor(
    private val getProviders: GetProvidersByCategoryUseCase,
    @Suppress("unused") private val getCategories: GetCategoriesUseCase,
) : ViewModel() {

    private var loadedProviders: List<Provider> = emptyList()
    private var loadedCategoryName: String = ""
    private var sortOrder: ProviderSortOrder = ProviderSortOrder.Default
    private var minimumRating: Double? = null

    private val _uiState = MutableStateFlow<ProfessionalsUiState>(
        ProfessionalsUiState.Loading(categoryName = ""),
    )
    val uiState: StateFlow<ProfessionalsUiState> = _uiState.asStateFlow()

    /**
     * Loads the providers for [categoryId] and updates the UI state.
     * The category name is shown by the header in
     * `ProfessionalsScreen`; it is not validated against the backend.
     */
    fun loadProviders(categoryId: Int, categoryName: String) {
        viewModelScope.launch {
            _uiState.update { ProfessionalsUiState.Loading(categoryName) }
            val next = when (val outcome = getProviders(categoryId)) {
                is ProvidersOutcome.Success ->
                    outcome.providers.let { providers ->
                        loadedProviders = providers
                        loadedCategoryName = categoryName
                        sortOrder = ProviderSortOrder.Default
                        minimumRating = null
                        stateForCurrentFilters()
                    }
                is ProvidersOutcome.Failure ->
                    ProfessionalsUiState.Error(categoryName)
            }
            _uiState.update { next }
        }
    }

    fun onSortOrderChanged(nextSortOrder: ProviderSortOrder) {
        sortOrder = nextSortOrder
        _uiState.update { stateForCurrentFilters() }
    }

    fun onMinimumRatingChanged(nextMinimumRating: Double?) {
        minimumRating = nextMinimumRating
        _uiState.update { stateForCurrentFilters() }
    }

    fun clearRatingFilter() {
        onMinimumRatingChanged(null)
    }

    private fun stateForCurrentFilters(): ProfessionalsUiState {
        if (loadedProviders.isEmpty()) {
            return ProfessionalsUiState.Empty(loadedCategoryName)
        }

        val filteredProviders = loadedProviders
            .asSequence()
            .filter { provider ->
                minimumRating == null || (
                    provider.ratingCount > 0 &&
                        provider.ratingAverage >= minimumRating!!
                    )
            }
            .let { providers ->
                when (sortOrder) {
                    ProviderSortOrder.Default -> providers
                    ProviderSortOrder.RatingDescending -> providers.sortedWith(
                        compareByDescending<Provider> { it.ratingAverage }
                            .thenByDescending { it.ratingCount }
                            .thenBy { it.id },
                    )
                }
            }
            .toList()

        return if (filteredProviders.isEmpty()) {
            ProfessionalsUiState.FilteredEmpty(
                categoryName = loadedCategoryName,
                sortOrder = sortOrder,
                minimumRating = requireNotNull(minimumRating),
            )
        } else {
            ProfessionalsUiState.Ready(
                categoryName = loadedCategoryName,
                providers = filteredProviders,
                sortOrder = sortOrder,
                minimumRating = minimumRating,
            )
        }
    }
}
