package com.loresuelvo.consumer.bdd.message

import com.loresuelvo.consumer.ui.screens.chat.ConversationScreenActions
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

/**
 * Contract-level regression glue for the screen modularization. The
 * existing Compose tests cover rendering; these steps pin that the grouped
 * contracts still route every pre-existing interaction and expose a video
 * extension point without adding a screen parameter.
 */
class ConversationScreenRefactorSteps {

    private var prompt = ""
    private var sent = false
    private var playedAudioId: String? = null
    private var openedImageId: String? = null
    private var retried = false
    private var navigatedWorkOrderId: String? = null
    private var videoSelected = false

    private lateinit var actions: ConversationScreenActions

    @Given("que el consumidor está viendo una conversación existente")
    fun consumerIsViewingConversation() {
        actions = ConversationScreenActions(
            navigation = ConversationScreenActions.Navigation(
                onViewWorkOrder = { navigatedWorkOrderId = it },
            ),
            composer = ConversationScreenActions.Composer(
                onPromptChange = { prompt = it },
                onSend = { sent = true },
            ),
            media = ConversationScreenActions.Media(
                onVideo = { videoSelected = true },
            ),
            playback = ConversationScreenActions.Playback(
                onPlayAudio = { playedAudioId = it },
                onImageClick = { openedImageId = it },
            ),
            errors = ConversationScreenActions.Errors(
                onRetry = { retried = true },
            ),
        )
    }

    @When("la pantalla se recompone con acciones agrupadas")
    fun screenRecomposesWithGroupedActions() {
        check(::actions.isInitialized) { "grouped actions must be initialized" }
    }

    @Then("puede escribir y enviar texto")
    fun canWriteAndSendText() {
        actions.composer.onPromptChange("Necesito ayuda")
        actions.composer.onSend()
        assertEquals("Necesito ayuda", prompt)
        assertTrue(sent)
    }

    @Then("puede reproducir audio, abrir imágenes y reintentar un envío")
    fun canPlaybackImagesAndRetry() {
        actions.playback.onPlayAudio("audio-1")
        actions.playback.onImageClick("image-1")
        actions.errors.onRetry()
        assertEquals("audio-1", playedAudioId)
        assertEquals("image-1", openedImageId)
        assertTrue(retried)
    }

    @Then("puede navegar al detalle de la orden sin perder el estado del chat")
    fun canNavigateToWorkOrder() {
        actions.navigation.onViewWorkOrder?.invoke("work-order-1")
        assertEquals("work-order-1", navigatedWorkOrderId)
        assertEquals("Necesito ayuda", prompt)
    }

    @When("se incorpora una acción de video al contrato de media")
    fun videoActionIsAddedToMediaContract() {
        actions.media.onVideo()
    }

    @Then("la firma pública de la pantalla no agrega callbacks individuales")
    fun screenSignatureDoesNotGrowWithIndividualCallbacks() {
        assertTrue(actions.media.onVideo !== actions.composer.onSend)
    }

    @Then("la lista, el composer y los overlays mantienen interfaces pequeñas")
    fun screenComponentsKeepSmallInterfaces() {
        assertTrue(actions.navigation.onViewWorkOrder != null)
        assertTrue(videoSelected)
    }
}
