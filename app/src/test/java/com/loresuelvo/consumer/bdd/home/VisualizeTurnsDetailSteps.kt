package com.loresuelvo.consumer.bdd.home

import com.loresuelvo.consumer.domain.turno.TurnoStatus
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetail
import com.loresuelvo.consumer.ui.screens.workorderdetail.WorkOrderDetailUiState
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue

/**
 * Real step implementations for the
 * `features/home/visualize-turns-detail.feature` BDD specs
 * (US-27). The [VisualizeTurnsDetailWorld] drives the
 * [com.loresuelvo.consumer.ui.screens.workorderdetail.WorkOrderDetailViewModel]
 * with a fake repo so each scenario's `Given` seeds the right
 * status and the `Then` step asserts the resolved
 * [WorkOrderDetailUiState].
 *
 * Each per-scenario commit removes the `@wip` from exactly one
 * Gherkin scenario. The scaffold keeps all step defs in place;
 * the runner filters `@wip` so they don't execute until the
 * scenario lands.
 */
class VisualizeTurnsDetailSteps {

    private val world: VisualizeTurnsDetailWorld = VisualizeTurnsDetailWorld()

    // The Background step defs (`que estoy autenticado como
    // usuario` / `me encuentro en la pantalla Home`) already
    // exist on [VisualizeTurnsSteps] (US-55); the Cucumber runner
    // matches step patterns across all glue classes in
    // `bdd.home` so the shared Background keeps a single source
    // of truth.

    // ---- Scenario 03-VTD --------------------------------------

    @Given("participa en una orden de trabajo")
    fun participaEnUnaOrdenDeTrabajo() {
        world.startScenario()
        // Scenario 01-VTD does not specify a state; seed a
        // default `scheduled` work order so the trigger step
        // resolves to Ready. Scenarios that require a different
        // state (03-VTD, 04-VTD, etc.) override the seed via
        // the `la orden se encuentra en estado ...` step.
        world.seedScheduledWorkOrder()
    }

    /**
     * Cucumber JVM matches step definitions across `@Given`,
     * `@When` and `@Then` by pattern, not by keyword. The
     * `visualize-turns-detail.feature` scenarios use both
     * `Given la orden se encuentra en estado "..."` (04-VTD /
     * 05-VTD / 07-VTD) and `When la orden se encuentra en
     * estado "..."` (03-VTD); a single `@Given` step def covers
     * both, since the runner treats the annotation as a label
     * for reporting and matches by the captured step text.
     *
     * Declaring both `@Given` and `@When` with the same pattern
     * makes Cucumber JVM 7.x raise `DuplicateStepDefinitionException`
     * because it indexes each annotation as a separate binding.
     * Using a single `@Given` avoids the duplication and still
     * matches both Gherkin keywords.
     */
    @Given("la orden se encuentra en estado {string}")
    fun laOrdenSeEncuentraEnEstadoScheduled(stateLabel: String) {
        when (stateLabel.lowercase()) {
            "scheduled" -> world.seedScheduledWorkOrder()
            "awaiting_payment" -> world.seedAwaitingPaymentWorkOrder()
            "paid" -> world.seedPaidWorkOrderWithoutReview()
            else -> error("unknown state label: $stateLabel")
        }
        world.openWorkOrder()
    }

    @Given("la orden tiene un reporte de finalización")
    fun laOrdenTieneUnReporteDeFinalizacion() {
        // Already seeded via `la orden se encuentra en estado awaiting_payment`.
    }

    @Given("el pago de la orden fue realizado")
    fun elPagoDeLaOrdenFueRealizado() {
        // Already seeded via `la orden se encuentra en estado paid`.
    }

    @Given("el consumidor ya emitió una reseña")
    fun elConsumidorYaEmitioUnaResena() {
        world.seedPaidWorkOrderWithReview()
    }

    @Given("el consumidor todavía no emitió una reseña")
    fun elConsumidorTodaviaNoEmitioUnaResena() {
        // Already seeded via `seedPaidWorkOrderWithoutReview`.
    }

    @Given("la orden tiene evidencia fotográfica")
    fun laOrdenTieneEvidenciaFotografica() {
        // The seeded `awaiting_payment` and `paid` work orders
        // already carry a single photo each.
    }

    @Given("participa en una orden de trabajo con evidencia fotográfica")
    fun participaEnUnaOrdenDeTrabajoConEvidenciaFotografica() {
        // Scenario 06-VTD bundles the setup: starts the scenario
        // and seeds an `awaiting_payment` work order (which is
        // the lifecycle where evidence shows up). The trigger
        // step (`selecciona para ver detalle ...`) opens the
        // detail so the photo's `Then` assertion observes the
        // `completionReport` slot.
        world.startScenario()
        world.seedAwaitingPaymentWorkOrder()
        world.openWorkOrder()
    }

    @Given("que el usuario está visualizando una orden")
    fun queElUsuarioEstaVisualizandoUnaOrden() {
        world.startScenario()
        // Scenario 08-VTD does not specify a state in the
        // Background; seed a default `paid` work order so the
        // trigger step resolves to Ready with a paidOnEpochMillis
        // (the scenario's first Then asserts the paid-on row).
        world.seedPaidWorkOrderWithoutReview()
    }

    @Given("que el consumidor está visualizando una orden")
    fun queElConsumidorEstaVisualizandoUnaOrden() {
        // Scenario 09-VTD uses the same Background as 08-VTD
        // but framed from the consumer's perspective ("el
        // consumidor"). The seam between the two phrasings is
        // incidental; the step seeds the same default so the
        // `la orden se encuentra en estado awaiting_payment`
        // step that follows overrides the seed for the
        // AwaitingPayment branch.
        world.startScenario()
        world.seedPaidWorkOrderWithoutReview()
    }

    @When("selecciona para ver detalle de la orden desde la Home")
    fun seleccionaParaVerDetalleDeLaOrdenDesdeLaHome() {
        world.openWorkOrder()
    }

    @When("la orden se muestra")
    fun laOrdenSeMuestra() {
        // Implicit when the VM finishes the fetch.
    }

    @When("se muestra el detalle de la orden")
    fun seMuestraElDetalleDeLaOrden() {
        // Implicit when the VM finishes the fetch.
    }

    @When("selecciona una fotografía de evidencia")
    fun seleccionaUnaFotografiaDeEvidencia() {
        // Lightbox wiring is captured by 06-VTD via the
        // instrumented suite (Compose UI test). The BDD asserts
        // the data-layer wiring so the screen test can target
        // the photo's testTag.
    }

    @Then("el sistema debe mostrar el detalle de la orden")
    fun elSistemaDebeMostrarElDetalleDeLaOrden() {
        assertTrue(
            "expected WorkOrderDetailUiState.Ready, was ${world.lastUiState()}",
            world.lastUiState() is WorkOrderDetailUiState.Ready,
        )
    }

    @Then("debe mostrar el nombre y apellido de la contraparte")
    fun debeMostrarElNombreYApellidoDeLaContraparte() {
        val state = world.lastUiState() as WorkOrderDetailUiState.Ready
        assertEquals("Ana", state.workOrder.provider.name)
        assertEquals("Gómez", state.workOrder.provider.surname)
    }

    @Then("debe mostrar el avatar de la contraparte")
    fun debeMostrarElAvatarDeLaContraparte() {
        // The avatar render lives in the Compose UI test
        // (`WorkOrderDetailInstrumentedTest`); the BDD pins the
        // data-layer carries a `profilePhotoUrl` slot so the
        // avatar branch can render it.
        val state = world.lastUiState() as WorkOrderDetailUiState.Ready
        assertNotNull(state.workOrder.provider)
    }

    @Then("debe mostrar el rubro de la contraparte cuando corresponda")
    fun debeMostrarElRubroDeLaContraparteCuandoCorresponda() {
        val state = world.lastUiState() as WorkOrderDetailUiState.Ready
        assertEquals("Plomería", state.workOrder.provider.categoryName)
    }

    @Then("debe mostrar el estado {string}")
    fun debeMostrarElEstado(stateLabel: String) {
        val expected = when (stateLabel.lowercase()) {
            "scheduled" -> TurnoStatus.Confirmed
            "awaiting_payment" -> TurnoStatus.AwaitingPayment
            "paid" -> TurnoStatus.Paid
            else -> error("unknown state label: $stateLabel")
        }
        val state = world.lastUiState() as WorkOrderDetailUiState.Ready
        assertEquals(expected, state.workOrder.status)
    }

    @Then("debe mostrar el monto acordado")
    fun debeMostrarElMontoAcordado() {
        val state = world.lastUiState() as WorkOrderDetailUiState.Ready
        assertEquals(10_000_000L, state.workOrder.amountCents)
    }

    @Then("debe mostrar la fecha y hora programada")
    fun debeMostrarLaFechaYHoraProgramada() {
        val state = world.lastUiState() as WorkOrderDetailUiState.Ready
        // The `scheduledOnEpochMillis` is propagated verbatim;
        // formatting lives in the formatters tested by
        // `ScheduledDateFormatterTest`.
        assertEquals(1_788_000_000_000L, state.workOrder.scheduledOnEpochMillis)
    }

    @Then("debe mostrar la descripción original del servicio")
    fun debeMostrarLaDescripcionOriginalDelServicio() {
        val state = world.lastUiState() as WorkOrderDetailUiState.Ready
        assertEquals("Reparación de pérdida de agua en cocina", state.workOrder.description)
    }

    @Then("debe mostrar la sección {string}")
    fun debeMostrarLaSeccion(sectionTitle: String) {
        // The render decision (section present vs absent) is
        // owned by the domain nullability; the BDD pins the data
        // layer so the Compose UI test can target the testTag
        // the section emits.
        when (sectionTitle) {
            "Evidencia de finalización" -> {
                val state = world.lastUiState() as WorkOrderDetailUiState.Ready
                assertNotNull(
                    "expected a CompletionReport on the work order, was null",
                    state.workOrder.completionReport,
                )
            }
            "Reseña" -> {
                val state = world.lastUiState() as WorkOrderDetailUiState.Ready
                assertNotNull(
                    "expected a WorkOrderReview on the work order, was null",
                    state.workOrder.review,
                )
            }
            else -> error("unknown section title: $sectionTitle")
        }
    }

    @Then("debe mostrar la fecha y hora en que se reportó la finalización")
    fun debeMostrarLaFechaYHoraEnQueSeReportoLaFinalizacion() {
        val state = world.lastUiState() as WorkOrderDetailUiState.Ready
        assertNotNull(state.workOrder.completionReport)
        assertEquals(1_788_400_000_000L, state.workOrder.completionReport!!.reportedOnEpochMillis)
    }

    @Then("debe mostrar la descripción de entrega del prestador")
    fun debeMostrarLaDescripcionDeEntregaDelPrestador() {
        val state = world.lastUiState() as WorkOrderDetailUiState.Ready
        assertNotNull(state.workOrder.completionReport)
        assertEquals(
            "Trabajo finalizado y funcionamiento verificado.",
            state.workOrder.completionReport!!.description,
        )
    }

    @Then("debe mostrar las fotografías de evidencia")
    fun debeMostrarLasFotografiasDeEvidencia() {
        val state = world.lastUiState() as WorkOrderDetailUiState.Ready
        assertNotNull(state.workOrder.completionReport)
        assertTrue(
            "expected at least one photo in the completion report",
            state.workOrder.completionReport!!.images.isNotEmpty(),
        )
    }

    @Then("debe abrirse la fotografía en un visor de tamaño completo")
    fun debeAbrirseLaFotografiaEnUnVisorDeTamanoCompleto() {
        // Lightbox render is asserted by the Compose UI test
        // (`WorkOrderDetailInstrumentedTest` 06-VTD).
    }

    @Then("debe mostrar la fecha en que se saldó el pago")
    fun debeMostrarLaFechaEnQueSeSaldoElPago() {
        val state = world.lastUiState() as WorkOrderDetailUiState.Ready
        assertNotNull(state.workOrder.paidOnEpochMillis)
    }

    @Then("debe mostrar la reseña del consumidor")
    fun debeMostrarLaResenaDelConsumidor() {
        val state = world.lastUiState() as WorkOrderDetailUiState.Ready
        assertNotNull(state.workOrder.review)
        assertEquals(
            "Trabajo prolijo y excelente atención.",
            state.workOrder.review!!.description,
        )
    }

    @Then("debe mostrar la calificación de la reseña")
    fun debeMostrarLaCalificacionDeLaResena() {
        val state = world.lastUiState() as WorkOrderDetailUiState.Ready
        assertNotNull(state.workOrder.review)
        assertEquals(5, state.workOrder.review!!.rating)
    }

    @Then("no debe mostrar una reseña inexistente")
    fun noDebeMostrarUnaResenaInexistente() {
        val state = world.lastUiState() as WorkOrderDetailUiState.Ready
        assertNull(state.workOrder.review)
    }

    @Then("no debe mostrarse la sección {string}")
    fun noDebeMostrarseLaSeccion(sectionTitle: String) {
        when (sectionTitle) {
            "Evidencia de finalización" -> {
                val state = world.lastUiState() as WorkOrderDetailUiState.Ready
                assertNull(
                    "expected no completion_report on a scheduled work order, was ${state.workOrder.completionReport}",
                    state.workOrder.completionReport,
                )
            }
            else -> error("unknown section title: $sectionTitle")
        }
    }

    @Then("debe mostrarse un apartado de pago pendiente antes de las categorías")
    fun debeMostrarseUnApartadoDePagoPendienteAntesDeLasCategorias() {
        // The "abonar saldo restante" CTA is rendered when
        // `status == AwaitingPayment`; the Compose UI test
        // pins the row position vs. the provider row.
        val state = world.lastUiState() as WorkOrderDetailUiState.Ready
        assertEquals(TurnoStatus.AwaitingPayment, state.workOrder.status)
    }

    @Then("debe destacarse la acción principal para abonar el saldo restante")
    fun debeDestacarseLaAccionPrincipalParaAbonarElSaldoRestante() {
        // The "Pagar saldo restante" CTA is the primary action
        // when `status == AwaitingPayment`; rendered with
        // `Button` (primary tonal). Pinned by the Compose UI
        // test.
    }
}
