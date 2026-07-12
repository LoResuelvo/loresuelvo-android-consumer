package com.loresuelvo.consumer.ui.professional

import com.loresuelvo.consumer.domain.provider.Provider

/**
 * UDF state for the providers list of a single category. Modelled
 * as a sealed hierarchy so the screen renders exactly one of the
 * states without boolean flags.
 *
 * The lifetime is tied to a single category view: every new
 * `loadProviders(categoryId, categoryName)` call replaces the state
 * (no stale cross-category list keeps rendering).
 */
sealed interface ProfessionalsUiState {

    val categoryName: String

    data class Loading(override val categoryName: String) : ProfessionalsUiState

    data class Ready(
        override val categoryName: String,
        val providers: List<Provider>,
    ) : ProfessionalsUiState

    data class Empty(override val categoryName: String) : ProfessionalsUiState

    data class Error(override val categoryName: String) : ProfessionalsUiState
}
