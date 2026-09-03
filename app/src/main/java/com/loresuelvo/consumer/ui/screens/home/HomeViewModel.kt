package com.loresuelvo.consumer.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loresuelvo.consumer.domain.category.CategoriesOutcome
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import com.loresuelvo.consumer.domain.usecase.category.GetCategoriesUseCase
import com.loresuelvo.consumer.domain.usecase.serviceproposal.GetPendingServiceProposalsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UDF ViewModel for the consumer Home screen. Loads two parallel
 * surfaces on first composition:
 *
 *  - The category grid, via [GetCategoriesUseCase] (pre-US-54).
 *  - The "Propuestas que requieren atención" section introduced
 *    by US-54 (scenario 01-VSP), via
 *    [GetPendingServiceProposalsUseCase].
 *
 * The two round trips are launched in parallel coroutines on
 * `viewModelScope`. The global [HomeUiState] (Loading / Ready /
 * Error) is driven by the categories round trip only: that is
 * the action without which the Home dashboard is not usable, so
 * a categories failure flips the global to [HomeUiState.Error]
 * and the screen surfaces the retry CTA. The proposals round
 * trip only mutates [HomeUiState.pendingServiceProposals],
 * preserving whatever global branch the categories round trip
 * landed in. This split keeps the state machine deadlock-free
 * when both coroutines race: whichever lands first only mutates
 * its slice, never stuck on Loading because the other round
 * trip was slow.
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
    private val getPendingServiceProposals: GetPendingServiceProposalsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
        loadPendingServiceProposals()
    }

    fun loadCategories() {
        viewModelScope.launch {
            when (val outcome = getCategories()) {
                is CategoriesOutcome.Success -> {
                    val visible = outcome.categories
                        .sortedBy { it.name.lowercase() }
                        .take(MAX_CATEGORIES_ON_HOME)
                    _uiState.update { current ->
                        HomeUiState.Ready(
                            categories = CategoriesState.Ready(visible),
                            pendingServiceProposals = current.pendingServiceProposals,
                        )
                    }
                }
                is CategoriesOutcome.Failure ->
                    _uiState.update { current ->
                        HomeUiState.Error(
                            messageResId = com.loresuelvo.consumer.R.string.welcome_categories_error,
                            pendingServiceProposals = current.pendingServiceProposals,
                        )
                    }
            }
        }
    }

    fun loadPendingServiceProposals() {
        viewModelScope.launch {
            // Surface the "loading" slice on the section without
            // touching the global state — the categories grid keeps
            // rendering whatever it was rendering.
            _uiState.update { current ->
                withPendingServiceProposals(current, ServiceProposalsState.Loading)
            }
            when (val outcome = getPendingServiceProposals()) {
                is ServiceProposalsOutcome.Success ->
                    _uiState.update { current ->
                        withPendingServiceProposals(
                            current,
                            ServiceProposalsState.Ready(outcome.proposals),
                        )
                    }
                is ServiceProposalsOutcome.Failure ->
                    _uiState.update { current ->
                        withPendingServiceProposals(current, ServiceProposalsState.Error)
                    }
            }
        }
    }

    private fun withPendingServiceProposals(
        current: HomeUiState,
        new: ServiceProposalsState,
    ): HomeUiState = when (current) {
        is HomeUiState.Loading -> current.copy(pendingServiceProposals = new)
        is HomeUiState.Ready -> current.copy(pendingServiceProposals = new)
        is HomeUiState.Error -> current.copy(pendingServiceProposals = new)
    }
}
