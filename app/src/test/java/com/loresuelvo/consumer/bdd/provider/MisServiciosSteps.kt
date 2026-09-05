package com.loresuelvo.consumer.bdd.provider

import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus
import com.loresuelvo.consumer.ui.screens.misservicios.MisServiciosUiState
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
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

    // ---- Scenario 04-VSP --------------------------------------

    @Given("que el usuario tiene varias propuestas de servicio con fechas distintas")
    fun queElUsuarioTieneVariasPropuestasDeServicioConFechasDistintas() {
        // Seed three proposals whose `createdOnEpochMillis` is
        // intentionally NOT in insertion order so the use case
        // sort is observable in the resulting `Ready.items`.
        // The "When" step that follows reuses the same
        // `accedeAMisServicios` as 03-VSP, which re-mounts the
        // VM against the latest seed.
        world.startScenario()
        world.seedProposalsWithDistinctDates()
        world.openMisServicios()
    }

    @Then("debe visualizar primero la propuesta más reciente")
    fun debeVisualizarPrimeroLaPropuestaMasReciente() {
        val state = world.lastUiState()
        assertTrue(
            "expected MisServiciosUiState.Ready, was $state",
            state is MisServiciosUiState.Ready,
        )
        val items = (state as MisServiciosUiState.Ready).proposals
        assertEquals(
            "expected the GetAll use case to surface the newest " +
                "proposal first (sorted by createdOnEpochMillis " +
                "descending); the seed inserted id=\"30\" first but " +
                "it carries the largest createdOnEpochMillis",
            listOf("30", "31", "32"),
            items.map { it.id },
        )
    }

    // ---- Scenario 05-VSP --------------------------------------

    @Given("que el usuario tiene propuestas en diferentes estados")
    fun queElUsuarioTienePropuestasEnDiferentesEstados() {
        // The Background step (`seedProposalsReceived`) already
        // wires a mixed-status seed (1 Pending, 1 Accepted, 1
        // Rejected) into the Home world. Re-mount the MisServicios
        // VM against that seed so this Given reads as "the
        // consumer has proposals of every status available".
        world.startScenario()
        world.seedProposalsReceived()
        world.openMisServicios()
    }

    @When("selecciona el filtro de propuestas que requieren su atención")
    fun seleccionaElFiltroDePropuestasQueRequierenSuAtencion() {
        // The "requieren atención" wording maps to the Pending
        // status filter (US-54 scenario 05-VSP). The VM routes
        // through `GetPendingServiceProposalsUseCase`.
        world.selectFilter(ServiceProposalStatus.Pending)
    }

    @Then("debe visualizar únicamente las propuestas pendientes")
    fun debeVisualizarUnicamenteLasPropuestasPendientes() {
        val state = world.lastUiState()
        assertTrue(
            "expected MisServiciosUiState.Ready, was $state",
            state is MisServiciosUiState.Ready,
        )
        val ready = state as MisServiciosUiState.Ready
        assertEquals(
            "expected the filter chip to land on Pending after the user tapped it",
            ServiceProposalStatus.Pending,
            ready.selectedStatusFilter,
        )
        assertEquals(
            "expected the GetPending use case to keep only the Pending entries " +
                "from the mixed seed (id=\"10\")",
            listOf("10"),
            ready.proposals.map { it.id },
        )
        assertTrue(
            "every visible proposal must be Pending, was ${ready.proposals.map { it.status }}",
            ready.proposals.all { it.status == ServiceProposalStatus.Pending },
        )
    }
}
