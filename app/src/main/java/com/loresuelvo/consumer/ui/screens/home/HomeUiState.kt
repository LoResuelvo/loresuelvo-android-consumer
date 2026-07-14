package com.loresuelvo.consumer.ui.screens.home

import com.loresuelvo.consumer.domain.category.Category

/**
 * UDF state for the consumer Home screen.
 *
 *  - [Loading]: categories are still being fetched.
 *  - [Ready]: categories loaded; render the grid.
 *  - [Error]: categories fetch failed; the screen shows a retry CTA.
 *
 * `activeRequests` and `recentDiagnoses` are intentionally **not**
 * part of this state yet — they're mocked from the screen until the
 * backend exposes those resources.
 */
sealed interface HomeUiState {

    val categories: CategoriesState

    data class Loading(override val categories: CategoriesState = CategoriesState.Loading) :
        HomeUiState

    data class Ready(
        override val categories: CategoriesState,
    ) : HomeUiState

    data class Error(
        override val categories: CategoriesState = CategoriesState.Error,
        val messageResId: Int,
    ) : HomeUiState
}

sealed interface CategoriesState {
    data object Loading : CategoriesState
    data class Ready(val items: List<Category>) : CategoriesState
    data object Error : CategoriesState
}
