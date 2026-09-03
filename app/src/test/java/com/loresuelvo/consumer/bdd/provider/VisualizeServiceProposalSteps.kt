package com.loresuelvo.consumer.bdd.provider

import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus
import com.loresuelvo.consumer.ui.screens.home.HomeUiState
import com.loresuelvo.consumer.ui.screens.home.ServiceProposalsState
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

/**
 * Real step implementations for the scenarios in
 * `features/provider/visualize-service-proposal.feature`.
 *
 * Today only scenario 01-VSP is green. Each subsequent scenario
 * (Mis Servicios, Detalle, Chat, Duración, Orden de trabajo)
 * will gain its step defs here as it lands.
 *
 * The per-scenario discipline (one scenario per commit, ≤ 400
 * lines) is documented at the top of the feature file. Comments
 * at each step flag whether the assertion is at the state level
 * (this file) or the visual / integration level (covered
 * separately by the Compose test suite).
 */
class VisualizeServiceProposalSteps {

    private val world: VisualizeServiceProposalWorld = VisualizeServiceProposalWorld()

    // ---- Scenario 01-VSP --------------------------------------

    /**
     * The Background step "que el usuario tiene una sesión
     * iniciada" is implicit in the BDD: every Android scenario
     * assumes a valid session because the smart router in
     * `LoResuelvoNav` redirects unauthenticated users to
     * `Welcome` (see `authentication-session.feature`). Starting
     * the scenario is therefore a no-op for the session surface.
     */
    @Given("que el usuario tiene una sesión iniciada")
    fun queElUsuarioTieneUnaSesionIniciada() {
        world.startScenario()
    }

    @And("que el usuario tiene propuestas de servicio recibidas")
    fun queElUsuarioTienePropuestasDeServicioRecibidas() {
        // Seed the mixed-status list (Pending / Accepted / Rejected)
        // so the `Pending` filter inside `GetPendingServiceProposalsUseCase`
        // has something to keep AND something to drop.
        world.seedProposalsReceived()
    }

    @And("que entre las propuestas recibidas hay pendientes")
    fun queEntreLasPropuestasRecibidasHayPendientes() {
        // The seed already includes pending entries; this step
        // exists so the Gherkin flow reads naturally.
    }

    @When("accede al inicio")
    fun accedeAlInicio() {
        world.openHome()
    }

    @Then("debe visualizar dichas propuestas destacadas")
    fun debeVisualizarDichasPropuestasDestacadas() {
        val state = world.lastUiState()
        assertTrue(
            "expected HomeUiState.Ready, was $state",
            state is HomeUiState.Ready,
        )
        val pending = (state as HomeUiState.Ready).pendingServiceProposals
        assertTrue(
            "expected ServiceProposalsState.Ready, was $pending",
            pending is ServiceProposalsState.Ready,
        )
        val items = (pending as ServiceProposalsState.Ready).items
        assertEquals(
            "expected the pending filter to keep exactly the Pending proposals",
            listOf("1"),
            items.map { it.id },
        )
        // Pin the filter actually ran: every surviving item is
        // `Pending`, and the `Accepted` / `Rejected` entries were
        // dropped at the use-case boundary.
        assertTrue(
            "every visible proposal must be Pending, was ${items.map { it.status }}",
            items.all { it.status == ServiceProposalStatus.Pending },
        )
    }
}
