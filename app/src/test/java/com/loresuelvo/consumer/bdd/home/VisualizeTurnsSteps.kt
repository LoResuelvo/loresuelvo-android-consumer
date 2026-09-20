package com.loresuelvo.consumer.bdd.home

import com.loresuelvo.consumer.domain.turno.Turno
import com.loresuelvo.consumer.domain.turno.TurnoCounterpart
import com.loresuelvo.consumer.domain.turno.TurnoStatus
import com.loresuelvo.consumer.ui.screens.turnos.TurnosUiState
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

/**
 * Step defs for `features/home/visualize-turns.feature`. Each
 * step is intentionally thin: the heavy lifting lives in
 * [VisualizeTurnsWorld].
 *
 * Landed incrementally per scenario:
 *  - 01-VT → Background steps + navigation step.
 *  - 02-VT → `queTengoTurnosRegistrados` (seed non-empty) +
 *    `veoUnaListaConMisTurnos`.
 *  - 03-VT → `queNoTengoTurnosRegistrados` (seed empty) +
 *    `veoUnMensajeIndicandoQueNoTengoTurnos`.
 *
 * Subsequent scenarios (04-VT..14-VT) add their own step
 * patterns here as they land.
 */
class VisualizeTurnsSteps {

    private val world: VisualizeTurnsWorld = VisualizeTurnsWorld()

    @Given("que estoy autenticado como usuario")
    fun queEstoyAutenticadoComoUsuario() {
        world.startScenario()
    }

    @Given("y me encuentro en la pantalla Home")
    fun yMeEncuentroEnLaPantallaHome() {
        // Home routing is owned by the navigation graph; the BDD
        // asserts the VM state without composing the host.
    }

    @When("selecciono la opción {string}")
    fun seleccionoLaOpcion(option: String) {
        // 01-VT: the Home "Mis Turnos" link is wired by the
        // host's `onSeeAllTurnosClick` callback. No world
        // action is needed — the screen is mounted by the
        // navigation graph on click.
    }

    @Then("veo la pantalla {string}")
    fun veoLaPantalla(screen: String) {
        assertEquals("Mis Turnos", screen)
        val state = world.lastUiState()
        assertNotNull(state)
        assertEquals(TurnosUiState.Loading, state)
    }

    @Given("que tengo turnos registrados")
    fun queTengoTurnosRegistrados() {
        world.startScenario()
        world.seedTurnos(
            listOf(
                turno(
                    id = "1",
                    status = TurnoStatus.Confirmed,
                    counterpartName = "Juan",
                    counterpartSurname = "Gómez",
                    categoryName = "Plomería",
                    description = "Reparación de cañería",
                ),
            ),
        )
    }

    @Given("que no tengo turnos registrados")
    fun queNoTengoTurnosRegistrados() {
        world.startScenario()
        world.seedTurnos(emptyList())
    }

    @When("accedo a la pantalla {string}")
    fun accedoALaPantalla(screen: String) {
        assertEquals("Mis Turnos", screen)
    }

    @Then("veo una lista con mis turnos")
    fun veoUnaListaConMisTurnos() {
        val state = world.lastUiState()
        val ready = state as? TurnosUiState.Ready
            ?: error("expected Ready, was $state")
        assertTrue("expected at least one turno", ready.turnos.isNotEmpty())
    }

    @Then("veo un mensaje indicando que no tengo turnos")
    fun veoUnMensajeIndicandoQueNoTengoTurnos() {
        val state = world.lastUiState()
        val ready = state as? TurnosUiState.Ready
            ?: error("expected Ready with empty list, was $state")
        assertTrue("expected empty turnos", ready.turnos.isEmpty())
    }
}

/**
 * Builder used by the step defs to construct turnos without
 * duplicating boilerplate. Landed minimally for 02-VT; richer
 * fields land alongside the scenarios that surface them.
 */
internal fun turno(
    id: String,
    status: TurnoStatus = TurnoStatus.Confirmed,
    counterpartName: String = "Juan",
    counterpartSurname: String = "Gómez",
    categoryName: String = "Plomería",
    description: String = "Reparación de cañería",
    amountCents: Long = 150_005_0L,
    scheduledOnEpochMillis: Long = 1_783_540_200_000L,
): Turno = Turno(
    id = id,
    serviceProposalId = "p-$id",
    status = status,
    counterpart = TurnoCounterpart(
        id = "$id-c",
        name = counterpartName,
        surname = counterpartSurname,
        categoryName = categoryName,
        profilePhotoUrl = null,
    ),
    description = description,
    amountCents = amountCents,
    scheduledOnEpochMillis = scheduledOnEpochMillis,
)
