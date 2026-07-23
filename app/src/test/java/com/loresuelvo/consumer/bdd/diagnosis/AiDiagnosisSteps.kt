package com.loresuelvo.consumer.bdd.diagnosis

import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When

/**
 * Step defs for `features/diagnosis/ai_diagnosis.feature`. Each step
 * is intentionally thin: the heavy lifting lives in [AiDiagnosisWorld]
 * which pins `Dispatchers.Main` and drives the [com.loresuelvo.consumer.ui.screens.chat.ChatViewModel]
 * through a `StandardTestDispatcher` against a [FakeDiagnosisRepository].
 *
 * Cucumber instantiates this class with its zero-arg constructor on
 * a per-scenario basis; `close()` is invoked from the JVM shutdown
 * hook.
 *
 * Steps that live here today:
 *  - 01-DIA: typing + sending surfaces the user's optimistic message.
 *  - 02-DIA: the assistant round-trip becomes visible in the chat.
 *  - 06-DIA: structural assertion that `Route.Chat` is registered.
 *
 * Steps for the remaining `@wip` scenarios live in [PendingSteps].
 */
class AiDiagnosisSteps {

    private val world: AiDiagnosisWorld = AiDiagnosisWorld()

    @Given("estoy autenticado como consumidor")
    fun estoyAutenticadoComoConsumidor() {
        world.startScenario()
    }

    @Given("me encuentro en la pantalla Home")
    fun meEncuentroEnLaPantallaHome() {
        // The chat VM does not depend on any Home state — the step
        // exists only to mirror the Gherkin Background and make the
        // scenarios read like a user journey. The Home screen's own
        // behaviours are covered by `bdd/home/HomeSteps`.
    }

    @When("ingreso un mensaje {string} en el campo de diagnóstico")
    fun ingresoUnMensajeEnElCampoDeDiagnostico(text: String) {
        world.typePrompt(text)
    }

    @And("presiono {string}")
    fun presiono(label: String) {
        when (label) {
            "Diagnosticar" -> world.tapSend()
            else -> error("Acción desconocida en el flujo de chat: $label")
        }
    }

    @Then("se inicia una conversación con el asistente")
    fun seIniciaUnaConversacionConElAsistente() {
        world.assertConversationStarted()
    }

    @Then("veo mi mensaje en el chat")
    fun veoMiMensajeEnElChat() {
        world.assertUserMessageVisible(world.lastTypedPromptSnapshot())
    }

    // ---- Scenario: 02-DIA Recibir respuesta del asistente ----------

    /**
     * 02-DIA "Given": the consumer has already kicked off the
     * conversation. We combine the typing + send from 01-DIA's
     * flow here so the launched coroutine sits in the queue
     * waiting for the seeded fake response.
     */
    @Given("inicié una conversación con el asistente")
    fun inicieUnaConversacionConElAsistente() {
        world.startConversationWithSeededResponse()
    }

    @When("el asistente procesa mi mensaje")
    fun elAsistenteProcesaMiMensaje() {
        world.simulateAssistantResponse()
    }

    @Then("veo una respuesta del asistente en el chat")
    fun veoUnaRespuestaDelAsistenteEnElChat() {
        world.assertAssistantMessageVisible()
    }

    // ---- Scenario: 06-DIA Navegar al chat de IA --------------------

    @When("selecciono la opción {string}")
    fun seleccionoLaOpcion(opcion: String) {
        when (opcion) {
            "Chat con IA" -> world.recordChatWithAiIntent()
            else -> error("Opción desconocida en la pantalla Home: $opcion")
        }
    }

    @Then("veo la pantalla de conversación con el asistente")
    fun veoLaPantallaDeConversacionConElAsistente() {
        // Structural assertion only — the chat route is registered
        // with its expected path. The actual "the chat surface is
        // rendered" proof is the Compose acceptance test.
        world.assertChatScreenRouteAvailable()
    }
}
