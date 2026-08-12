package com.loresuelvo.consumer.bdd.diagnosis

import io.cucumber.java.PendingException

/**
 * Placeholder step definitions for scenarios in
 * `features/diagnosis/ai_diagnosis.feature` that are still marked
 * `@wip`. Each method throws [PendingException] so the Cucumber
 * JUnit reporter classifies the scenario as **pending** (yellow)
 * instead of failing the build with `UndefinedStepException`.
 *
 * Each commit that goes green for one scenario deletes the
 * corresponding method(s) here and replaces them with the real
 * step implementation in [AiDiagnosisSteps]. Once the last
 * `@wip` lands, this file should be empty and can be removed.
 *
 * Method naming follows the wording of each Gherkin step verbatim
 * so the mapping is obvious during code review.
 */
@Suppress("unused", "UNUSED_PARAMETER")
class PendingSteps {

    // 09-DIA, 10-DIA, 11-DIA moved to AiDiagnosisSteps.

    // ---- Scenario: 12-DIA Ver sesiones previas del chat con IA -

    /**
     * TBD: the AI session list (`GET /chatbot/conversations`)
     * enumerates every conversation the consumer has had with the
     * assistant — the BDD world will enqueue a deterministic
     * server reply through the new `AiConversationRepository`
     * port (mirrors the pattern used by
     * `FakeDiagnosisRepository`) so the Assistant VM lands in
     * the "ready with N sessions" state.
     */
    @io.cucumber.java.en.And("he tenido {int} conversaciones previas con el asistente")
    fun heTenidoConversacionesPreviasConElAsistente(count: Int) {
        throw PendingException("12-DIA pendiente")
    }

    @io.cucumber.java.en.When("accedo al apartado \"Asistente IA\"")
    fun accedoAlApartadoAsistenteIA() {
        throw PendingException("12-DIA pendiente")
    }

    @io.cucumber.java.en.Then("veo una lista con mis {int} sesiones previas con la IA")
    fun veoUnaListaConMisSesionesPreviasConLaIA(count: Int) {
        throw PendingException("12-DIA pendiente")
    }

    @io.cucumber.java.en.And("cada sesión muestra el título y la fecha del último mensaje")
    fun cadaSesionMuestraElTituloYLaFechaDelUltimoMensaje() {
        throw PendingException("12-DIA pendiente")
    }
}
