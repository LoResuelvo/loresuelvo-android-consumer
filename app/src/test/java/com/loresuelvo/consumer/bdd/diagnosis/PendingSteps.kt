package com.loresuelvo.consumer.bdd.diagnosis

import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
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

    // 09-DIA, 10-DIA moved to AiDiagnosisSteps.

    // ---- Scenario: 11-DIA Contactar prestador desde el chat ------
    //
    // The happy-path flow when the user taps "Contactar" on a
    // recommended provider tile INSIDE the AI diagnostic chat.
    // The wire shape is `POST /chatbot/conversations/{convId}/job-requests`
    // with `{provider_id}` — the backend's AI pre-fills the
    // `title` and `description` (mirrors the webapp behavior at
    // `useAiDiagnosisChat.ts:handleContactProvider`). The two
    // `Then` steps pin (a) the request is sent with the right
    // provider and (b) the navigation event lands on the existing
    // `Route.Conversation` with the `conversation_id` the backend
    // returned.

    @When("toco {string} en el primer prestador recomendado")
    fun tocoEnElPrimerPrestadorRecomendado(label: String) {
        throw PendingException("11-DIA pendiente")
    }

    @Then("la IA envía su propio resumen para ese prestador")
    fun laIaEnviaSuPropioResumenParaEsePrestador() {
        throw PendingException("11-DIA pendiente")
    }

    @And("la app navega a la conversación con ese prestador")
    fun laAppNavegaALaConversacionConEsePrestador() {
        throw PendingException("11-DIA pendiente")
    }
}
