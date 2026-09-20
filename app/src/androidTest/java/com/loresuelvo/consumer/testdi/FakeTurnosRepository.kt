package com.loresuelvo.consumer.testdi

import com.loresuelvo.consumer.domain.turno.Turno
import com.loresuelvo.consumer.domain.turno.TurnosOutcome
import com.loresuelvo.consumer.domain.turno.TurnosRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Test-only [TurnosRepository] for the acceptance / integration
 * tests that `@UninstallModules(RepositoryModule::class)`.
 *
 * The production binding lives in
 * [com.loresuelvo.consumer.di.RepositoryModule]; once a test
 * uninstalls that module it must rebind every port the ViewModels
 * under test transitively depend on — including
 * [TurnosRepository], which `HomeViewModel` pulls in via
 * [com.loresuelvo.consumer.domain.usecase.turno.GetTurnosUseCase]
 * (and `TurnosViewModel` does directly).
 *
 * Default seed: empty list. Tests that need a populated list
 * call [set] from the test thread before `scenario.recreate()` so
 * the new seed lands in the same `@Singleton` instance the
 * activity resolves through `hiltViewModel()`. The setter MUST
 * run on the main thread (via `composeTestRule.runOnUiThread { ... }`)
 * so the Hilt graph sees the change before the activity's VM is
 * constructed.
 */
@Singleton
class FakeTurnosRepository @Inject constructor() : TurnosRepository {
    private var seed: List<Turno> = emptyList()

    fun set(turnos: List<Turno>) {
        seed = turnos
    }

    override suspend fun getTurnos(): TurnosOutcome =
        TurnosOutcome.Success(seed)
}
