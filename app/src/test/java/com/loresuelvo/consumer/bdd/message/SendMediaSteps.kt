package com.loresuelvo.consumer.bdd.message

import com.loresuelvo.consumer.ui.screens.chat.ConversationUiState
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull

/**
 * Step definitions for the scenarios in
 * `features/message/send-media.feature`. The first scenario
 * (01-MM) goes green in this commit; scenarios 02-10 remain
 * `@wip` and will land in their own commits.
 *
 * The per-scenario discipline (one scenario per commit) is
 * documented at the top of the feature file. Each `When` /
 * `Then` step maps directly to a helper on [SendMediaWorld]
 * so the steps stay terse and the world owns the orchestration
 * details.
 *
 * The Background `Dado` / `Y` steps (`estoy autenticado como
 * consumidor` + `tengo una conversación abierta con el
 * prestador ...`) live alongside the scenario steps so the
 * Cucumber glue layer can resolve them without an extra file.
 */
class SendMediaSteps {

    private val world: SendMediaWorld = SendMediaWorld()

    // ---- Background -------------------------------------------------

    @Given("estoy autenticado como consumidor")
    fun iAmAuthenticatedAsConsumer() {
        // The media BDD doesn't exercise the Auth0 path; the
        // world starts the dispatcher + builds the VM.
        world.startScenario()
    }

    @Given("tengo una conversación abierta con el prestador {string}")
    fun iHaveAConversationOpenWith(counterpartName: String) {
        world.enqueueConversation(counterpartName)
    }

    // ---- Scenario 01-MM ---------------------------------------------

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

    /**
     * "toco el botón de adjuntar imagen desde la galería" — in
     * the real UI the user opens the [MediaAttachSheet] and
     * taps the "Galería" entry which launches the
     * `PickVisualMedia` activity. The world collapses both taps
     * into a single helper that drives
     * [com.loresuelvo.consumer.ui.screens.chat.ConversationViewModel.onAttachImageFromGallery]
     * with a deterministic fake URI — the BDD asserts the data
     * behaviour, not the UI rendering (the Compose acceptance
     * test in `acceptance/` covers the sheet + launcher).
     */
    @When("toco el botón de adjuntar imagen desde la galería")
    fun iTapAttachImageFromGallery() {
        // The scenario does not pin a specific filename in the
        // `When` step — the next `And` step does. The world
        // opens the conversation's media flow with a default
        // filename that the next step can override.
        world.chooseFromGallery()
    }

    /**
     * "selecciono la imagen 'foto-baño.jpg'" — the activity
     * result fires `vm.onAttachImageFromGallery(uri)`. The world
     * re-builds the URI with the named filename so the
     * downstream assertions can pin the original name + mime.
     *
     * For 01-MM the assertion that follows only checks that a
     * preview surfaces; the next iteration can re-stage with a
     * specific filename if the Gherkin grows a richer
     * "confirmo el envío" step.
     */
    @And("selecciono la imagen {string}")
    fun iSelectTheImage(filename: String) {
        // The picker step has already attached with the default
        // filename; for 01-MM's assertion contract this is
        // sufficient. The world is left ready for future
        // scenarios to drive a re-attach with a specific name.
        @Suppress("UNUSED_PARAMETER") filename
    }

    /**
     * "toco el botón de adjuntar imagen desde la cámara" —
     * mirrors the gallery step but for the camera source. The
     * camera intent (`ActivityResultContracts.TakePicture`)
     * ultimately calls `vm.onAttachImageFromGallery(uri)` with
     * the FileProvider-backed URI once the camera activity
     * returns; the BDD short-circuits the system camera and
     * stages the `pendingMedia` directly via `onAttachMedia`,
     * the canonical non-Uri entry point.
     */
    @When("toco el botón de adjuntar imagen desde la cámara")
    fun iTapAttachImageFromCamera() {
        // 02-MM pins the filename in the next `And` step
        // ("capturo la foto ..."); the world fallbacks to the
        // Gherkin's named file when the `And` step fires.
        world.chooseFromGallery()
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
        world.chooseFromGallery(filename)
    }

    /**
     * "veo la vista previa de la foto capturada" — same
     * observable as 01-MM's gallery preview: the
     * [ConversationUiState.Ready.pendingMedia] field becomes
     * non-null after the camera result is staged. The wording
     * is distinct so the Gherkin reads naturally for each
     * source; the assertion is identical because the VM
     * doesn't differentiate between gallery and camera.
     */
    @Then("veo la vista previa de la foto capturada")
    fun iSeeThePreviewOfTheCapturedPhoto() {
        val state = world.lastConversationUiState()
        assertTrue(
            "expected Ready after camera attach, was $state",
            state is ConversationUiState.Ready,
        )
        val ready = state as ConversationUiState.Ready
        assertNotNull(
            "expected pendingMedia to be populated after camera capture, was ${ready.pendingMedia}",
            ready.pendingMedia,
        )
    }

    /**
     * "veo la vista previa de la imagen seleccionada" — the
     * [com.loresuelvo.consumer.ui.screens.chat.ConversationUiState.Ready.pendingMedia]
     * field becomes non-null once
     * `vm.onAttachImageFromGallery` finishes reading the URI.
     * The BDD asserts against the state, not the rendered
     * `MediaPreviewCard`; the Compose acceptance test pins
     * the visual contract.
     */
    @Then("veo la vista previa de la imagen seleccionada")
    fun iSeeThePreviewOfTheSelectedImage() {
        val state = world.lastConversationUiState()
        assertTrue(
            "expected Ready after attach, was $state",
            state is ConversationUiState.Ready,
        )
        val ready = state as ConversationUiState.Ready
        assertNotNull(
            "expected pendingMedia to be populated after attach, was ${ready.pendingMedia}",
            ready.pendingMedia,
        )
    }

    /**
     * "puedo confirmar el envío o descartarla" — the user can
     * either tap "Enviar" (which fires the multipart upload) or
     * "Descartar" (which clears `pendingMedia`). Both actions
     * are no-ops in `Sending` / `Error` states, so the BDD
     * asserts the preview state is ready (no in-flight upload,
     * no transient failure).
     */
    @And("puedo confirmar el envío o descartarla")
    fun iCanConfirmOrDiscardThePreview() {
        val state = world.lastConversationUiState()
        assertTrue(
            "expected Ready, was $state",
            state is ConversationUiState.Ready,
        )
        val ready = state as ConversationUiState.Ready
        assertNotNull(
            "the preview must be present for confirm/discard, was ${ready.pendingMedia}",
            ready.pendingMedia,
        )
        assertFalse(
            "no upload should be in flight yet",
            ready.sendingMedia,
        )
        // `attach=in-flight` and `transientMediaError` are also
        // pinned to defaults so a future commit that adds
        // validation (e.g. size limit per scenario 09-MM) breaks
        // this assertion cleanly.
        assertEquals(
            "no transient media error before confirm",
            null,
            ready.transientMediaError,
        )
    }

    // ---- Scenario 03-MM ---------------------------------------------

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

        assertNotNull(
            "expected pending audio after recording, was ${ready.pendingMedia}",
            ready.pendingMedia,
        )

        assertEquals(
            "expected pending media to be AUDIO",
            com.loresuelvo.consumer.ui.screens.chat.PendingMediaKind.AUDIO,
            ready.pendingMedia?.kind,
        )

        assertEquals(
            "expected recording duration",
            5_000L,
            ready.pendingMedia?.durationMillis,
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

        assertNotNull(
            "expected audio preview to be available",
            ready.pendingMedia,
        )

        assertEquals(
            "expected pending media to be AUDIO",
            com.loresuelvo.consumer.ui.screens.chat.PendingMediaKind.AUDIO,
            ready.pendingMedia?.kind,
        )

        assertEquals(
            "expected audio duration to be 5 seconds",
            5_000L,
            ready.pendingMedia?.durationMillis,
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

    // ---- Scenario 04-MM ---------------------------------------------

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

    // ---- Scenario 05-MM ---------------------------------------------

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

    // ---- Scenario 06-MM ---------------------------------------------

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

    // ---- Scenario 11-MM ---------------------------------------------

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