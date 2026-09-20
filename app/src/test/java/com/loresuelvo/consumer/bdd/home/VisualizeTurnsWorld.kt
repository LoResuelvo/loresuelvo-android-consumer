package com.loresuelvo.consumer.bdd.home

import com.loresuelvo.consumer.domain.turno.Turno
import com.loresuelvo.consumer.domain.turno.TurnosOutcome
import com.loresuelvo.consumer.domain.turno.TurnosRepository
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

/**
 * Per-scenario world for the "Mis Turnos" BDD specs
 * (visualize-turns.feature).
 *
 * Landed minimally for scenario 02-VT: it builds the
 * [TurnosViewModel] against an in-memory [TurnosRepository] the
 * scenario can seed via [seedTurnos] and exposes the resulting
 * [TurnosUiState] for assertions.
 *
 * The full world (failure injection, contact capture, detail
 * navigation) lands with subsequent scenarios.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class VisualizeTurnsWorld : AutoCloseable {

    private val scheduler: TestCoroutineScheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(dispatcher + supervisorJob)

    private lateinit var repository: FakeTurnosRepository
    private lateinit var viewModel: TurnosViewModel

    private var started: Boolean = false

    fun startScenario() {
        if (started) return
        started = true

        Dispatchers.setMain(dispatcher)

        repository = FakeTurnosRepository(items = emptyList())
        viewModel = TurnosViewModel(getTurnos = GetTurnosUseCase(repository))
    }

    /**
     * Replaces the seeded list and re-fires the VM's `load()`
     * so the world observes the new `Ready(items)` state.
     */
    fun seedTurnos(items: List<Turno>) {
        repository.set(items)
        viewModel.load()
        scheduler.advanceUntilIdle()
    }

    fun lastUiState(): TurnosUiState = viewModel.uiState.value

    override fun close() {
        supervisorJob.cancel()
        Dispatchers.resetMain()
    }

    /**
     * In-memory [TurnosRepository] that returns whatever the
     * step def seeded.
     */
    private class FakeTurnosRepository(
        items: List<Turno>,
    ) : TurnosRepository {
        private var current: List<Turno> = items

        override suspend fun getTurnos(): TurnosOutcome =
            TurnosOutcome.Success(current)

        fun set(items: List<Turno>) {
            current = items
        }
    }
}
