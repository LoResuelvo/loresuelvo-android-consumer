package com.loresuelvo.consumer.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.category.CategoriesOutcome
import com.loresuelvo.consumer.domain.category.Category
import com.loresuelvo.consumer.domain.usecase.category.GetCategoriesUseCase
import com.loresuelvo.consumer.ui.screens.home.CategoriesState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UDF ViewModel for the "all categories" screen reached from
 * the Home "Ver todas" link (scenario 02-UXUI).
 *
 * Unlike the Home VM, this one does NOT truncate the result to
 * `MAX_CATEGORIES_ON_HOME` so the consumer sees every category
 * the platform publishes. [allCategories] holds the canonical
 * dataset so the search filter can be re-applied on every query
 * change and after a network retry.
 */
@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val getCategories: GetCategoriesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CategoriesUiState>(CategoriesUiState.Loading())
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    /** Canonical dataset, `null` until the first successful fetch. */
    private var allCategories: List<Category>? = null

    init {
        loadCategories()
    }

    fun loadCategories() {
        viewModelScope.launch {
            _uiState.update {
                CategoriesUiState.Loading(
                    categories = it.categories,
                    searchQuery = it.searchQuery,
                )
            }
            val currentQuery = _uiState.value.searchQuery
            val next = when (val outcome = getCategories()) {
                is CategoriesOutcome.Success -> {
                    val sorted = outcome.categories.sortedBy { it.name.lowercase() }
                    allCategories = sorted
                    CategoriesUiState.Ready(
                        categories = CategoriesState.Ready(
                            items = applyFilter(sorted, currentQuery),
                        ),
                        searchQuery = currentQuery,
                    )
                }
                is CategoriesOutcome.Failure.Network ->
                    CategoriesUiState.Error(
                        categories = CategoriesState.Error,
                        messageResId = R.string.categories_error_body,
                        searchQuery = currentQuery,
                    )
                is CategoriesOutcome.Failure.Server ->
                    CategoriesUiState.Error(
                        categories = CategoriesState.Error,
                        messageResId = R.string.categories_error_body,
                        searchQuery = currentQuery,
                    )
            }
            _uiState.update { next }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { current ->
            when (current) {
                is CategoriesUiState.Ready -> {
                    val filtered = allCategories
                        ?.let { applyFilter(it, query) }
                        ?: emptyList()
                    current.copy(
                        searchQuery = query,
                        categories = CategoriesState.Ready(items = filtered),
                    )
                }
                is CategoriesUiState.Loading -> current.copy(searchQuery = query)
                is CategoriesUiState.Error -> current.copy(searchQuery = query)
            }
        }
    }

    private fun applyFilter(items: List<Category>, query: String): List<Category> =
        if (query.isBlank()) items
        else items.filter { it.name.contains(query, ignoreCase = true) }
}
