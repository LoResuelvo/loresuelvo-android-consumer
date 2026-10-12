package com.loresuelvo.consumer.ui.screens.turnos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loresuelvo.consumer.domain.auth.CalendarConnectionStatus
import com.loresuelvo.consumer.domain.auth.CurrentUserOutcome
import com.loresuelvo.consumer.domain.turno.TurnosOutcome
import com.loresuelvo.consumer.domain.turno.Turno
import com.loresuelvo.consumer.domain.turno.TurnoStatus
import com.loresuelvo.consumer.domain.usecase.auth.GetConsumerProfileUseCase
import com.loresuelvo.consumer.domain.usecase.turno.GetTurnosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class TurnosViewModel @Inject constructor(
    private val getTurnos: GetTurnosUseCase,
    private val getConsumerProfile: GetConsumerProfileUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<TurnosUiState>(TurnosUiState.Loading)
    val uiState: StateFlow<TurnosUiState> = _uiState.asStateFlow()

    private var selectedStatusFilter: TurnoStatus? = null
    private var searchQuery: String = ""
    private var loadedTurnos: List<Turno> = emptyList()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { TurnosUiState.Loading }
            _uiState.update {
                when (val outcome = getTurnos()) {
                    is TurnosOutcome.Success -> {
                        loadedTurnos = outcome.turnos
                        val profileOutcome = try {
                            getConsumerProfile()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Throwable) {
                            null
                        }
                        val calendarStatus = profileOutcome.calendarConnectionStatusOrUnknown()
                        TurnosUiState.Ready(
                            turnos = filteredTurnos(),
                            selectedStatusFilter = selectedStatusFilter,
                            searchQuery = searchQuery,
                            calendarConnectionStatus = calendarStatus,
                        )
                    }
                    is TurnosOutcome.Failure ->
                        TurnosUiState.Error(
                            failure = outcome,
                            selectedStatusFilter = selectedStatusFilter,
                            searchQuery = searchQuery,
                        )
                }
            }
        }
    }

    fun onFilterSelected(filter: TurnoStatus?) {
        selectedStatusFilter = filter
        updateVisibleTurnos()
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery = query
        updateVisibleTurnos()
    }

    private fun updateVisibleTurnos() {
        _uiState.update { current ->
            when (current) {
                is TurnosUiState.Ready -> current.copy(
                    turnos = filteredTurnos(),
                    selectedStatusFilter = selectedStatusFilter,
                    searchQuery = searchQuery,
                )
                is TurnosUiState.Error -> current.copy(searchQuery = searchQuery)
                TurnosUiState.Loading -> current
            }
        }
    }

    private fun filteredTurnos(): List<Turno> = loadedTurnos.filter { turno ->
        val matchesStatus = selectedStatusFilter == null || turno.status == selectedStatusFilter
        val normalizedQuery = searchQuery.trim()
        val matchesQuery = normalizedQuery.isBlank() || listOf(
            turno.counterpart.name,
            turno.counterpart.surname,
            turno.counterpart.categoryName,
            turno.description,
        ).any { value -> value.contains(normalizedQuery, ignoreCase = true) }
        matchesStatus && matchesQuery
    }
}

private fun CurrentUserOutcome?.calendarConnectionStatusOrUnknown(): CalendarConnectionStatus =
    (this as? CurrentUserOutcome.Success)?.user?.calendarConnectionStatus
        ?: CalendarConnectionStatus.UNKNOWN
