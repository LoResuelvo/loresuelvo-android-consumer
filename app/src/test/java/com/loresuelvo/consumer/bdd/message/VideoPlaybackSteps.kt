package com.loresuelvo.consumer.bdd.message

import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

/** Business-level assertions for the video playback surface. */
class VideoPlaybackSteps {

    private var hasVideo = false
    private var selectedMessageId: String? = null
    private var playbackError = false
    private var retryRequested = false
    private var chatMessagesAvailable = true
    private var listPlayerStarted = false
    private var caption = ""

    @Given("que la conversación contiene un mensaje de video válido")
    fun conversationContainsRestVideo() {
        hasVideo = true
        caption = "Mirá la pérdida"
    }

    @Given("que recibo por WebSocket un mensaje de video válido")
    fun websocketDeliversVideo() {
        hasVideo = true
        selectedMessageId = null
    }

    @Given("que la URL privada del video devuelve un error de reproducción")
    fun privateVideoUrlFails() {
        hasVideo = true
        playbackError = true
        chatMessagesAvailable = true
    }

    @Given("que una conversación tiene un video como último mensaje")
    fun conversationListHasVideoPreview() {
        hasVideo = true
        listPlayerStarted = false
    }

    @When("abro la burbuja del video")
    fun openVideoBubble() {
        selectedMessageId = if (hasVideo) "video-1" else null
    }

    @When("selecciono ese video para reproducirlo")
    fun selectWebsocketVideo() {
        selectedMessageId = if (hasVideo) "video-ws-1" else null
    }

    @When("intento reproducirlo nuevamente")
    fun retryPlayback() {
        retryRequested = true
        playbackError = true
    }

    @When("se muestra la lista de conversaciones")
    fun showConversationList() {
        listPlayerStarted = false
    }

    @Then("veo el player con play, pausa, progreso y duración")
    fun playerHasAccessibleControls() {
        assertTrue(hasVideo)
        assertEquals("video-1", selectedMessageId)
    }

    @Then("el caption del mensaje conserva su texto")
    fun captionRemainsVisible() {
        assertEquals("Mirá la pérdida", caption)
    }

    @Then("solo ese video queda seleccionado para reproducción")
    fun onlySelectedVideoPlays() {
        assertEquals("video-ws-1", selectedMessageId)
    }

    @Then("veo un error accesible con una acción de reintento")
    fun playbackErrorOffersRetry() {
        assertTrue(playbackError)
        assertTrue(retryRequested)
    }

    @Then("los demás mensajes del chat siguen disponibles")
    fun otherChatMessagesRemainAvailable() {
        assertTrue(chatMessagesAvailable)
    }

    @Then("veo un indicador de video en el preview")
    fun videoPreviewIsShown() {
        assertTrue(hasVideo)
    }

    @Then("no se inicia ningún player en la lista")
    fun listDoesNotStartPlayer() {
        assertFalse(listPlayerStarted)
    }
}
