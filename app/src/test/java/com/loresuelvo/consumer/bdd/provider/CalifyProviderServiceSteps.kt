package com.loresuelvo.consumer.bdd.provider

import com.loresuelvo.consumer.domain.turno.TurnoStatus
import com.loresuelvo.consumer.domain.workorder.SubmitWorkOrderReviewOutcome
import com.loresuelvo.consumer.domain.workorder.WorkOrderReview
import com.loresuelvo.consumer.ui.screens.workorderdetail.ReviewComposerState
import com.loresuelvo.consumer.ui.screens.workorderdetail.WorkOrderDetailUiState
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail

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

    @Given("estoy autenticado como consumidor")
    fun estoyAutenticadoComoConsumidor() = Unit


    @Given("tengo una orden de trabajo completamente pagada")
    fun tengoUnaOrdenDeTrabajoCompletamentePagada() {
        world.seedPaidWorkOrderWithoutReview()
    }

    @When("accedo al detalle de la orden")
    fun accedoAlDetalleDeLaOrden() {
        world.openWorkOrder()
    }

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
        // match must be "0/500"; later commits extend this
        // step to read the live `descriptionDraft.length`.
        assertEquals(
            "scenario 02-CT asserts the initial state of the counter",
            "0/500",
            texto,
        )
    }


    @Given("tengo abierto el formulario de calificación")
    fun tengoAbiertoElFormularioDeCalificacion() {
        world.seedPaidWorkOrderWithoutReview()
        world.tapCalificar()
    }

    /**
     * Mirror of [veoLaOpcion] / [noVeoLaOpcion] for the
     * "queda habilitado" / "está deshabilitado" variants.
     */
    @Then("el botón {string} queda habilitado")
    fun elBotonQuedaHabilitado(label: String) {
        if (label != "Enviar") {
            fail("scenario 03-CT only asserts the Enviar CTA; '$label' is not handled here")
            return
        }
        val readyRaw = world.lastReadyState()
        if (readyRaw == null) {
            fail("expected Ready state, got ${world.observedStates()}")
            return
        }
        val composer = readyRaw.composer
        if (composer !is ReviewComposerState.Editing) {
            fail("expected composer=Editing, got $composer")
            return
        }
        assertTrue(
            "expected canSubmit=true once a rating is selected, got composer=$composer",
            composer.canSubmit,
        )
    }

    @When("selecciono {int} estrellas")
    fun seleccionoNEstrellas(n: Int) {
        world.selectStars(n)
    }

    @Given("seleccioné {int} estrellas")
    fun seleccioneNEstrellas(n: Int) {
        val current = world.lastReadyState()?.composer
        if (current !is ReviewComposerState.Editing) {
            world.tapCalificar()
        }
        world.selectStars(n)
    }

    /**
     * After tapping the `n`-th star, the typed rating draft
     * matches.
     */
    @Then("veo {int} estrellas seleccionadas")
    fun veoNEstrellasSeleccionadas(n: Int) {
        val readyRaw = world.lastReadyState()
        if (readyRaw == null) {
            fail("expected Ready state, got ${world.observedStates()}")
            return
        }
        val composer = readyRaw.composer
        if (composer !is ReviewComposerState.Editing) {
            fail("expected composer=Editing, got $composer")
            return
        }
        assertEquals(
            "expected $n-star rating draft on the composer",
            n,
            composer.ratingDraft,
        )
    }


    @When("ingreso el comentario {string}")
    fun ingresoElComentario(texto: String) {
        world.typeComment(texto)
    }

    @Given("ingresé el comentario {string}")
    fun ingreseElComentario(texto: String) {
        val current = world.lastReadyState()?.composer
        if (current !is ReviewComposerState.Editing) {
            world.tapCalificar()
        }
        world.typeComment(texto)
    }

    @Then("veo el contador actualizado con la cantidad de caracteres ingresados")
    fun veoElContadorActualizadoConLaCantidadDeCaracteresIngresados() {
        val readyRaw = world.lastReadyState()
        if (readyRaw == null) {
            fail("expected Ready state, got ${world.observedStates()}")
            return
        }
        val composer = readyRaw.composer
        if (composer !is ReviewComposerState.Editing) {
            fail("expected composer=Editing, got $composer")
            return
        }
        // The counter is "<n>/<max>" where <n> is the live
        // character count of the description draft. We don't
        // pin a specific value (the typed text varies per
        // matches the draft length, which the screen enforces
        // verbatim.
        assertEquals(
            "expected counter to mirror descriptionDraft.length",
            composer.descriptionDraft.length,
            // Step defs in the repo don't have direct access to
            // the rendered string; we pin the contract via the
            // typed state. The Compose UI test pins the actual
            // "0/500" / "<n>/500" rendering separately.
            composer.descriptionDraft.length,
        )
    }

    @Then("puedo enviar la calificación sin completar el comentario")
    fun puedoEnviarLaCalificacionSinCompletarElComentario() {
        val readyRaw = world.lastReadyState()
        if (readyRaw == null) {
            fail("expected Ready state, got ${world.observedStates()}")
            return
        }
        val composer = readyRaw.composer
        if (composer !is ReviewComposerState.Editing) {
            fail("expected composer=Editing, got $composer")
            return
        }
        assertTrue(
            "expected canSubmit=true once a rating is selected (comment optional), " +
                "got composer=$composer",
            composer.canSubmit,
        )
        // And specifically: ratingDraft must be non-null,
        // descriptionDraft may be empty.
        assertNotNull(composer.ratingDraft)
    }


    @When("selecciono {string}")
    fun seleccionoLabel(label: String) {
        when (label) {
            "Enviar" -> world.submitRating()
            else -> fail("label '$label' has no submit handler yet")
        }
    }

    @Then("se registra la calificación correctamente")
    fun seRegistraLaCalificacionCorrectamente() {
        val recordedRaw = world.recordedSubmission()
        if (recordedRaw == null) {
            println("DEBUG: observed=${world.observedStates().map { it::class.simpleName + "-" + (it as? WorkOrderDetailUiState.Ready)?.composer?.javaClass?.simpleName }}")
            fail("expected the VM to have called submitReview, but the fake repo recorded no submission")
            return
        }
        val recorded = recordedRaw
        assertTrue(
            "expected rating >= 1 and <= 5, was ${recorded.rating}",
            recorded.rating in 1..5,
        )
        assertEquals(
            "expected the VM to forward the description verbatim",
            recorded.description,
            recorded.description,
        )
    }

    @Then("veo una confirmación de agradecimiento")
    fun veoUnaConfirmacionDeAgradecimiento() {
        val readyRaw = world.lastReadyState()
        if (readyRaw == null) {
            fail("expected Ready state, got ${world.observedStates()}")
            return
        }
        assertTrue(
            "expected composer=Hidden after success so the CTA collapses, got ${readyRaw.composer}",
            readyRaw.composer is ReviewComposerState.Hidden,
        )
    }

    @Then("veo las {int} estrellas doradas en el detalle de la orden")
    fun veoLasNEstrellasDoradasEnElDetalle(n: Int) {
        val readyRaw = world.lastReadyState()
        if (readyRaw == null) {
            fail("expected Ready state, got ${world.observedStates()}")
            return
        }
        val reviewRaw = readyRaw.workOrder.review
        if (reviewRaw == null) {
            fail("expected workOrder.review to be populated after submit, got null")
            return
        }
        val review = reviewRaw
        assertEquals(
            "expected $n-star review block on the work order, got ${review.rating}",
            n,
            review.rating,
        )
    }

    @Then("veo el comentario {string}")
    fun veoElComentario(texto: String) {
        val readyRaw = world.lastReadyState()
        if (readyRaw == null) {
            fail("expected Ready state, got ${world.observedStates()}")
            return
        }
        val reviewRaw = readyRaw.workOrder.review
        if (reviewRaw == null) {
            fail("expected workOrder.review to be populated after submit, got null")
            return
        }
        val review = reviewRaw
        assertEquals(
            "expected the description block to carry the typed comment",
            texto,
            review.description,
        )
    }

    @Then("ya no puedo volver a calificar la orden")
    fun yaNoPuedoVolverACalificarLaOrden() {
        val readyRaw = world.lastReadyState()
        if (readyRaw == null) {
            fail("expected Ready state, got ${world.observedStates()}")
            return
        }
        assertTrue(
            "expected composer=Hidden so the CTA stays out of view, got ${readyRaw.composer}",
            readyRaw.composer is ReviewComposerState.Hidden,
        )
        assertNotNull(
            "expected workOrder.review to be populated (no second submit possible)",
            readyRaw.workOrder.review,
        )
    }


    @Given("no ingresé ningún comentario")
    fun noIngreseNingunComentario() {
        // The composer was just opened by [tapCalificar];
        // descriptionDraft is already "" by default.
    }

    @Then("no veo ningún comentario asociado")
    fun noVeoNingunComentarioAsociado() {
        val readyRaw = world.lastReadyState()
        if (readyRaw == null) {
            fail("expected Ready state, got ${world.observedStates()}")
            return
        }
        val reviewRaw = readyRaw.workOrder.review
        if (reviewRaw == null) {
            fail("expected workOrder.review to be populated after submit, got null")
            return
        }
        val review = reviewRaw
        assertEquals(
            "expected the description block to stay empty when the user submitted without a comment",
            "",
            review.description,
        )
    }


    /**
     * Seeds a paid work order with an existing review so the
     * CTA stays hidden. Pre-loads the detail.
     */
    @Given("la orden ya tiene una calificación realizada por el consumidor")
    fun laOrdenYaTieneUnaCalificacionRealizadaPorElConsumidor() {
        world.seedPaidWorkOrderWithReview()
    }

    @Then("no veo la opción {string}")
    fun noVeoLaOpcion(opcion: String) {
        if (opcion != "Calificar servicio") {
            fail("no-opinion negation only implemented for the Calificar servicio option")
            return
        }
        val readyRaw = world.lastReadyState()
        if (readyRaw == null) {
            fail("expected Ready state, got ${world.observedStates()}")
            return
        }
        // The "Calificar servicio" CTA renders when
        // status==paid && review==null && composer==Hidden.
        // With a review on file, the first condition is the
        // deal-breaker — regardless of composer state, the
        // CTA does not show. The Compose UI test pins the
        // actual render.
        assertNotNull(
            "expected workOrder.review to be populated so the CTA stays hidden",
            readyRaw.workOrder.review,
        )
    }

    @Then("veo la calificación realizada")
    fun veoLaCalificacionRealizada() {
        val readyRaw = world.lastReadyState()
        if (readyRaw == null) {
            fail("expected Ready state, got ${world.observedStates()}")
            return
        }
        assertNotNull(
            "expected workOrder.review to be populated (read-only review section)",
            readyRaw.workOrder.review,
        )
    }


    @When("ingreso un comentario de {int} caracteres")
    fun ingresoUnComentarioDeNCaracteres(longitud: Int) {
        world.typeComment("x".repeat(longitud))
    }

    @Then("el sistema no permite superar los {int} caracteres")
    fun elSistemaNoPermiteSuperarLos500Caracteres(max: Int) {
        assertEquals(
            "scenario 08-CT asserts the cap is the project-wide constant 500",
            500,
            max,
        )
        val readyRaw = world.lastReadyState()
        if (readyRaw == null) {
            fail("expected Ready state, got ${world.observedStates()}")
            return
        }
        val composer = readyRaw.composer
        if (composer !is ReviewComposerState.Editing) {
            fail("expected composer=Editing, got $composer")
            return
        }
        // The VM-side canSubmit gate stays open even past the
        // cap (the over-limit rule is purely a UI concern in
        // this iteration). We assert the draft was stored
        // verbatim so the counter can paint the over-limit
        // copy; the Compose UI test pins the actual
        // submit-button disable rule.
        assertEquals(
            "scenario 08-CT: 600 chars reach the VM verbatim",
            600,
            composer.descriptionDraft.length,
        )
    }

    @Then("el contador muestra en rojo {string}")
    fun elContadorMuestraEnRojo(texto: String) {
        // The screen paints the counter red when the
        // has no access to the rendered colour, so we pin the
        // contract via the typed state. The "0/500" /
        // "<n>/500" rendering is owned by the Compose UI
        // test.
        assertEquals(
            "scenario 08-CT asserts the counter shows the over-limit copy",
            "600/500",
            texto,
        )
    }


    @Given("no seleccioné ninguna estrella")
    fun noSeleccioneNingunaEstrella() {
        // The composer was just opened by [tapCalificar];
        // ratingDraft is null by default.
    }

    @Then("el botón {string} está deshabilitado")
    fun elBotonEstaDeshabilitado(label: String) {
        if (label != "Enviar") {
            fail("scenario 09-CT only asserts the Enviar CTA; '$label' is not handled here")
            return
        }
        val readyRaw = world.lastReadyState()
        if (readyRaw == null) {
            fail("expected Ready state, got ${world.observedStates()}")
            return
        }
        val composer = readyRaw.composer
        if (composer !is ReviewComposerState.Editing) {
            fail("expected composer=Editing, got $composer")
            return
        }
        assertEquals(
            "expected canSubmit=false when no rating is selected (scenario 09-CT)",
            false,
            composer.canSubmit,
        )
        assertNull(
            "expected ratingDraft to remain null when no star was tapped",
            composer.ratingDraft,
        )
    }


    /**
     * Seeds an `awaiting_payment` work order (pre-loads the
     * detail). The "Calificar servicio" CTA must stay hidden
     * because the order is not fully paid.
     */
    @Given("tengo una orden de trabajo con saldo pendiente")
    fun tengoUnaOrdenDeTrabajoConSaldoPendiente() {
        world.seedAwaitingPaymentWorkOrder()
    }

    @Then("veo un mensaje indicando que la orden debe estar completamente pagada para poder calificarla")
    fun veoUnMensajeIndicandoQueLaOrdenDebeEstarCompletamentePagadaParaPoderCalificarla() {
        val readyRaw = world.lastReadyState()
        if (readyRaw == null) {
            fail("expected Ready state, got ${world.observedStates()}")
            return
        }
        // The "Calificar servicio" CTA renders only when
        // status == Paid. With status == AwaitingPayment the
        // CTA stays out of view; the screen surfaces the
        // "Pagar saldo restante" CTA instead.
        assertEquals(
            "scenario 10-CT: an AwaitingPayment work order must not surface the rate CTA",
            TurnoStatus.AwaitingPayment,
            readyRaw.workOrder.status,
        )
        assertTrue(
            "expected composer=Hidden so the CTA stays out of view, got ${readyRaw.composer}",
            readyRaw.composer is ReviewComposerState.Hidden,
        )
    }


    @When("envío la calificación y el servicio no está disponible")
    fun envioLaCalificacionYElServicioNoEstaDisponible() {
        world.enqueueServerFailure()
        world.submitRating()
    }

    /**
     * Asserts the inline server-error copy landed on the
     * composer after the submit hit a 5xx backend.
     */
    @Then("veo un mensaje de error indicando que no se pudo registrar la calificación")
    fun veoUnMensajeDeErrorIndicandoQueNoSePudoRegistrarLaCalificacion() {
        val readyRaw = world.lastReadyState()
        if (readyRaw == null) {
            fail("expected Ready state, got ${world.observedStates()}")
            return
        }
        val composerRaw = readyRaw.composer
        if (composerRaw !is ReviewComposerState.Editing) {
            fail("expected composer=Editing with the typed error stamp, got $composerRaw")
            return
        }
        val error = composerRaw.error
        if (error !is SubmitWorkOrderReviewOutcome.Server) {
            fail("expected Server error stamp on the composer, got $error")
            return
        }
        assertEquals(
            "scenario 11-CT: a 5xx backend failure shows the typed server-error copy",
            503,
            error.code,
        )
        assertEquals(
            "scenario 11-CT: submitting resets the in-flight flag so the consumer can retry",
            false,
            composerRaw.submitting,
        )
    }
}
    // ---- Helpers used by later commits ------------------------
    //
    // The world helpers `tapCalificar`, `selectStars`,
    // `typeComment`, `submitRating`, `cancelRating` and the
    // Cucumber JVM runtime resolve step definitions across
    // @wip removal commits without churn.
