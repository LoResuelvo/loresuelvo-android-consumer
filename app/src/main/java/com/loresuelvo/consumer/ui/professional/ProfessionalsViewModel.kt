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
                    if (outcome.providers.isEmpty()) {
                        ProfessionalsUiState.Empty(categoryName)
                    } else {
                        ProfessionalsUiState.Ready(
                            categoryName = categoryName,
                            providers = outcome.providers,
                        )
                    }
                is ProvidersOutcome.Failure ->
                    ProfessionalsUiState.Error(categoryName)
            }
            _uiState.update { next }
        }
    }
}
