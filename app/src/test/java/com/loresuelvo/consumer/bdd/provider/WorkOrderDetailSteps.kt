package com.loresuelvo.consumer.bdd.provider

import com.loresuelvo.consumer.domain.turno.TurnoStatus
import com.loresuelvo.consumer.ui.screens.workorderdetail.WorkOrderDetailUiState
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class WorkOrderDetailSteps {

    private val world: WorkOrderDetailWorld = WorkOrderDetailWorld()


    @Given("que existe una orden de trabajo con un tiempo estimado para realizar el servicio")
    fun queExisteUnaOrdenDeTrabajoConUnTiempoEstimadoParaRealizarElServicio() {
        world.startScenario()
        world.seedAcceptedProposalWithNinetyMinutesEstimate()
    }

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
        assertEquals("wo-100", workOrder.proposalId)
        assertEquals("Cambio de termotanque", workOrder.description)
        assertEquals(8_500_000L, workOrder.amountCents)
        assertEquals("Gas", workOrder.provider.categoryName)
        assertEquals("Andrés Quiroga", "${workOrder.provider.name} ${workOrder.provider.surname}")
        assertEquals(1_793_500_800_000L, workOrder.scheduledOnEpochMillis)
    }
}
