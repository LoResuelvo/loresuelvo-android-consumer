package com.loresuelvo.consumer.bdd.provider

import com.loresuelvo.consumer.ui.screens.workorder.WorkOrderUiState
import com.loresuelvo.consumer.ui.util.EstimatedDurationFormatter
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

/**
 * Real step implementations for the US-54 BDD spec
 * `16-VSP Consultar el tiempo estimado de trabajo en la orden`.
 * The [WorkOrderWorld] drives the
 * [com.loresuelvo.consumer.ui.screens.workorder.WorkOrderViewModel]
 * with a fake repo whose only proposal is the seeded accepted
 * proposal with a 90-minute estimate, so the work-order detail
 * renders the pinned `1 h 30 min` formatter output alongside
 * every agreed-terms field.
 */
class WorkOrderSteps {

    private val world: WorkOrderWorld = WorkOrderWorld()

    // ---- Scenario 16-VSP --------------------------------------

    /**
     * "que existe una orden de trabajo con un tiempo estimado
     * para realizar el servicio" — scenario 16-VSP. The seed
     * carries a single accepted proposal with
     * `estimatedDurationMinutes = 90` so the formatter renders
     * `"1 h 30 min"`.
     */
    @Given("que existe una orden de trabajo con un tiempo estimado para realizar el servicio")
    fun queExisteUnaOrdenDeTrabajoConUnTiempoEstimadoParaRealizarElServicio() {
        world.startScenario()
        world.seedAcceptedProposalWithNinetyMinutesEstimate()
    }

    /**
     * "el usuario consulta el detalle de la orden de trabajo" —
     * scenario 16-VSP. Drives the VM's `load(proposalId)` so the
     * `Then` assertion observes the resolved state.
     */
    @When("el usuario consulta el detalle de la orden de trabajo")
    fun elUsuarioConsultaElDetalleDeLaOrdenDeTrabajo() {
        world.openWorkOrder()
    }

    @Then("debe visualizar el tiempo estimado de trabajo junto con los datos acordados del servicio")
    fun debeVisualizarElTiempoEstimadoDeTrabajoJuntoConLosDatosAcordadosDelServicio() {
        val state = world.lastWorkOrderState()
        assertTrue(
            "expected WorkOrderUiState.Ready, was $state",
            state is WorkOrderUiState.Ready,
        )
        val workOrder = (state as WorkOrderUiState.Ready).workOrder

        // Pinned formatter output for 90 minutes.
        val minutes = workOrder.estimatedDurationMinutes
        assertTrue(
            "expected the work order to carry an estimatedDurationMinutes, was $minutes",
            minutes != null,
        )
        assertEquals(
            "expected the EstimatedDurationFormatter to render the " +
                "estimated work time as the scenario pins",
            "1 h 30 min",
            EstimatedDurationFormatter.formatDuration(minutes!!),
        )

        // "junto con los datos acordados del servicio" — the work
        // order also carries the agreed amount, scheduled date,
        // description and status, so the BDD pins they survive
        // the lookup.
        assertEquals("wo-100", workOrder.proposalId)
        assertEquals("Cambio de termotanque", workOrder.description)
        assertEquals(8_500_000L, workOrder.amountCents)
        assertEquals("Gas", workOrder.categoryName)
        assertEquals("Andrés Quiroga", workOrder.providerName)
        assertEquals(1_793_500_800_000L, workOrder.scheduledOnEpochMillis)
    }
}