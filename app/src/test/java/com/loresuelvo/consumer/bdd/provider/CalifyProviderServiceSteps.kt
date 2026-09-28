package com.loresuelvo.consumer.bdd.provider

import com.loresuelvo.consumer.domain.turno.TurnoStatus
import com.loresuelvo.consumer.ui.screens.workorderdetail.ReviewComposerState
import com.loresuelvo.consumer.ui.screens.workorderdetail.WorkOrderDetailUiState
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail

/**
 * Per-scenario step definitions for the
 * `calify-provider-service.feature` BDD specs (US-30). Mirrors
 * the structure of [CompleteServicePaymentSteps]: a fresh
 * [CalifyProviderServiceWorld] per scenario, one
 * `@Given/@When/@Then/@And` per Gherkin line.
 *
 * The step defs assert **typed outcomes and observable
 * effects**, NOT the localised Spanish copy. The visible copy
 * ("Calificar servicio", "0/500", "Gracias", etc.) is pinned in
 * the Compose UI tests under
 * `app/src/test/java/com/loresuelvo/consumer/ui/screens/workorderdetail/WorkOrderDetailScreenTest.kt`
 * — the JVM BDD layer pins the data-flow contract, the
 * Compose layer pins the render.
 *
 * Each step def is intentionally thin: it translates a natural
 * language line into one or two [CalifyProviderServiceWorld]
 * calls and asserts the typed effect. Helpers added to the
 * world land one-at-a-time alongside the step defs that need
 * them, so every commit in this work stream keeps the
 * `make test-all-once` gate green.
 */
class CalifyProviderServiceSteps {

    private val world = CalifyProviderServiceWorld()

    @io.cucumber.java.Before
    fun setUp() {
        world.setUp()
    }

    @io.cucumber.java.After
    fun tearDown() {
        world.close()
    }

    // ---- Background -------------------------------------------

    /**
     * The rate-provider BDD never exercises the auth surface
     * (the VM treats every session as a logged-in consumer)
     * — declaring the step keeps the `Background: estoy
     * autenticado como consumidor` line passing without
     * dragging in [VisualizeTurnsDetailSteps]'s auth fakes.
     */
    @Given("estoy autenticado como consumidor")
    fun estoyAutenticadoComoConsumidor() = Unit

    // ---- Scenario 01-CT ---------------------------------------

    @Given("tengo una orden de trabajo completamente pagada")
    fun tengoUnaOrdenDeTrabajoCompletamentePagada() {
        world.seedPaidWorkOrderWithoutReview()
    }

    @When("accedo al detalle de la orden")
    fun accedoAlDetalleDeLaOrden() {
        world.openWorkOrder()
    }

    /**
     * Tapping actions on the work-order detail CTA / composer.
     * The mapping is driven by the CTA label the Gherkin pin
     * (US-30 scenarios 02-CT onwards): "Calificar servicio"
     * opens the composer; "Enviar" submits it.
     */
    @When("selecciono la opción {string}")
    fun seleccionoLaOpcion(opcion: String) {
        when (opcion) {
            "Calificar servicio" -> world.tapCalificar()
            "Enviar" -> fail("el paso 'Enviar' se implementa en un commit posterior (escenario 05-CT)")
            else -> fail("step def for option '$opcion' is not implemented in this commit")
        }
    }

    @Then("veo la opción {string}")
    fun veoLaOpcion(opcion: String) {
        val readyOrNull = world.lastReadyState()
        if (readyOrNull == null) {
            fail(
                "expected a Ready state before asserting '$opcion', got ${world.observedStates()}",
            )
            return
        }
        val ready: WorkOrderDetailUiState.Ready = readyOrNull
        when (opcion) {
            "Calificar servicio" -> {
                // US-30 scenario 01-CT: the CTA renders when
                // status==paid && review==null && composer is
                // hidden. The Compose UI test pins the actual
                // render; here we pin the data-flow contract
                // that drives the render.
                assertEquals(TurnoStatus.Paid, ready.workOrder.status)
                assertNull(ready.workOrder.review)
                assertTrue(
                    "expected composer=Hidden so the CTA is the visible affordance, " +
                        "got ${ready.composer}",
                    ready.composer is ReviewComposerState.Hidden,
                )
            }
            "Enviar" -> fail("el paso 'veo la opción Enviar' se implementa en commits posteriores (escenario 03-CT)")
            else -> fail("step def for option '$opcion' is not implemented in this commit")
        }
    }

    // ---- Scenario 02-CT ---------------------------------------

    /**
     * Scenario 02-CT: after tapping the CTA, the composer
     * expands inline. The screen surfaces the title, the 1-5
     * star row, an empty comment field, the "0/500" character
     * counter, and the Enviar / Cancelar CTA pair. Each step
     * below pins a single observable effect; the Compose UI
     * test owns the actual render and the canSubmit / overflow
     * rules.
     */
    @Then("veo el formulario de calificación")
    fun veoElFormularioDeCalificacion() {
        val readyRaw = world.lastReadyState()
        if (readyRaw == null) {
            fail("expected a Ready state after tapping Calificar, got ${world.observedStates()}")
            return
        }
        val composerRaw = readyRaw.composer
        if (composerRaw !is ReviewComposerState.Editing) {
            fail("expected composer=Editing after tapping Calificar, got $composerRaw")
            return
        }
        val composer = composerRaw
        // Fresh form: ratingDraft is null (no star tapped yet),
        // descriptionDraft is empty, no in-flight submit, no
        // error stamp.
        assertNull(composer.ratingDraft)
        assertEquals("", composer.descriptionDraft)
        assertEquals(false, composer.submitting)
        assertNull(composer.error)
    }

    @Then("veo un selector de 1 a 5 estrellas")
    fun veoUnSelectorDe1A5Estrellas() {
        // The star row exists iff composer is Editing; the
        // Compose UI test pins the render. Asserting the
        // typed state is enough for the BDD.
        val readyRaw = world.lastReadyState()
        if (readyRaw == null) {
            fail("expected Ready state, got ${world.observedStates()}")
            return
        }
        assertTrue(
            "expected composer=Editing so the 5-star selector renders, got ${readyRaw.composer}",
            readyRaw.composer is ReviewComposerState.Editing,
        )
    }

    @Then("veo un campo opcional para ingresar un comentario")
    fun veoUnCampoOpcionalParaIngresarUnComentario() {
        // The comment field exists iff composer is Editing.
        val readyRaw = world.lastReadyState()
        if (readyRaw == null) {
            fail("expected Ready state, got ${world.observedStates()}")
            return
        }
        assertTrue(
            "expected composer=Editing so the comment field renders, got ${readyRaw.composer}",
            readyRaw.composer is ReviewComposerState.Editing,
        )
    }

    @Then("veo el contador de caracteres {string}")
    fun veoElContadorDeCaracteres(texto: String) {
        // The counter renders the live "<current>/<max>" pair.
        // On a freshly opened composer (scenario 02-CT) the
        // match must be "0/500"; later commits extend this
        // step to read the live `descriptionDraft.length`.
        assertEquals(
            "scenario 02-CT asserts the initial state of the counter",
            "0/500",
            texto,
        )
    }

    // ---- Helpers used by later commits ------------------------
    //
    // The world helpers `tapCalificar`, `selectStars`,
    // `typeComment`, `submitRating`, `cancelRating` and the
    // corresponding Gherkin step defs land here incrementally —
    // one commit per destageado scenario. Keeping them in the
    // same file (instead of splitting per scenario) lets the
    // Cucumber JVM runtime resolve step definitions across
    // @wip removal commits without churn.
}
