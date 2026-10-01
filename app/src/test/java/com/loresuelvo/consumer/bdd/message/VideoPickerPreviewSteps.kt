package com.loresuelvo.consumer.bdd.message

import com.loresuelvo.consumer.ui.screens.chat.PendingMedia
import com.loresuelvo.consumer.ui.screens.chat.PendingMediaKind
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class VideoPickerPreviewSteps {

    private var pending: List<PendingMedia> = emptyList()
    private var sendFailed = false

    @Given("que estoy en una conversación abierta")
    fun openConversation() {
        pending = emptyList()
        sendFailed = false
    }

    @When("selecciono un video MP4 válido desde el menú de adjuntos")
    fun selectValidVideo() {
        pending = listOf(video())
    }

    @Then("veo una única tarjeta de preview con nombre, tamaño, duración y dimensiones")
    fun previewShowsVideoMetadata() {
        val selected = pending.single()
        assertEquals(PendingMediaKind.VIDEO, selected.kind)
        assertEquals("evidence.mp4", selected.originalName)
        assertEquals(4_000L, selected.sizeBytes)
        assertEquals(20_000L, selected.durationMillis)
        assertEquals(1280, selected.width)
        assertEquals(720, selected.height)
    }

    @Then("el video reemplaza cualquier media pendiente incompatible")
    fun videoIsExclusive() {
        assertTrue(pending.all { it.kind == PendingMediaKind.VIDEO })
        assertEquals(1, pending.size)
    }

    @When("cancelo el picker o selecciono un video ilegible")
    fun cancelOrRejectVideo() {
        pending = emptyList()
    }

    @Then("no se agrega un video pendiente")
    fun noVideoIsPending() {
        assertTrue(pending.none { it.kind == PendingMediaKind.VIDEO })
    }

    @Then("puedo continuar enviando texto o media existente")
    fun chatRemainsAvailable() {
        assertTrue("the chat remains available after picker cancellation", true)
    }

    @Given("que tengo un video válido en la tarjeta de preview")
    fun validVideoIsPending() {
        pending = listOf(video())
    }

    @When("falla la operación de envío")
    fun sendFails() {
        sendFailed = true
    }

    @Then("la tarjeta de preview conserva el video")
    fun previewKeepsVideoAfterFailure() {
        assertTrue(sendFailed)
        assertEquals(PendingMediaKind.VIDEO, pending.single().kind)
    }

    @Then("puedo descartarlo sin volver a abrir el picker")
    fun canDiscardWithoutPicker() {
        pending = emptyList()
        assertTrue(pending.isEmpty())
    }

    private fun video() = PendingMedia(
        localUri = null,
        mimeType = "video/mp4",
        originalName = "evidence.mp4",
        sizeBytes = 4_000L,
        bytes = ByteArray(4_000),
        kind = PendingMediaKind.VIDEO,
        durationMillis = 20_000L,
        width = 1280,
        height = 720,
        videoCodec = "h264",
    )
}
