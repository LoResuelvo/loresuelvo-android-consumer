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

class VisualizeServiceProposalSteps {

    private val world: VisualizeServiceProposalWorld = VisualizeServiceProposalWorld()


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


    @And("que entre las propuestas recibidas hay aceptadas")
    fun queEntreLasPropuestasRecibidasHayAceptadas() {
        // The seed in [VisualizeServiceProposalWorld.seedProposalsReceived]
        // already includes an `Accepted` entry; this step exists
    }

    @Then("debe visualizar los trabajos próximos destacados")
    fun debeVisualizarLosTrabajosProximosDestacados() {
        val state = world.lastUiState()
        assertTrue(
            "expected HomeUiState.Ready, was $state",
            state is HomeUiState.Ready,
        )
        val upcoming = (state as HomeUiState.Ready).upcomingServiceProposals
        assertTrue(
            "expected ServiceProposalsState.Ready for upcoming, was $upcoming",
            upcoming is ServiceProposalsState.Ready,
        )
        val items = (upcoming as ServiceProposalsState.Ready).items
        assertEquals(
            "expected the accepted filter to keep exactly the Accepted proposals",
            listOf("2"),
            items.map { it.id },
        )
        // Pin the filter actually ran: every surviving item is
        // `Accepted`, and the `Pending` / `Rejected` entries were
        // dropped at the use-case boundary.
        assertTrue(
            "every visible upcoming proposal must be Accepted, was ${items.map { it.status }}",
            items.all { it.status == ServiceProposalStatus.Accepted },
        )
    }
}
