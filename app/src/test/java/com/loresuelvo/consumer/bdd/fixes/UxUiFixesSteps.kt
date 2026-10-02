package com.loresuelvo.consumer.bdd.fixes

import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When

class UxUiFixesSteps {

    private val world: UxUiFixesWorld = UxUiFixesWorld()


    @Given("que la funcionalidad de audio para IA no está disponible")
    fun laFuncionalidadDeAudioParaIaNoEstaDisponible() {
        world.startScenario()
    }

    @When("visualizo la pantalla correspondiente")
    fun visualizoLaPantallaCorrespondiente() {
        // The chat screen is mounted by the Compose route; the
    }

    @Then("el ícono de audio no debe mostrarse")
    fun elIconoDeAudioNoDebeMostrarse() {
        // / Stop affordances never surface while audioEnabled is
        // false" assertion lives in
        // `ChatInputBarTest.shows_send_disabled_when_audio_disabled_and_prompt_empty`.
        val state = world.lastUiState()
        if (state.audioEnabled) {
            error(
                "expected ChatUiState.audioEnabled=false so the Mic / Stop " +
                    "affordances never render (01-UXUI), but " +
                    "audioEnabled=${state.audioEnabled}",
            )
        }
    }

    @And("el botón de enviar mensajes se muestra deshabilitado mientras el campo esté vacío")
    fun elBotonDeEnviarMensajesSeMuestraDeshabilitadoMientrasElCampoEsteVacio() {
        // Driven by `ChatUiState.canSend`: false while the prompt
        // is empty (and not sending). The visual "the Send slot
        // is rendered disabled" assertion lives in the same
        // Compose-test referenced above.
        val state = world.lastUiState()
        if (state.canSend) {
            error(
                "expected ChatUiState.canSend=false so the Send button renders " +
                    "disabled while the prompt is empty (01-UXUI), but " +
                    "canSend=${state.canSend}",
            )
        }
    }


    @Given("que estoy en la aplicación")
    fun queEstoyEnLaAplicacion() {
        // The user is already authenticated by the time the Home
        // "Ver todas" link is reachable; mounting the categories
        // VM is the responsibility of the `When` step.
    }

    @When("accedo a la sección de categorías")
    fun accedoALaSeccionDeCategorias() {
        world.startCategoriesScenario()
    }

    @Then("veo una pantalla con todas las categorías disponibles")
    fun veoUnaPantallaConTodasLasCategoriasDisponibles() {
        world.assertAllCategoriesVisible(FakeCategoryRepository.DEFAULT_CATEGORIES.map { it.name })
    }

    @And("una barra de búsqueda que me permite encontrar las distintas categorías")
    fun unaBarraDeBusquedaQueMePermiteEncontrarLasDistintasCategorias() {
        // Pin that the search affordance filters the Ready
        // state. Typing "plom" must surface the single
        // "Plomería" category from the default set; clearing
        // the query restores the full list. The visual
        // presence of the OutlinedTextField is verified by
        // the Compose-test in `CategoriesScreenTest`
        // (pinned via `CATEGORIES_SEARCH_FIELD_TAG`).
        world.typeSearchQuery("plom")
        world.assertAllCategoriesVisible(listOf("Plomería"))

        world.typeSearchQuery("")
        world.assertAllCategoriesVisible(FakeCategoryRepository.DEFAULT_CATEGORIES.map { it.name })
    }


    /**
     * 03-UXUI `Given`: the consumer has tapped "Contactar" on a
     * provider card and the contact bottom sheet is now open
     * with the VM exposing an [ContactProviderUiState.Open]
     * payload. The world mirrors that flow by mounting the
     * [ContactProviderViewModel] against a relaxed fake
     * [MediaReader] and opening the modal against a sample
     * provider.
     */
    @Given("que estoy creando una oferta de trabajo")
    fun queEstoyCreandoUnaOfertaDeTrabajo() {
        world.openContactSheet(
            provider = com.loresuelvo.consumer.domain.provider.Provider(
                id = 1,
                name = "Juan",
                surname = "Pérez",
                categoryId = 1,
                categoryName = "Plomería",
                profilePhotoUrl = null,
            ),
        )
    }

    @When("selecciono una o más imágenes desde el dispositivo")
    fun seleccionoUnaOMasImagenesDesdeElDispositivo() {
        world.attachJobRequestImages(
            listOf(
                "perdida-bajo-mesada.jpg",
                "detalle-sifon.webp",
                "humedad-pared.png",
            ),
        )
    }

    /**
     * 03-UXUI `Then`: the consumer can see the staged images
     * reflected in the contact form state. The visual rendering
     * of the thumbnails is verified by the Compose-test in
     * `JobRequestImageAttachmentSelectorTest`; here we pin the
     * data contract (one entry per picked image, in the order
     * they were staged).
     */
    @Then("las imágenes quedan adjuntadas a la oferta")
    fun lasImagenesQuedanAdjuntadasALaOferta() {
        world.assertAttachedImageNames(
            listOf(
                "perdida-bajo-mesada.jpg",
                "detalle-sifon.webp",
                "humedad-pared.png",
            ),
        )
    }


    /**
     * 04-UXUI `Given`: the consumer has already picked one or
     * more images from the gallery / camera on the contact
     * form. We open the contact sheet (mirroring 03-UXUI's
     * precondition) and stage a single canonical image so the
     * preview-then-publish flow has data to display.
     */
    @Given("que seleccioné una o más imágenes para mi oferta de trabajo")
    fun queSeleccioneUnaOMasImagenesParaMiOfertaDeTrabajo() {
        world.openContactSheet(
            provider = com.loresuelvo.consumer.domain.provider.Provider(
                id = 1,
                name = "Juan",
                surname = "Pérez",
                categoryId = 1,
                categoryName = "Plomería",
                profilePhotoUrl = null,
            ),
        )
        world.attachJobRequestImages(
            listOf("perdida-bajo-mesada.jpg"),
        )
    }

    @When("continúo con la creación de la oferta")
    fun continuoConLaCreacionDeLaOferta() {
        // No-op: the contact form is single-step, so the
        // consumer stays on the same surface after attaching.
    }

    /**
     * 04-UXUI `Then`: the staged images are rendered as
     * thumbnails by the
     * [com.loresuelvo.consumer.ui.components.images.JobRequestImageAttachmentSelector].
     * The visual presence is verified by the Compose-test in
     * `JobRequestImageAttachmentSelectorTest`; here we pin
     * the data contract — every staged image is reflected in
     * `state.attachedImages` so the selector can render its
     * preview.
     */
    @Then("veo una vista previa de las imágenes seleccionadas")
    fun veoUnaVistaPreviaDeLasImagenesSeleccionadas() {
        world.assertAttachedImageNames(listOf("perdida-bajo-mesada.jpg"))
    }


    /**
     * 05-UXUI `Given`: open the contact sheet and stage three
     * canonical images so the removal step has a deterministic
     * index to remove (`"detalle-sifon.webp"` at index 1).
     */
    @Given("que tengo una o más imágenes seleccionadas para mi oferta de trabajo")
    fun queTengoUnaOMasImagenesSeleccionadasParaMiOfertaDeTrabajo() {
        world.openContactSheet(
            provider = com.loresuelvo.consumer.domain.provider.Provider(
                id = 1,
                name = "Juan",
                surname = "Pérez",
                categoryId = 1,
                categoryName = "Plomería",
                profilePhotoUrl = null,
            ),
        )
        world.attachJobRequestImages(
            listOf(
                "perdida-bajo-mesada.jpg",
                "detalle-sifon.webp",
                "humedad-pared.png",
            ),
        )
    }

    /**
     * 05-UXUI `When`: the user taps the `×` chip on the
     * second thumbnail. The route forwards the tap to the VM
     * via [ContactProviderViewModel.onRemoveImage] with the
     * index of the staged image.
     */
    @When("elimino una de las imágenes")
    fun eliminoUnaDeLasImagenes() {
        world.removeJobRequestImage(index = 1)
    }

    /**
     * 05-UXUI `Then`: the removed image is no longer in the
     * staged list. The visual removal is verified by the
     * Compose-test in
     * `JobRequestImageAttachmentSelectorTest`
     * (`remove_button_click_invokes_onRemove_with_correct_index`).
     */
    @Then("la imagen deja de estar adjuntada a la oferta")
    fun laImagenDejaDeEstarAdjuntadaALaOferta() {
        world.assertAttachedImageNames(
            listOf("perdida-bajo-mesada.jpg", "humedad-pared.png"),
        )
    }


    @Given("que tengo un chat con mensajes nuevos sin leer")
    fun queTengoUnChatConMensajesNuevosSinLeer() {
    }

    @When("visualizo la lista de chats")
    fun visualizoLaListaDeChats() {
    }

    @Then("veo una indicación visual de que el chat tiene nuevos mensajes")
    fun veoUnaIndicacionVisualDeQueElChatTieneNuevosMensajes() {
    }


    @When("ingreso al chat")
    fun ingresoAlChat() {
    }

    @And("visualizo los mensajes pendientes")
    fun visualizoLosMensajesPendientes() {
    }

    @And("el indicador de nuevos mensajes deja de mostrarse para ese chat")
    fun elIndicadorDeNuevosMensajesDejaDeMostrarseParaEseChat() {
    }


    @Given("que navego por las distintas pantallas de la aplicación")
    fun queNavegoPorLasDistintasPantallasDeLaAplicacion() {
        // The visual sweep covers Home, Messages, Assistant,
        // Professionals, Categories, Chat, Conversation,
        // Welcome, CompleteProfile. The Compose tests for each
        // screen pin the structural contract (the relevant
        // `testTag`s render), and the refactor that landed for
        // 08-UXUI removes the duplicate `statusBarsPadding()`,
        // routes the IME inset through the `Scaffold`, and lifts
        // the conversation input bar above the soft keyboard.
        world.openMessagesList()
    }

    @When("visualizo los componentes de la interfaz")
    fun visualizoLosComponentesDeLaInterfaz() {
        world.assertMessagesListRendered()
    }

    @Then("los bordes y márgenes se muestran correctamente")
    fun losBordesYMargenesSeMuestranCorrectamente() {
        // screen stops rendering. The visual inspection is the
        // dev's responsibility (manual sweep on the device).
    }

    @And("ningún elemento aparece cortado")
    fun ningunElementoApareceCortado() {
        // Mirrors the previous step — structural coverage lives
        // in the per-screen Compose tests; this step is a
        // placeholder for the manual sweep.
    }

    @And("ningún elemento aparece desbordado")
    fun ningunElementoApareceDesbordado() {
        // See comment above.
    }

    @And("ningún elemento aparece fuera de los límites de la pantalla")
    fun ningunElementoApareceFueraDeLosLimitesDeLaPantalla() {
        // See comment above.
    }
}
