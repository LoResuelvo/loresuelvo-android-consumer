package com.loresuelvo.consumer.bdd.home

import com.loresuelvo.consumer.domain.auth.CalendarConnectionStatus
import com.loresuelvo.consumer.domain.auth.CurrentUserOutcome
import com.loresuelvo.consumer.domain.auth.User
import com.loresuelvo.consumer.domain.turno.Turno
import com.loresuelvo.consumer.domain.turno.TurnosOutcome
import com.loresuelvo.consumer.domain.turno.TurnosRepository
import com.loresuelvo.consumer.domain.usecase.auth.GetConsumerProfileUseCase
import com.loresuelvo.consumer.domain.usecase.turno.GetTurnosUseCase
import com.loresuelvo.consumer.ui.screens.turnos.TurnosUiState
import com.loresuelvo.consumer.ui.screens.turnos.TurnosViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarSyncWorld : AutoCloseable {
    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val turnosRepository = mockk<TurnosRepository>()
    private val getConsumerProfile = mockk<GetConsumerProfileUseCase>()
    private lateinit var viewModel: TurnosViewModel
    private var navigatedToProfile = false

    fun start(status: CalendarConnectionStatus) {
        Dispatchers.setMain(dispatcher)
        coEvery { turnosRepository.getTurnos() } returns TurnosOutcome.Success(listOf(turno("calendar-1")))
        coEvery { getConsumerProfile() } returns CurrentUserOutcome.Success(
            User("Ana Perez", "Ana", "Perez", calendarConnectionStatus = status),
        )
        viewModel = TurnosViewModel(
            getTurnos = GetTurnosUseCase(turnosRepository),
            getConsumerProfile = getConsumerProfile,
        )
        scheduler.advanceUntilIdle()
    }

    fun state(): TurnosUiState = viewModel.uiState.value

    fun chooseConnectionAction() {
        navigatedToProfile = true
    }

    fun navigatedToProfile(): Boolean = navigatedToProfile

    override fun close() {
        Dispatchers.resetMain()
    }
}
