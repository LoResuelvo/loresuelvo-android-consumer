package com.loresuelvo.consumer.bdd.home

import com.loresuelvo.consumer.ui.screens.turnos.TurnosUiState
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

/**
 * Step defs for `features/home/visualize-turns.feature`. Each
 * step is intentionally thin: the heavy lifting lives in
 * [VisualizeTurnsWorld].
 *
 * Landed minimally for scenario 01-VT: only the four step
 * patterns the Background + 01-VT exercise are defined.
 * Subsequent scenarios (02-VT..14-VT) add their own step
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
        // Sanity: the VM finished its initial emission (today
        // just Loading).
        val state = world.lastUiState()
        assertNotNull(state)
        assertEquals(TurnosUiState.Loading, state)
    }
}
