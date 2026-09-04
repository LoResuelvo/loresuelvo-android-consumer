package com.loresuelvo.consumer.bdd.provider

import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus
import com.loresuelvo.consumer.ui.screens.misservicios.MisServiciosUiState
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

/**
 * Real step implementations for the US-54 "Mis Servicios" BDD
 * specs in `features/provider/visualize-service-proposal.feature`.
 *
 * Today only scenario 03-VSP is green ("ver todas las propuestas
 * en Mis Servicios"). Each subsequent scenario — order
 * chronologically (04-VSP), filter by status (05-VSP / 06-VSP /
 * 07-VSP) — will gain its step defs here as it lands.
 *
 * The Background steps ("sesión iniciada", "propuestas recibidas")
 * live on [VisualizeServiceProposalSteps] and seed the Home world.
 * That world is the one driving the Home dashboard scenarios
 * (01-VSP / 02-VSP); MisServicios has its own world seeded from
 * the `When` step below so we avoid cross-world coupling and
 * duplicate step definitions against the Background glue.
 *
 * At the JVM BDD layer "el usuario accede a Mis Servicios" maps
 * to "the MisServiciosViewModel mounts against the seeded repo";
 * the navigation graph itself is exercised by the
 * `MisServiciosScreenInstrumentedTest` on a real device.
 */
class MisServiciosSteps {

    private val world: MisServiciosWorld = MisServiciosWorld()

    // ---- Scenario 03-VSP --------------------------------------

    @When("accede a Mis Servicios")
    fun accedeAMisServicios() {
        // The Background already started the Home world; this
        // step mounts the MisServiciosViewModel against the
        // seeded repo so the Then assertion observes its state.
        // The seed mirrors the Home world's seed (mixed statuses)
        // because scenario 03-VSP asserts every status is present.
        world.startScenario()
        world.seedProposalsReceived()
        world.openMisServicios()
    }

    @Then("debe visualizar todas sus propuestas de servicio")
    fun debeVisualizarTodasSusPropuestasDeServicio() {
        val state = world.lastUiState()
        assertTrue(
            "expected MisServiciosUiState.Ready, was $state",
            state is MisServiciosUiState.Ready,
        )
        val items = (state as MisServiciosUiState.Ready).proposals
        assertEquals(
            "expected the GetAll use case to surface every seeded proposal " +
                "without filtering by status",
            listOf("10", "11", "12"),
            items.map { it.id },
        )
        // Pin the explicit breadth: every status is present in
        // the rendered list (the screen-level filter chips on
        // top of this list are scenarios 05-VSP / 06-VSP / 07-VSP).
        val statuses = items.map { it.status }.toSet()
        assertEquals(
            "expected the seeded mixed-status set, was $statuses",
            setOf(
                ServiceProposalStatus.Pending,
                ServiceProposalStatus.Accepted,
                ServiceProposalStatus.Rejected,
            ),
            statuses,
        )
    }
}
