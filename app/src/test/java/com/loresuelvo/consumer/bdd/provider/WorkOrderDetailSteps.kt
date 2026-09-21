package com.loresuelvo.consumer.bdd.provider

import com.loresuelvo.consumer.domain.turno.TurnoStatus
import com.loresuelvo.consumer.ui.screens.workorderdetail.WorkOrderDetailUiState
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

/**
 * Real step implementations for the US-54 BDD spec
 * `16-VSP Consultar el detalle de la orden de trabajo`. The
 * [WorkOrderDetailWorld] drives the
 * [com.loresuelvo.consumer.ui.screens.workorderdetail.WorkOrderDetailViewModel]
 * with a fake repo whose only proposal is the seeded accepted
 * proposal, so the work-order detail renders every agreed-terms
 * field.
 *
 * US-27 (`visualize-turns-detail`) drops the original duration
 * assertion: the new dedicated `GET /work-orders/{workOrderID}`
 * endpoint does not carry `estimated_duration_minutes`, so the
 * formatter output can no longer be pinned to `"1 h 30 min"`.
 * The screen / formatter stay in the codebase for future use;
 * the scenario relaxes to "the agreed-terms fields render".
 */
class WorkOrderDetailSteps {

    private val world: WorkOrderDetailWorld = WorkOrderDetailWorld()

    // ---- Scenario 16-VSP --------------------------------------

    /**
     * "que existe una orden de trabajo" — scenario 16-VSP. The
     * seed carries a single accepted proposal whose agreed
     * terms are the scenario's pin.
     */
    @Given("que existe una orden de trabajo con un tiempo estimado para realizar el servicio")
    fun queExisteUnaOrdenDeTrabajoConUnTiempoEstimadoParaRealizarElServicio() {
        world.startScenario()
        world.seedAcceptedProposalWithNinetyMinutesEstimate()
    }

    /**
     * "el usuario consulta el detalle de la orden de trabajo" —
     * scenario 16-VSP. Drives the VM's `load(workOrderId)` so
     * the `Then` assertion observes the resolved state.
     */
    @When("el usuario consulta el detalle de la orden de trabajo")
    fun elUsuarioConsultaElDetalleDeLaOrdenDeTrabajo() {
        world.openWorkOrder()
    }

    @Then("debe visualizar los datos acordados del servicio")
    fun debeVisualizarLosDatosAcordadosDelServicio() {
        val state = world.lastWorkOrderState()
        assertTrue(
            "expected WorkOrderDetailUiState.Ready, was $state",
            state is WorkOrderDetailUiState.Ready,
        )
        val workOrder = (state as WorkOrderDetailUiState.Ready).workOrder

        // "datos acordados del servicio" — the work order carries
        // the agreed amount, scheduled date, description, status
        // and counterpart. The BDD pins they survive the lookup
        // (US-54 16-VSP).
        assertEquals("wo-100", workOrder.proposalId)
        assertEquals("Cambio de termotanque", workOrder.description)
        assertEquals(8_500_000L, workOrder.amountCents)
        assertEquals("Gas", workOrder.categoryName)
        assertEquals("Andrés Quiroga", workOrder.providerName)
        assertEquals(1_793_500_800_000L, workOrder.scheduledOnEpochMillis)
    }
}