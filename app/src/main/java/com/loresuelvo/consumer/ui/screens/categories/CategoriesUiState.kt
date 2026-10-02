package com.loresuelvo.consumer.ui.screens.categories

import com.loresuelvo.consumer.ui.screens.home.CategoriesState

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
