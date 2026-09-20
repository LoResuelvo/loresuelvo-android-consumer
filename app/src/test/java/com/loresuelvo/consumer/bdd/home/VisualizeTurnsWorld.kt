package com.loresuelvo.consumer.bdd.home

import com.loresuelvo.consumer.ui.screens.turnos.TurnosUiState
import com.loresuelvo.consumer.ui.screens.turnos.TurnosViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * Per-scenario world for the "Mis Turnos" BDD specs
 * (visualize-turns.feature).
 *
 * Landed minimally for scenario 01-VT: it builds the
 * [TurnosViewModel] (which today only holds a Loading state —
 * no fetch yet) and observes its emissions so step defs can
 * assert `world.lastUiState()` after the scenario's "veo la
 * pantalla" step.
 *
 * The full world (turno seeding, failure injection, contact
 * capture) lands with scenarios 02-VT..14-VT — see the
 * `visualize-turns.feature` header for the per-scenario
 * cadence.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class VisualizeTurnsWorld : AutoCloseable {

    private val scheduler: TestCoroutineScheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(dispatcher + supervisorJob)

    private lateinit var viewModel: TurnosViewModel

    private val observedUiStates: MutableList<TurnosUiState> = mutableListOf()

    private var started: Boolean = false

    fun startScenario() {
        if (started) return
        started = true

        Dispatchers.setMain(dispatcher)

        // 01-VT wires no repository yet — the VM emits Loading
        // directly. Scenarios 02-VT..14-VT inject a fake
        // `TurnosRepository` here.
        viewModel = TurnosViewModel()

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.uiState.collect { observedUiStates += it }
        }

        scheduler.advanceUntilIdle()
    }

    fun lastUiState(): TurnosUiState = observedUiStates.last()

    override fun close() {
        supervisorJob.cancel()
        Dispatchers.resetMain()
    }
}
