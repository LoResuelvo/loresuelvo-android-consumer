package com.loresuelvo.consumer.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loresuelvo.consumer.domain.category.CategoriesOutcome
import com.loresuelvo.consumer.domain.usecase.category.GetCategoriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UDF ViewModel for the consumer Home screen. Loads the public
 * service categories via [GetCategoriesUseCase] on first
 * composition, exposes a [HomeUiState] with [CategoriesState]
 * carrying the list (or Loading / Error).
 *
 * The Home screen is the entry point of the authenticated flow, so
 * everything we need to render is fetched from the backend; the
 * only mocked data lives in the screen itself (active requests,
 * recent diagnoses).
 */
/**
 * Maximum number of categories surfaced on the Home grid. Anything
 * beyond that lives behind the "Ver todas" link (placeholder for now).
 * This is a UI decision, not a domain rule; the use case still returns
 * the full list.
 */
private const val MAX_CATEGORIES_ON_HOME = 6

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getCategories: GetCategoriesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    fun loadCategories() {
        viewModelScope.launch {
            _uiState.update {
                HomeUiState.Loading(categories = it.categories)
            }
            val next = when (val outcome = getCategories()) {
                is CategoriesOutcome.Success -> {
                    val visible = outcome.categories
                        .sortedBy { it.name.lowercase() }
                        .take(MAX_CATEGORIES_ON_HOME)
                    HomeUiState.Ready(
                        categories = CategoriesState.Ready(visible),
                    )
                }
                is CategoriesOutcome.Failure ->
                    HomeUiState.Error(
                        categories = CategoriesState.Error,
                        messageResId = com.loresuelvo.consumer.R.string.welcome_categories_error,
                    )
            }
            _uiState.update { next }
        }
    }
}
