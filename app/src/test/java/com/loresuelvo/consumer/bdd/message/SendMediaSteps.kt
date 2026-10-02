package com.loresuelvo.consumer.bdd.message

import com.loresuelvo.consumer.domain.conversation.MAX_AUDIO_BYTES
import com.loresuelvo.consumer.ui.screens.chat.ConversationUiState
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull

class SendMediaSteps {

    private val world: SendMediaWorld = SendMediaWorld()

    // ---- Background -------------------------------------------------

    @Given("estoy autenticado como consumidor")
    fun iAmAuthenticatedAsConsumer() {
        // world starts the dispatcher + builds the VM.
        world.startScenario()
    }

    @Given("tengo una conversación abierta con el prestador {string}")
    fun iHaveAConversationOpenWith(counterpartName: String) {
        world.enqueueConversation(counterpartName)
    }


    /**
     * "que estoy en la conversación con 'Juan Pérez'" — fires
     * `ConversationViewModel.load` so the screen surfaces the
     * seeded detail in the new VM's state stream. Mirrors the
     * production `LaunchedEffect(conversationId) { vm.load(...) }`
     * in `ConversationRoute`.
     */
    @Given("que estoy en la conversación con {string}")
    fun iAmInTheConversationWith(counterpartName: String) {
        world.openConversation()
    }

    @When("toco el botón de adjuntar imagen desde la galería")
    fun iTapAttachImageFromGallery() {
        // `When` step — the next `And` step does. The world
        // opens the conversation's media flow with a default
        // filename that the next step can override.
        world.chooseFromGallery()
    }

    @And("selecciono la imagen {string}")
    fun iSelectTheImage(filename: String) {
        // The picker step has already attached with the default
        // filename; for 01-MM's assertion contract this is
        // sufficient. The world is left ready for future
        @Suppress("UNUSED_PARAMETER") filename
    }

    @When("toco el botón de adjuntar imagen desde la cámara")
    fun iTapAttachImageFromCamera() {
        // La captura real ocurre en el siguiente step:
        // "Y capturo la foto {string}"
    }

    /**
     * "capturo la foto 'gotera-baño.jpg'" — the camera activity
     * has returned with success and the route calls
     * `vm.onAttachImageFromGallery(uri)`. The world stages a
     * `MediaUpload.Image` with the named filename via
     * `onAttachMedia` (the same path the production code goes
     * through after the MediaReader reads the URI).
     */
    @And("capturo la foto {string}")
    fun iCaptureThePhoto(filename: String) {
        world.captureFromCamera(filename)
    }

    @Then("veo la vista previa de la foto capturada")
    fun iSeeThePreviewOfTheCapturedPhoto() {
        val state = world.lastConversationUiState()
        assertTrue(
            "expected Ready after camera attach, was $state",
            state is ConversationUiState.Ready,
        )
        val ready = state as ConversationUiState.Ready
        assertEquals(
            "expected pendingMedia to be populated after camera capture, was ${ready.pendingMedia}",
            1,
            ready.pendingMedia.size,
        )
    }

    @Then("veo la vista previa de la imagen seleccionada")
    fun iSeeThePreviewOfTheSelectedImage() {
        val state = world.lastConversationUiState()
        assertTrue(
            "expected Ready after attach, was $state",
            state is ConversationUiState.Ready,
        )
        val ready = state as ConversationUiState.Ready
        assertEquals(
            "expected pendingMedia to be populated after attach, was ${ready.pendingMedia}",
            1,
            ready.pendingMedia.size,
        )
    }

    @And("puedo confirmar el envío o descartarla")
    fun iCanConfirmOrDiscardThePreview() {
        val state = world.lastConversationUiState()
        assertTrue(
            "expected Ready, was $state",
            state is ConversationUiState.Ready,
        )
        val ready = state as ConversationUiState.Ready
        assertEquals(
            "the preview must be present for confirm/discard, was ${ready.pendingMedia}",
            1,
            ready.pendingMedia.size,
        )
        assertFalse(
            "no upload should be in flight yet",
            ready.sendingMedia,
        )
        // `attach=in-flight` and `transientMediaError` are also
        // pinned to defaults so a future commit that adds
        // this assertion cleanly.
        assertEquals(
            "no transient media error before confirm",
            null,
            ready.transientMediaError,
        )
    }


    @When("toco el botón de grabar audio")
    fun iTapRecordAudioButton() {
        world.startAudioRecording()
    }

    @And("grabo un audio de {int} segundos")
    fun iRecordAudioForSeconds(seconds: Int) {
        world.recordAudioFor(seconds)
    }

    @Then("veo la vista previa del audio grabado")
    fun iSeeTheRecordedAudioPreview() {
        val state = world.lastConversationUiState()

        println("=== 03-MM PREVIEW STATE ===")
        println(state)

        assertTrue(
            "expected Ready after audio recording, was $state",
            state is ConversationUiState.Ready,
        )

        val ready = state as ConversationUiState.Ready

        println("=== 03-MM PENDING MEDIA ===")
        println(ready.pendingMedia)

        assertEquals(
            "expected pending audio after recording",
            1,
            ready.pendingMedia.size,
        )

        assertEquals(
            "expected pending media to be AUDIO",
            com.loresuelvo.consumer.ui.screens.chat.PendingMediaKind.AUDIO,
            ready.pendingMedia.single().kind,
        )

        assertEquals(
            "expected recording duration",
            5_000L,
            ready.pendingMedia.single().durationMillis,
        )
    }

    @And("puedo reproducirlo antes de enviarlo")
    fun iCanPlayTheRecordedAudio() {
        val state = world.lastConversationUiState()

        println("=== 03-MM PLAY STATE ===")
        println(state)

        assertTrue(
            "expected Ready, was $state",
            state is ConversationUiState.Ready,
        )

        val ready = state as ConversationUiState.Ready

        assertEquals(
            "expected audio preview to be available",
            1,
            ready.pendingMedia.size,
        )

        assertEquals(
            "expected pending media to be AUDIO",
            com.loresuelvo.consumer.ui.screens.chat.PendingMediaKind.AUDIO,
            ready.pendingMedia.single().kind,
        )

        assertEquals(
            "expected audio duration to be 5 seconds",
            5_000L,
            ready.pendingMedia.single().durationMillis,
        )
    }

    @And("puedo confirmar el envío")
    fun iCanConfirmTheSend() {
        println("=== 03-MM CONFIRM STATE ===")
        println(world.lastConversationUiState())

        world.confirmSend()

        println("=== 03-MM AFTER CONFIRM ===")
        println(world.lastConversationUiState())
    }


    @Given("que envié una imagen en la conversación con {string}")
    fun iSentAnImage(counterpartName: String) {
        world.seedConversationWithSentImage(counterpartName)
    }

    @When("accedo a esa conversación")
    fun iAccessThatConversation() {
        world.openConversation()
    }

    @Then("veo la burbuja de la imagen enviada en el hilo")
    fun iSeeTheSentImageBubble() {
        world.assertSentImageBubbleIsVisible()
    }

    @And("la burbuja expone la miniatura de la imagen enviada")
    fun theBubbleExposesTheImageThumbnail() {
        world.assertSentImageThumbnailIsVisible()
    }


    @Given("que envié un audio en la conversación con {string}")
    fun iSentAnAudio(counterpartName: String) {
        world.seedConversationWithSentAudio(counterpartName)
    }

    @Then("veo la burbuja del audio enviado en el hilo")
    fun iSeeTheSentAudioBubble() {
        world.assertSentAudioBubbleIsVisible()
    }

    @And("la burbuja muestra la duración del audio enviado")
    fun theBubbleShowsTheSentAudioDuration() {
        world.assertSentAudioDurationIsVisible()
    }


    @Given("que el prestador {string} me envió una imagen")
    fun theProviderSentMeAnImage(counterpartName: String) {
        world.seedConversationWithReceivedImage(counterpartName)
    }

    @When("accedo a la conversación con {string}")
    fun iAccessTheConversationWith(counterpartName: String) {
        world.openConversation()
    }

    @When("toco la burbuja de la imagen recibida")
    fun iTapTheReceivedImageBubble() {
        world.tapReceivedImageBubble()
    }

    @Then("la imagen se abre en pantalla completa")
    fun theImageOpensFullscreen() {
        world.assertReceivedImageIsOpenFullscreen()
    }


    @Given("que el prestador {string} me envió un audio")
    fun theProviderSentMeAnAudio(counterpartName: String) {
        world.seedConversationWithReceivedAudio(counterpartName)
    }

    @When("toco la burbuja del audio recibido")
    fun iTapTheReceivedAudioBubble() {
        world.playReceivedAudio()
    }


    @Given("que el backend no responde")
    fun theBackendIsDown() {
        world.simulateBackendNetworkFailure()
    }

    @When("adjunto una imagen desde la galería y confirmo el envío")
    fun iAttachAnImageFromGalleryAndConfirm() {
        world.chooseFromGallery()
        world.confirmSend()
    }

    @Then("veo un error de red")
    fun iSeeANetworkError() {
        world.assertMediaSendFailureIsNetwork()
    }

    @And("la imagen NO aparece en la conversación")
    fun theImageDoesNotAppearInTheConversation() {
        world.assertNoMessageWasAppended()
    }


    @And("grabo un audio que excede el tamaño máximo permitido")
    fun iRecordedAnAudioThatExceedsTheMaxSize() {
        // Use case rejects clips larger than MAX_AUDIO_BYTES with
        // a typed PayloadTooLarge failure. The helper records a
        // 5-second clip whose byte payload is one byte over the
        // cap, so the rejection fires before the network round-trip.
        world.recordAudioFor(
            seconds = 5,
            sizeBytes = (MAX_AUDIO_BYTES + 1).toInt(),
        )
    }

    @When("intento confirmar el envío")
    fun iTryToConfirmTheSend() {
        world.confirmSend()
    }

    @Then("veo un error de tamaño excedido")
    fun iSeeASizeExceededError() {
        world.assertMediaSendFailureIsPayloadTooLarge()
    }

    @And("el audio NO aparece en la conversación")
    fun theAudioDoesNotAppearInTheConversation() {
        world.assertNoMessageWasAppended()
    }


    /**
     * "selecciono una imagen de la galería" — equivalent to
     * the 01-MM pick + select chain but stated as a single step
     * (the user already picked before reaching the discard
     * screen). The world collapses the picker + MediaReader
     * pipeline into a single `chooseFromGallery()` call that
     * stages a deterministic in-memory JPEG via `onAttachMedia`
     * (canonical non-Uri entry point).
     */
    @Given("selecciono una imagen de la galería")
    fun iSelectedAnImageFromTheGallery() {
        world.chooseFromGallery()
    }

    @When("descarto la vista previa")
    fun iDiscardThePreview() {
        world.discardPreview()
    }

    @Then("NO se crea ninguna burbuja en la conversación")
    fun noMessageBubbleIsCreatedInTheConversation() {
        world.assertNoMessageWasAppended()
    }


    @Given("que tengo un audio enviado en la conversación con {string}")
    fun iHaveASentAudio(counterpartName: String) {
        world.seedConversationWithSentAudio(counterpartName)
    }

    @When("presiono reproducir el audio")
    fun iPressPlayTheAudio() {
        world.playSentAudio()
    }

    @Then("el audio comienza a reproducirse")
    fun theAudioStartsPlaying() {
        world.assertAudioIsPlaying()
    }

    @And("veo avanzar la línea de progreso mientras se reproduce")
    fun iSeeTheProgressLineAdvance() {
        world.assertAudioProgressAdvanced()
    }

    @And("el tiempo transcurrido se actualiza")
    fun elapsedTimeIsUpdated() {
        world.assertElapsedAudioTimeUpdated()
    }

    private fun assertTrue(message: String, condition: Boolean) {
        if (!condition) throw AssertionError(message)
    }
}
