package com.loresuelvo.consumer.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loresuelvo.consumer.domain.category.CategoriesOutcome
import com.loresuelvo.consumer.domain.assistant.AiConversationListOutcome
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import com.loresuelvo.consumer.domain.turno.TurnoStatus
import com.loresuelvo.consumer.domain.turno.TurnosOutcome
import com.loresuelvo.consumer.domain.usecase.category.GetCategoriesUseCase
import com.loresuelvo.consumer.domain.usecase.assistant.GetAiConversationsUseCase
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
 * Maximum number of categories surfaced on the Home grid. Anything
 * beyond that lives behind the "Ver todas" link.
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
private const val MAX_AI_CONVERSATIONS_ON_HOME = 3

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getCategories: GetCategoriesUseCase,
    private val getPendingServiceProposals: GetPendingServiceProposalsUseCase,
    private val getAcceptedServiceProposals: GetAcceptedServiceProposalsUseCase,
    private val getTurnos: GetTurnosUseCase,
    private val getAiConversations: GetAiConversationsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var categoriesRequestId = 0L
    private var pendingProposalsRequestId = 0L
    private var upcomingProposalsRequestId = 0L
    private var turnosRequestId = 0L
    private var aiConversationsRequestId = 0L

    init {
        refresh()
    }

    fun refresh() {
        loadCategories()
        loadPendingServiceProposals()
        loadUpcomingServiceProposals()
        loadTurnos()
        loadRecentAiConversations()
    }

    fun loadCategories() {
        val requestId = ++categoriesRequestId
        viewModelScope.launch {
            when (val outcome = getCategories()) {
                is CategoriesOutcome.Success -> {
                    if (requestId != categoriesRequestId) return@launch
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
                            recentAiConversations = current.recentAiConversations,
                        )
                    }
                }
                is CategoriesOutcome.Failure -> {
                    if (requestId == categoriesRequestId) {
                        _uiState.update { current ->
                            HomeUiState.Error(
                                messageResId = com.loresuelvo.consumer.R.string.welcome_categories_error,
                                pendingServiceProposals = current.pendingServiceProposals,
                                upcomingServiceProposals = current.upcomingServiceProposals,
                                awaitingPaymentTurnos = current.awaitingPaymentTurnos,
                                turnos = current.turnos,
                                recentAiConversations = current.recentAiConversations,
                            )
                        }
                    }
                }
            }
        }
    }

    fun loadPendingServiceProposals() {
        val requestId = ++pendingProposalsRequestId
        viewModelScope.launch {
            _uiState.update { current ->
                withPendingServiceProposals(current, ServiceProposalsState.Loading)
            }
            when (val outcome = getPendingServiceProposals()) {
                is ServiceProposalsOutcome.Success -> {
                    if (requestId == pendingProposalsRequestId) {
                        _uiState.update { current ->
                            withPendingServiceProposals(
                                current,
                                ServiceProposalsState.Ready(outcome.proposals),
                            )
                        }
                    }
                }
                is ServiceProposalsOutcome.Failure -> {
                    if (requestId == pendingProposalsRequestId) {
                        _uiState.update { current ->
                            withPendingServiceProposals(current, ServiceProposalsState.Error)
                        }
                    }
                }
            }
        }
    }

    fun loadUpcomingServiceProposals() {
        val requestId = ++upcomingProposalsRequestId
        viewModelScope.launch {
            _uiState.update { current ->
                withUpcomingServiceProposals(current, ServiceProposalsState.Loading)
            }
            when (val outcome = getAcceptedServiceProposals()) {
                is ServiceProposalsOutcome.Success -> {
                    if (requestId == upcomingProposalsRequestId) {
                        _uiState.update { current ->
                            withUpcomingServiceProposals(
                                current,
                                ServiceProposalsState.Ready(outcome.proposals),
                            )
                        }
                    }
                }
                is ServiceProposalsOutcome.Failure -> {
                    if (requestId == upcomingProposalsRequestId) {
                        _uiState.update { current ->
                            withUpcomingServiceProposals(current, ServiceProposalsState.Error)
                        }
                    }
                }
            }
        }
    }

    fun loadRecentAiConversations() {
        val requestId = ++aiConversationsRequestId
        viewModelScope.launch {
            _uiState.update { current ->
                withRecentAiConversations(current, AiConversationsState.Loading)
            }
            when (val outcome = getAiConversations()) {
                is AiConversationListOutcome.Success -> {
                    if (requestId != aiConversationsRequestId) return@launch
                    val visible = outcome.conversations
                        .sortedByDescending { it.lastMessageAtEpochMillis }
                        .take(MAX_AI_CONVERSATIONS_ON_HOME)
                    _uiState.update { current ->
                        withRecentAiConversations(
                            current,
                            AiConversationsState.Ready(visible),
                        )
                    }
                }
                is AiConversationListOutcome.Failure -> {
                    if (requestId != aiConversationsRequestId) return@launch
                    _uiState.update { current ->
                        withRecentAiConversations(
                            current,
                            AiConversationsState.Error(outcome),
                        )
                    }
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

    private fun withRecentAiConversations(
        current: HomeUiState,
        new: AiConversationsState,
    ): HomeUiState = when (current) {
        is HomeUiState.Loading -> current.copy(recentAiConversations = new)
        is HomeUiState.Ready -> current.copy(recentAiConversations = new)
        is HomeUiState.Error -> current.copy(recentAiConversations = new)
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
        val requestId = ++turnosRequestId
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
                    if (requestId != turnosRequestId) return@launch
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
                is TurnosOutcome.Failure -> {
                    if (requestId != turnosRequestId) return@launch
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
