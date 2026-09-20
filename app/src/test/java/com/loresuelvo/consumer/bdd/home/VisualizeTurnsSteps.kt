package com.loresuelvo.consumer.bdd.home

import com.loresuelvo.consumer.domain.turno.Turno
import com.loresuelvo.consumer.domain.turno.TurnoCounterpart
import com.loresuelvo.consumer.domain.turno.TurnoStatus
import com.loresuelvo.consumer.domain.turno.TurnosOutcome
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

    @Given("que tengo un turno registrado")
    fun queTengoUnTurnoRegistrado() {
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
                    amountCents = 1_500_000L,
                    scheduledOnEpochMillis = 1_792_074_600_000L,
                ),
            ),
        )
    }

    @Then("veo el nombre y apellido de la contraparte")
    fun veoElNombreYApellidoDeLaContraparte() {
        val state = world.lastUiState() as TurnosUiState.Ready
        val first = state.turnos.first()
        assertTrue(first.counterpart.name.isNotBlank())
        assertTrue(first.counterpart.surname.isNotBlank())
    }

    @Then("veo la foto de perfil de la contraparte")
    fun veoLaFotoDePerfilDeLaContraparte() {
        // The full UI assertion lives in
        // [com.loresuelvo.consumer.ui.components.turnocard.TurnoCardTest];
        // the BDD only pins that the seeded turno carries a
        // photo URL so the avatar branch can render it.
        val state = world.lastUiState() as TurnosUiState.Ready
        val first = state.turnos.first()
        assertNotNull(first.counterpart.profilePhotoUrl)
    }

    @Then("veo el motivo del servicio")
    fun veoElMotivoDelServicio() {
        val state = world.lastUiState() as TurnosUiState.Ready
        assertTrue(state.turnos.first().description.isNotBlank())
    }

    @Then("veo el monto del servicio")
    fun veoElMontoDelServicio() {
        val state = world.lastUiState() as TurnosUiState.Ready
        assertTrue(state.turnos.first().amountCents > 0)
    }

    @Then("veo la fecha del turno")
    fun veoLaFechaDelTurno() {
        val state = world.lastUiState() as TurnosUiState.Ready
        assertTrue(state.turnos.first().scheduledOnEpochMillis > 0)
    }

    @Then("veo la hora del turno")
    fun veoLaHoraDelTurno() {
        // The Turno domain type stores a single
        // `scheduledOnEpochMillis` that encodes date + time;
        // `ScheduledDateFormatter.formatScheduled` splits them
        // visually as "dd/MM/yyyy - HH:mm hs". The BDD asserts
        // the underlying timestamp is non-zero; the formatted
        // "fecha" / "hora" split is covered by the JVM
        // `TurnoCardTest` + `ScheduledDateFormatterTest`.
        veoLaFechaDelTurno()
    }

    @When("visualizo el turno")
    fun visualizoElTurno() {
        // The screen always renders the seeded turno; this step
        // is a marker for the Gherkin narrative.
    }

    @Then("veo el estado actual del turno")
    fun veoElEstadoActualDelTurno() {
        // The badge label rendering is pinned by
        // [com.loresuelvo.consumer.ui.components.turnocard.TurnoCardTest];
        // the BDD asserts the domain type carries a non-null
        // [TurnoStatus] so the badge can render it.
        val state = world.lastUiState() as TurnosUiState.Ready
        assertNotNull(state.turnos.first().status)
    }

    @Given("que tengo un turno con estado {string}")
    fun queTengoUnTurnoConEstado(statusLabel: String) {
        world.startScenario()
        val parsed = parseStatusLabel(statusLabel)
        world.seedTurnos(
            listOf(
                turno(
                    id = "1",
                    status = parsed,
                    counterpartName = "Juan",
                    counterpartSurname = "Gómez",
                    categoryName = "Plomería",
                    description = "Reparación de cañería",
                ),
            ),
        )
    }

    @Then("veo el estado {string}")
    fun veoElEstado(statusLabel: String) {
        val expected = parseStatusLabel(statusLabel)
        val state = world.lastUiState() as TurnosUiState.Ready
        assertEquals(expected, state.turnos.first().status)
    }

    private fun parseStatusLabel(label: String): TurnoStatus = when (label) {
        "Pendiente" -> TurnoStatus.Pending
        "Confirmado" -> TurnoStatus.Confirmed
        "Finalizado" -> TurnoStatus.Finished
        "Cancelado" -> TurnoStatus.Cancelled
        else -> error("unknown status label: $label")
    }

    @Given("que el backend no responde")
    fun queElBackendNoResponde() {
        world.startScenario()
        world.seedTurnosFailure(
            TurnosOutcome.Failure.Network(java.io.IOException("dns")),
        )
    }

    @Given("que el backend responde con error")
    fun queElBackendRespondeConError() {
        world.startScenario()
        world.seedTurnosFailure(
            TurnosOutcome.Failure.Server(code = 500, message = "boom"),
        )
    }

    @Then("veo un mensaje de error de conexión")
    fun veoUnMensajeDeErrorDeConexion() {
        val state = world.lastUiState()
        assertTrue(
            "expected Error with Failure.Network, was $state",
            state is TurnosUiState.Error &&
                state.failure is TurnosOutcome.Failure.Network,
        )
    }

    @Then("veo un mensaje de error del servidor")
    fun veoUnMensajeDeErrorDelServidor() {
        val state = world.lastUiState()
        assertTrue(
            "expected Error with Failure.Server, was $state",
            state is TurnosUiState.Error &&
                state.failure is TurnosOutcome.Failure.Server,
        )
    }

    @Then("veo un botón para reintentar")
    fun veoUnBotonParaReintentar() {
        assertTrue(world.lastUiState() is TurnosUiState.Error)
    }
}
