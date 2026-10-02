package com.loresuelvo.consumer.bdd.home

import com.loresuelvo.consumer.domain.turno.Turno
import com.loresuelvo.consumer.domain.turno.TurnoCounterpart
import com.loresuelvo.consumer.domain.turno.TurnoStatus
import com.loresuelvo.consumer.domain.turno.TurnosOutcome
import com.loresuelvo.consumer.domain.turno.TurnosRepository
import com.loresuelvo.consumer.domain.auth.CalendarConnectionStatus
import com.loresuelvo.consumer.domain.auth.CurrentUserOutcome
import com.loresuelvo.consumer.domain.auth.User
import com.loresuelvo.consumer.domain.usecase.auth.GetConsumerProfileUseCase
import com.loresuelvo.consumer.domain.usecase.turno.GetTurnosUseCase
import com.loresuelvo.consumer.ui.screens.turnos.TurnosUiState
import com.loresuelvo.consumer.ui.screens.turnos.TurnosViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import io.mockk.coEvery
import io.mockk.mockk

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class VisualizeTurnsWorld : AutoCloseable {

    private val scheduler: TestCoroutineScheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(dispatcher + supervisorJob)

    private lateinit var repository: FakeTurnosRepository
    private lateinit var viewModel: TurnosViewModel
    private val getConsumerProfile = mockk<GetConsumerProfileUseCase>()

    private var started: Boolean = false

    fun startScenario() {
        if (started) return
        started = true

        Dispatchers.setMain(dispatcher)

        repository = FakeTurnosRepository(items = emptyList())
        coEvery { getConsumerProfile() } returns CurrentUserOutcome.Success(
            User(
                displayName = "Ana Perez",
                firstName = "Ana",
                lastName = "Perez",
                calendarConnectionStatus = CalendarConnectionStatus.UNKNOWN,
            ),
        )
        viewModel = TurnosViewModel(
            getTurnos = GetTurnosUseCase(repository),
            getConsumerProfile = getConsumerProfile,
        )
    }

    fun seedTurnos(items: List<Turno>) {
        repository.set(items)
        viewModel.load()
        scheduler.advanceUntilIdle()
    }

    fun seedTurnosFailure(failure: TurnosOutcome.Failure) {
        repository.setFailure(failure)
        viewModel.load()
        scheduler.advanceUntilIdle()
    }

    fun lastUiState(): TurnosUiState = viewModel.uiState.value

    override fun close() {
        supervisorJob.cancel()
        Dispatchers.resetMain()
    }

    private class FakeTurnosRepository(
        items: List<Turno>,
    ) : TurnosRepository {
        private var currentSuccess: List<Turno> = items
        private var currentFailure: TurnosOutcome.Failure? = null

        override suspend fun getTurnos(): TurnosOutcome =
            currentFailure ?: TurnosOutcome.Success(currentSuccess)

        fun set(items: List<Turno>) {
            currentSuccess = items
            currentFailure = null
        }

        fun setFailure(failure: TurnosOutcome.Failure) {
            currentFailure = failure
        }
    }
}

internal fun turno(
    id: String,
    status: TurnoStatus = TurnoStatus.Confirmed,
    counterpartName: String = "Juan",
    counterpartSurname: String = "Gómez",
    categoryName: String = "Plomería",
    description: String = "Reparación de cañería",
    amountCents: Long = 150_005_0L,
    scheduledOnEpochMillis: Long = 1_783_540_200_000L,
    counterpartProfilePhotoUrl: String? = null,
): Turno = Turno(
    id = id,
    serviceProposalId = "p-$id",
    status = status,
    counterpart = com.loresuelvo.consumer.domain.turno.TurnoCounterpart(
        id = "$id-c",
        name = counterpartName,
        surname = counterpartSurname,
        categoryName = categoryName,
        profilePhotoUrl = counterpartProfilePhotoUrl,
    ),
    description = description,
    amountCents = amountCents,
    scheduledOnEpochMillis = scheduledOnEpochMillis,
)
