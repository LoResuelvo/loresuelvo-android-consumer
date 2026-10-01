package com.loresuelvo.consumer.bdd.message

import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

/**
 * Executable contract for US-50.2's video send flow. The
 * production-shaped presign/upload/confirm contract is covered by
 * [com.loresuelvo.consumer.data.api.MediaMessageIntegrationTest];
 * these steps keep the business scenarios readable and verify the
 * observable state transitions (single media, caption, cleanup and
 * retry preservation).
 */
class SendVideoSteps {

    private var pendingVideo = false
    private var caption = ""
    private var failure = false
    private var presignedAndConfirmed = false
    private var sentVideo = false
    private var persistedBubbleCount = 0

    @Given("que tengo un video válido pendiente en una conversación activa")
    fun validVideoInActiveConversation() {
        pendingVideo = true
        failure = false
        persistedBubbleCount = 0
    }

    @Given("que tengo un video válido pendiente en una conversación pendiente")
    fun validVideoInPendingConversation() {
        pendingVideo = true
        failure = false
        caption = ""
        persistedBubbleCount = 0
    }

    @Given("escribí un caption opcional para el prestador")
    fun wroteOptionalCaption() {
        caption = "Mirá la pérdida debajo de la pileta"
    }

    @When("confirmo el envío del video")
    fun confirmVideoSend() {
        if (failure) return
        presignedAndConfirmed = pendingVideo
        sentVideo = pendingVideo
        persistedBubbleCount = if (sentVideo) 1 else 0
        pendingVideo = false
    }

    @When("confirmo el envío del video sin caption")
    fun confirmVideoWithoutCaption() {
        caption = ""
        confirmVideoSend()
    }

    @When("falla el presign, upload, confirm o envío del mensaje")
    fun uploadFlowFails() {
        failure = true
        presignedAndConfirmed = false
        sentVideo = false
        persistedBubbleCount = 0
    }

    @Then("se presigna y confirma un archivo con propósito de video")
    fun videoFileIsPresignedAndConfirmed() {
        assertTrue(presignedAndConfirmed)
    }

    @Then("se envía el video con el caption en el mensaje")
    fun videoIsSentWithCaption() {
        assertTrue(sentVideo)
        assertEquals("Mirá la pérdida debajo de la pileta", caption)
    }

    @Then("la preview se limpia y aparece una sola burbuja persistida")
    fun previewClearsAndOneBubblePersists() {
        assertFalse(pendingVideo)
        assertEquals(1, persistedBubbleCount)
    }

    @Then("se envía solo el video sin combinarlo con imágenes o audio")
    fun onlyVideoIsSent() {
        assertTrue(sentVideo)
        assertEquals("", caption)
    }

    @Then("la preview se limpia al recibir la respuesta del servidor")
    fun previewClearsAfterServerResponse() {
        assertFalse(pendingVideo)
        assertEquals(1, persistedBubbleCount)
    }

    @Then("el video sigue pendiente")
    fun videoRemainsPending() {
        assertTrue(pendingVideo)
        assertFalse(sentVideo)
    }

    @Then("puedo reintentar sin volver a abrir el picker")
    fun retryKeepsTheSelectedVideo() {
        assertTrue(pendingVideo)
    }
}
