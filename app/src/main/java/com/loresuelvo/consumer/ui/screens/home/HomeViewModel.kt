package com.loresuelvo.consumer.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loresuelvo.consumer.domain.category.CategoriesOutcome
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import com.loresuelvo.consumer.domain.turno.TurnoStatus
import com.loresuelvo.consumer.domain.turno.TurnosOutcome
import com.loresuelvo.consumer.domain.usecase.category.GetCategoriesUseCase
import com.loresuelvo.consumer.domain.usecase.serviceproposal.GetAcceptedServiceProposalsUseCase
import com.loresuelvo.consumer.domain.usecase.serviceproposal.GetPendingServiceProposalsUseCase
import com.loresuelvo.consumer.domain.usecase.turno.GetTurnosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UDF ViewModel for the consumer Home screen. Loads four
 * parallel surfaces on first composition:
 *
 *  - The category grid, via [GetCategoriesUseCase] (pre-US-54).
 *  - The "Propuestas que requieren atención" section (US-54,
 *    scenario 01-VSP), via [GetPendingServiceProposalsUseCase].
 *  - The "Trabajos próximos" section (US-54, scenario 02-VSP),
 *    via [GetAcceptedServiceProposalsUseCase].
 *  - The "Pagos pendientes" + "Mis Turnos" preview sections, via
 *    [GetTurnosUseCase] (visualize-turns.feature scenario 01-VT
 *    + the awaiting-payment follow-up). The same `GET /work-orders`
 *    response feeds both sections; the VM derives one filter for
 *    the upcoming preview (`[ScheduledDateComparator] closest-to-
 *    now` + take `MAX_TURNOS_ON_HOME`) and one for the
 *    awaiting-payment block (`TurnoStatus.AwaitingPayment`).
 *
 * The four round trips are launched in parallel coroutines on
 * `viewModelScope`. The global [HomeUiState] (Loading / Ready /
 * Error) is driven by the categories round trip only: that is
 * the action without which the Home dashboard is not usable, so
 * a categories failure flips the global to [HomeUiState.Error]
 * and the screen surfaces the retry CTA. Each proposals / turnos
 * round trip only mutates its own sub-state, preserving whatever
 * global branch the categories round trip landed in. This split
 * keeps the state machine deadlock-free when the coroutines race:
 * whichever lands first only mutates its slice, never stuck on
 * Loading because the other round trip was slow.
 */

/**
 * Maximum number of categories surfaced on the Home grid. Anything
 * beyond that lives behind the "Ver todas" link (placeholder for now).
 * This is a UI decision, not a domain rule; the use case still returns
 * the full list.
 */
private const val MAX_CATEGORIES_ON_HOME = 6

/**
 * Maximum number of scheduled appointments surfaced on the Home
 * "Mis Turnos" preview row. The full list lives behind the "Ver
 * todas" link to `Route.Turnos`. Two is enough to convey "you have
 * something coming up" without crowding the dashboard — the
 * dedicated screen renders the rest.
 */
private const val MAX_TURNOS_ON_HOME = 2

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getCategories: GetCategoriesUseCase,
    private val getPendingServiceProposals: GetPendingServiceProposalsUseCase,
    private val getAcceptedServiceProposals: GetAcceptedServiceProposalsUseCase,
    private val getTurnos: GetTurnosUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
        loadPendingServiceProposals()
        loadUpcomingServiceProposals()
        loadTurnos()
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
                            upcomingServiceProposals = current.upcomingServiceProposals,
                            awaitingPaymentTurnos = current.awaitingPaymentTurnos,
                            turnos = current.turnos,
                        )
                    }
                }
                is CategoriesOutcome.Failure ->
                    _uiState.update { current ->
                        HomeUiState.Error(
                            messageResId = com.loresuelvo.consumer.R.string.welcome_categories_error,
                            pendingServiceProposals = current.pendingServiceProposals,
                            upcomingServiceProposals = current.upcomingServiceProposals,
                            awaitingPaymentTurnos = current.awaitingPaymentTurnos,
                            turnos = current.turnos,
                        )
                    }
            }
        }
    }

    fun loadPendingServiceProposals() {
        viewModelScope.launch {
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

    fun loadUpcomingServiceProposals() {
        viewModelScope.launch {
            _uiState.update { current ->
                withUpcomingServiceProposals(current, ServiceProposalsState.Loading)
            }
            when (val outcome = getAcceptedServiceProposals()) {
                is ServiceProposalsOutcome.Success ->
                    _uiState.update { current ->
                        withUpcomingServiceProposals(
                            current,
                            ServiceProposalsState.Ready(outcome.proposals),
                        )
                    }
                is ServiceProposalsOutcome.Failure ->
                    _uiState.update { current ->
                        withUpcomingServiceProposals(current, ServiceProposalsState.Error)
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

    private fun withUpcomingServiceProposals(
        current: HomeUiState,
        new: ServiceProposalsState,
    ): HomeUiState = when (current) {
        is HomeUiState.Loading -> current.copy(upcomingServiceProposals = new)
        is HomeUiState.Ready -> current.copy(upcomingServiceProposals = new)
        is HomeUiState.Error -> current.copy(upcomingServiceProposals = new)
    }

    /**
     * Loads the consumer's scheduled appointments for the Home
     * "Mis Turnos" preview row + the "Pagos pendientes" section.
     * The same `GET /work-orders` response feeds both:
     *  - the upcoming preview filters + sorts by closest
     *    `scheduledOnEpochMillis` and takes `MAX_TURNOS_ON_HOME`.
     *  - the awaiting-payment block filters by
     *    [TurnoStatus.AwaitingPayment] (no MAX cap; in practice
     *    the count is tiny — 0..1 — so showing them all is fine
     *    and keeps the "clear your pending balance" signal
     *    loud).
     *
     * Failures land in [TurnosState.Error] so the dedicated Mis
     * Turnos screen can render the typed retry CTA while the
     * rest of the dashboard keeps working.
     */
    fun loadTurnos() {
        viewModelScope.launch {
            _uiState.update { current ->
                withTurnosState(
                    current,
                    awaitingPayment = TurnosState.Loading,
                    turnos = TurnosState.Loading,
                )
            }
            when (val outcome = getTurnos()) {
                is TurnosOutcome.Success -> {
                    val all = outcome.turnos
                    val awaiting = all
                        .filter { it.status == TurnoStatus.AwaitingPayment }
                        .sortedBy { it.scheduledOnEpochMillis }
                    val upcoming = all
                        .sortedBy { it.scheduledOnEpochMillis }
                        .take(MAX_TURNOS_ON_HOME)
                    _uiState.update { current ->
                        withTurnosState(
                            current,
                            awaitingPayment = TurnosState.Ready(awaiting),
                            turnos = TurnosState.Ready(upcoming),
                        )
                    }
                }
                is TurnosOutcome.Failure ->
                    _uiState.update { current ->
                        withTurnosState(
                            current,
                            awaitingPayment = TurnosState.Error,
                            turnos = TurnosState.Error,
                        )
                    }
            }
        }
    }

    /**
     * Atomic write to the two turnos sub-states (awaiting-payment
     * + upcoming preview). Both slots flip together so the screen
     * never renders a partial update where the awaiting-payment
     * block landed but the upcoming preview is still `Loading`.
     */
    private fun withTurnosState(
        current: HomeUiState,
        awaitingPayment: TurnosState,
        turnos: TurnosState,
    ): HomeUiState = when (current) {
        is HomeUiState.Loading -> current.copy(
            awaitingPaymentTurnos = awaitingPayment,
            turnos = turnos,
        )
        is HomeUiState.Ready -> current.copy(
            awaitingPaymentTurnos = awaitingPayment,
            turnos = turnos,
        )
        is HomeUiState.Error -> current.copy(
            awaitingPaymentTurnos = awaitingPayment,
            turnos = turnos,
        )
    }
}
