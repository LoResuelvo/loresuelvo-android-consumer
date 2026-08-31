package com.loresuelvo.consumer.bdd.fixes

import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When

/**
 * Step defs for `features/fixes/ux_ui_fixes.feature`. Each step
 * delegates to [UxUiFixesWorld] which pins `Dispatchers.Main` and
 * drives [com.loresuelvo.consumer.ui.screens.chat.ChatViewModel]
 * through a `StandardTestDispatcher`.
 *
 * The runner filters `not @wip` globally (`app/build.gradle.kts`),
 * so steps for scenarios still marked `@wip` are registered but
 * never executed today. Each commit that turns one scenario green
 * removes the `@wip` marker from the feature and replaces the
 * matching no-op body in this file with the real assertion.
 *
 * Steps for `01-UXUI` (the only green scenario today):
 *  - verifies [com.loresuelvo.consumer.ui.screens.chat.ChatUiState.audioEnabled]
 *    defaults to `false` so
 *    [com.loresuelvo.consumer.ui.screens.chat.ChatInputBar] hides
 *    the Mic / Stop buttons. The visual absence of the Mic is
 *    pinned by the Compose-test
 *    `ChatInputBarTest.hides_microphone_when_audio_disabled_and_prompt_empty`.
 */
class UxUiFixesSteps {

    private val world: UxUiFixesWorld = UxUiFixesWorld()

    // ---- Scenario 01-UXUI -----------------------------------------

    @Given("que la funcionalidad de audio para IA no está disponible")
    fun laFuncionalidadDeAudioParaIaNoEstaDisponible() {
        world.startScenario()
    }

    @When("visualizo la pantalla correspondiente")
    fun visualizoLaPantallaCorrespondiente() {
        // The chat screen is mounted by the Compose route; the
        // BDD layer only inspects the VM state observable to it.
    }

    @Then("el ícono de audio no debe mostrarse")
    fun elIconoDeAudioNoDebeMostrarse() {
        val state = world.lastUiState()
        if (state.audioEnabled) {
            error(
                "expected ChatUiState.audioEnabled=false so the Mic is hidden " +
                    "(01-UXUI), but audioEnabled=${state.audioEnabled}",
            )
        }
    }

    // ---- Scenario 02-UXUI (wip) -----------------------------------

    @Given("que estoy en la aplicación")
    fun queEstoyEnLaAplicacion() {
        // No-op: covered when scenario 02 goes green.
    }

    @When("accedo a la sección de categorías")
    fun accedoALaSeccionDeCategorias() {
        // No-op: covered when scenario 02 goes green.
    }

    @Then("veo una pantalla con todas las categorías disponibles")
    fun veoUnaPantallaConTodasLasCategoriasDisponibles() {
        // No-op: covered when scenario 02 goes green.
    }

    // ---- Scenario 03-UXUI (wip) -----------------------------------

    @Given("que estoy creando una oferta de trabajo")
    fun queEstoyCreandoUnaOfertaDeTrabajo() {
        // No-op: covered when scenario 03 goes green.
    }

    @When("selecciono una o más imágenes desde el dispositivo")
    fun seleccionoUnaOMasImagenesDesdeElDispositivo() {
        // No-op: covered when scenario 03 goes green.
    }

    @Then("las imágenes quedan adjuntadas a la oferta")
    fun lasImagenesQuedanAdjuntadasALaOferta() {
        // No-op: covered when scenario 03 goes green.
    }

    // ---- Scenario 04-UXUI (wip) -----------------------------------

    @Given("que seleccioné una o más imágenes para mi oferta de trabajo")
    fun queSeleccioneUnaOMasImagenesParaMiOfertaDeTrabajo() {
        // No-op: covered when scenario 04 goes green.
    }

    @When("continúo con la creación de la oferta")
    fun continuoConLaCreacionDeLaOferta() {
        // No-op: covered when scenario 04 goes green.
    }

    @Then("veo una vista previa de las imágenes seleccionadas")
    fun veoUnaVistaPreviaDeLasImagenesSeleccionadas() {
        // No-op: covered when scenario 04 goes green.
    }

    // ---- Scenario 05-UXUI (wip) -----------------------------------

    @Given("que tengo una o más imágenes seleccionadas para mi oferta de trabajo")
    fun queTengoUnaOMasImagenesSeleccionadasParaMiOfertaDeTrabajo() {
        // No-op: covered when scenario 05 goes green.
    }

    @When("elimino una de las imágenes")
    fun eliminoUnaDeLasImagenes() {
        // No-op: covered when scenario 05 goes green.
    }

    @Then("la imagen deja de estar adjuntada a la oferta")
    fun laImagenDejaDeEstarAdjuntadaALaOferta() {
        // No-op: covered when scenario 05 goes green.
    }

    // ---- Scenario 06-UXUI (wip) -----------------------------------

    @Given("que tengo un chat con mensajes nuevos sin leer")
    fun queTengoUnChatConMensajesNuevosSinLeer() {
        // No-op: covered when scenario 06 goes green.
    }

    @When("visualizo la lista de chats")
    fun visualizoLaListaDeChats() {
        // No-op: covered when scenario 06 goes green.
    }

    @Then("veo una indicación visual de que el chat tiene nuevos mensajes")
    fun veoUnaIndicacionVisualDeQueElChatTieneNuevosMensajes() {
        // No-op: covered when scenario 06 goes green.
    }

    // ---- Scenario 07-UXUI (wip) -----------------------------------

    @When("ingreso al chat")
    fun ingresoAlChat() {
        // No-op: covered when scenario 07 goes green.
    }

    @And("visualizo los mensajes pendientes")
    fun visualizoLosMensajesPendientes() {
        // No-op: covered when scenario 07 goes green.
    }

    @And("el indicador de nuevos mensajes deja de mostrarse para ese chat")
    fun elIndicadorDeNuevosMensajesDejaDeMostrarseParaEseChat() {
        // No-op: covered when scenario 07 goes green.
    }

    // ---- Scenario 08-UXUI (wip) -----------------------------------

    @Given("que navego por las distintas pantallas de la aplicación")
    fun queNavegoPorLasDistintasPantallasDeLaAplicacion() {
        // No-op: covered when scenario 08 goes green.
    }

    @When("visualizo los componentes de la interfaz")
    fun visualizoLosComponentesDeLaInterfaz() {
        // No-op: covered when scenario 08 goes green.
    }

    @Then("los bordes y márgenes se muestran correctamente")
    fun losBordesYMargenesSeMuestranCorrectamente() {
        // No-op: covered when scenario 08 goes green.
    }

    @And("ningún elemento aparece cortado")
    fun ningunElementoApareceCortado() {
        // No-op: covered when scenario 08 goes green.
    }

    @And("ningún elemento aparece desbordado")
    fun ningunElementoApareceDesbordado() {
        // No-op: covered when scenario 08 goes green.
    }

    @And("ningún elemento aparece fuera de los límites de la pantalla")
    fun ningunElementoApareceFueraDeLosLimitesDeLaPantalla() {
        // No-op: covered when scenario 08 goes green.
    }
}
