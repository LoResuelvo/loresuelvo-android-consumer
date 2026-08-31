package com.loresuelvo.consumer.ui.screens.categories

import com.loresuelvo.consumer.ui.screens.home.CategoriesState

/**
 * UDF state for the "all categories" screen (scenario 02-UXUI).
 * Reuses [CategoriesState] from the Home module — the Home grid
 * truncates to `MAX_CATEGORIES_ON_HOME`, this screen shows all.
 *
 * [searchQuery] is preserved across Loading / Error transitions
 * so the input keeps context when the network round-trip is in
 * flight or has failed.
 */
sealed interface CategoriesUiState {

    val categories: CategoriesState
    val searchQuery: String

    data class Loading(
        override val categories: CategoriesState = CategoriesState.Loading,
        override val searchQuery: String = "",
    ) : CategoriesUiState

    data class Ready(
        override val categories: CategoriesState,
        override val searchQuery: String = "",
    ) : CategoriesUiState

    data class Error(
        override val categories: CategoriesState = CategoriesState.Error,
        val messageResId: Int,
        override val searchQuery: String = "",
    ) : CategoriesUiState
}
