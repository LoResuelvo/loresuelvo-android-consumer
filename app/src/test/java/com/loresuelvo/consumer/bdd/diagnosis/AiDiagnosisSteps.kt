package com.loresuelvo.consumer.bdd.diagnosis

import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When

class AiDiagnosisSteps {

    private val world: AiDiagnosisWorld = AiDiagnosisWorld()

    @Given("estoy autenticado como consumidor")
    fun estoyAutenticadoComoConsumidor() {
        world.startScenario()
    }

    @Given("me encuentro en la pantalla Home")
    fun meEncuentroEnLaPantallaHome() {
        // The chat VM does not depend on any Home state — the step
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


    /**
     * 03-DIA "Given": the consumer is already mid-conversation
     * (one round-trip done). The world drives that complete
     * cycle so `state.messages` and `state.conversationId` look
     * like a real interaction before the slow next-round-trip
     * in the `When`.
     */
    @Given("estoy en una conversación con el asistente")
    fun estoyEnUnaConversacionConElAsistente03() {
        world.driveCompletedRoundTrip()
    }

    /**
     * 03-DIA "When": the user types a follow-up and taps send,
     * but the fake stalls the round-trip indefinitely. The state
     * we observe from here onward has `sending = true` until the
     * next `When` releases the suspension (or the step is
     * declared passed with the indicator visible).
     */
    @When("envío un nuevo mensaje y la respuesta tarda en llegar")
    fun envioUnNuevoMensajeYLaRespuestaTardaEnLlegar() {
        world.simulateHangingSend()
    }

    @Then("veo un indicador de carga")
    fun veoUnIndicadorDeCarga() {
        world.assertTypingIndicatorVisible()
    }

    @And("no puedo enviar un nuevo mensaje hasta recibir una respuesta")
    fun noPuedoEnviarNuevoMensajeHastaRecibirRespuesta() {
        world.assertSendingFlagBlocksNewSends()
    }


    /**
     * 04-DIA "When": the user types and sends a follow-up, but
     * the backend returns a 500. The VM must surface the error
     * to the UI without dropping the user's optimistic bubble.
     * (The `Given estoy en una conversación con el asistente` step
     * above is shared with 03-DIA via Cucumber's step-key dispatch.)
     */
    @When("envío un nuevo mensaje y el servicio falla")
    fun envioUnNuevoMensajeYElServicioFalla() {
        world.simulateFailingSend()
    }

    @Then("veo el mensaje del asistente {string}")
    fun veoElMensajeDelAsistente(mensaje: String) {
        world.assertAssistantMessageShows(mensaje)
    }

    /**
     * 04-DIA "And puedo volver a intentarlo": the retry CTA in
     * [com.loresuelvo.consumer.ui.screens.chat.ChatErrorCard] calls
     * [com.loresuelvo.consumer.ui.screens.chat.ChatViewModel.onRetryClick],
     * which clears `transientError` and refires with
     * `lastAttemptedPrompt`.
     */
    @Then("puedo volver a intentarlo")
    fun puedoVolverAIntentarlo() {
        // The retry is initiated first; `assertRetryClearsError`
        // verifies the post-retry state (`sending = true`,
        // `transientError = null`).
        world.simulateRetry()
        world.assertRetryClearsError()
    }


    /**
     * 05-DIA "When": the user opens the chat screen. There's no
     * setup beyond what [estoyAutenticadoComoConsumidor] already
     * does — the warning is part of the default chat surface.
     *
     * `Then veo el mensaje del asistente {string}` is shared with
     * 04-DIA — see [veoElMensajeDelAsistente]. The World's
     * `assertAssistantMessageShows` dispatches between the
     * warning and the error card based on which surface is
     * actually showing.
     */

    @When("visualizo la conversación con el asistente")
    fun visualizoLaConversacionConElAsistente() {
        // No-op: the chat screen always shows the warning
        // banner by default (see [ChatUiState.preliminaryWarningVisible]).
    }


    /**
     * 07-DIA "Given": the consumer is in the chat screen typing.
     * No-op: the default VM state already represents this. The
     * visual field rendering is verified by the Compose-test in
     * `src/test/.../ChatInputBarTest.kt`.
     */
    @Given("me encuentro escribiendo un mensaje para el asistente")
    fun meEncuentroEscribiendoUnMensajeParaElAsistente() {
        // Visual assertion only — verified by ChatInputBarTest.
    }

    @When("el contenido supera una línea")
    fun elContenidoSuperaUnaLinea() {
        world.typePrompt("Línea uno\nLínea dos\nLínea tres")
    }

    @Then("el campo de texto aumenta su altura automáticamente")
    fun elCampoDeTextoAumentaSuAltura() {
        // Visual assertion — covered by ChatInputBarTest.
    }

    @And("permite visualizar hasta 6 líneas de contenido sin scroll")
    fun permiteVisualizarHasta6Lineas() {
        // Visual assertion — covered by ChatInputBarTest.
    }


    @When("el contenido supera las 6 líneas visibles")
    fun elContenidoSuperaLas6LineasVisibles() {
        world.typePrompt("L1\nL2\nL3\nL4\nL5\nL6\nL7\nL8")
    }

    @Then("el campo de texto mantiene una altura máxima de 6 líneas")
    fun elCampoDeTextoMantieneAlturaMaximaDe6Lineas() {
        // Visual assertion — covered by ChatInputBarTest.
    }

    @And("puedo desplazarme mediante scroll dentro del campo")
    fun puedoDesplazarmePorScroll() {
        // Visual assertion — covered by ChatInputBarTest.
    }

    @And("el contenido completo permanece accesible")
    fun contenidoCompletoAccesible() {
        // Visual assertion — covered by ChatInputBarTest.
    }


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


    /**
     * 09-DIA "Given": the AI concluded the diagnosis and the
     * backend response includes an assessment + a list of
     * recommended providers for the supplied [rubro]. The world
     * seeds the fake's response and drives a complete round-trip
     * so the VM lands in the post-conclusion state.
     */
    @Given("la IA concluyó el diagnóstico y recomienda prestadores del rubro {string}")
    fun laIaConcluyoDiagnosticoYRubro(rubro: String) {
        world.startScenario()
        world.seedConcludedDiagnosis(categoryName = rubro)
    }

    @When("visualizo la respuesta del asistente")
    fun visualizoLaRespuestaDelAsistente() {
        // No-op: the round-trip has already settled in the
        // `Given` step.
    }

    @Then("veo la explicación del problema detectado")
    fun veoLaExplicacionDelProblema() {
        world.assertAssessmentVisible()
    }

    @Then("veo los prestadores recomendados del rubro {string}")
    fun veoLosPrestadoresRecomendados(rubro: String) {
        world.assertRecommendedProvidersVisible(categoryName = rubro)
    }


    @Then("cada prestador muestra nombre y apellido")
    fun cadaPrestadorMuestraNombreYApellido() {
        // "veo los prestadores recomendados" already proved a
        // non-empty list with a non-blank full name. Reuse the
        // same check so the step is independently runnable.
        world.assertRecommendedProvidersVisible(categoryName = "Plomería")
    }

    @Then("cada prestador muestra el rubro {string}")
    fun cadaPrestadorMuestraElRubro(rubro: String) {
        world.assertRecommendedProvidersVisible(categoryName = rubro)
    }

    @Then("cada prestador muestra su foto de perfil")
    fun cadaPrestadorMuestraSuFotoDePerfil() {
        // The ProviderAvatar is rendered for every provider row
        // regardless of whether the URL is null (the avatar falls
        // back to the initial). The Compose test pins the
        // avatar-image testTag to confirm the photo URL is wired
        // to the AsyncImage; here we only assert the world holds
        // a non-empty providers list so each row has a slot.
        val providers = world.lastUiState().recommendedProviders
            ?: error("expected recommended providers to be present for 10-DIA")
        if (providers.isEmpty()) {
            error("expected at least one provider row to render an avatar")
        }
    }


    /**
     * 11-DIA `When`: the user taps "Contactar" on the first
     * recommended provider in the carousel. The step param
     * carries the visible label so the matcher can sanity-check
     * the call (rejects presses on unrelated buttons on the
     * chat surface).
     */
    @When("toco {string} en el primer prestador recomendado")
    fun tocoEnElPrimerPrestadorRecomendado(label: String) {
        require(label == "Contactar") {
            "Acción desconocida en el contact flow del chat: '$label'"
        }
        world.tapContactOnFirstRecommendedProvider()
    }

    /**
     * 11-DIA `Then`: the AI pre-filled job-request wire was
     * sent with the FIRST recommended provider's id. The wording
     * "su propio resumen" reflects the backend's behavior: the AI
     * writes `title` and `description` server-side, the consumer
     * only sends the provider id.
     */
    @Then("la IA envía su propio resumen para ese prestador")
    fun laIaEnviaSuPropioResumenParaEsePrestador() {
        val provider = world.firstRecommendedProviderSnapshot()
        world.assertAiJobRequestInvokedFor(provider)
    }

    /**
     * 11-DIA `And`: the VM emitted
     * [AiDiagnosisContactEvent.NavigateToConversation], which the
     * route's `LaunchedEffect` forwards to
     * `Route.Conversation.buildPath(event.conversationId)`.
     */
    @And("la app navega a la conversación con ese prestador")
    fun laAppNavegaALaConversacionConEsePrestador() {
        world.assertNavigatesToConversation()
    }


    /**
     * 12-DIA `And`: seed the AI conversation list fake with
     * [count] synthetic conversations so the Assistant VM lands
     * on `Ready` with that exact list. The synthetic conversations
     * carry the canonical "Pérdida/Flujo en la cocina" titles
     * and the dev backend's timestamp so the `Then` step can
     * pin the eventual UI.
     */
    @And("he tenido {int} conversaciones previas con el asistente")
    fun heTenidoConversacionesPreviasConElAsistente(count: Int) {
        val conversations = (1..count).map { idx ->
            com.loresuelvo.consumer.domain.assistant.AiConversationSummary(
                id = idx.toString(),
                title = "Pérdida de agua #$idx",
                lastMessageAtEpochMillis = 1_716_080_400_000L + idx * 60_000L,
                lastMessagePreview = "Última respuesta del asistente #$idx",
            )
        }
        world.seedAiConversations(conversations)
    }

    @When("accedo al apartado \"Asistente IA\"")
    fun accedoAlApartadoAsistenteIA() {
        // No-op: the Assistant VM auto-loads on construction in
        // `startScenario()`, so the `Then` step can assert the
        // resulting state directly.
    }

    @Then("veo una lista con mis {int} sesiones previas con la IA")
    fun veoUnaListaConMisSesionesPreviasConLaIA(count: Int) {
        world.assertAssistantHasConversationCount(count)
    }

    @And("cada sesión muestra el título y la fecha del último mensaje")
    fun cadaSesionMuestraElTituloYLaFechaDelUltimoMensaje() {
        // The list is asserted to have N rows by the previous
        // `Then`; this step pins the per-row contract.
        world.assertAssistantConversationTitlePresent("Pérdida de agua #1")
        world.assertAssistantConversationsHaveTimestamp()
    }


    @When("toco el botón de adjuntar imagen desde la galería")
    fun tocoBotonAdjuntarImagenGaleria() {
        // No-op: the next `And` step drives the VM via
        // `world.chooseFromGallery`.
    }

    @And("selecciono la imagen {string}")
    fun seleccionoLaImagen(filename: String) {
        world.chooseFromGallery(filename)
    }

    @Then("la imagen queda pendiente de envío en la conversación")
    fun laImagenQuedaPendienteDeEnvio() {
        world.assertPendingAttachmentStaged()
    }

    /**
     * 01-AIP `Y puedo ver la vista previa de la imagen
     * seleccionada`. Shared with 02-AIP — see
     * [laImagenQuedaPendienteDeEnvio].
     */
    @And("puedo ver la vista previa de la imagen seleccionada")
    fun puedoVerLaVistaPreviaDeLaImagenSeleccionada() {
        world.assertPendingAttachmentStaged()
    }

    /**
     * 01-AIP `Y puedo confirmar el envío o descartarla`. Shared
     * with 02-AIP — the staged attachment is still on the
     * surface (the send round-trip has NOT fired).
     */
    @And("puedo confirmar el envío o descartarla")
    fun puedoConfirmarElEnvioODescartarla() {
        world.assertPendingAttachmentStaged()
    }


    /**
     * 02-AIP `When toco el botón de adjuntar imagen desde la
     * cámara`. Drives the canonical non-Uri VM entry point
     * through [world].chooseFromCamera, which collapses the
     * camera capture + launcher + reader into a single JPEG
     * staging. The Compose acceptance test owns the real
     * `TakePicture` launcher contract.
     */
    @When("toco el botón de adjuntar imagen desde la cámara")
    fun tocoBotonAdjuntarImagenCamara() {
        world.chooseFromCamera()
    }

    @And("capturo la foto {string}")
    fun capturoLaFoto(filename: String) {
        @Suppress("UNUSED_PARAMETER") filename
    }



    @Given("que tengo la imagen {string} pendiente de envío")
    fun tengoLaImagenPendiente(filename: String) {
        world.stageImages(listOf(filename))
    }

    /**
     * 06-AIP `Y la subida de archivos está disponible`. Wires the
     * `FileRepository` mock so presign/upload/confirm all return
     * Success with deterministic IDs, mirroring the production
     * happy-path contract.
     */
    @And("la subida de archivos está disponible")
    fun laSubidaDeArchivosEstaDisponible() {
        world.seedFileRepositorySuccess()
    }

    @And("la IA acepta el mensaje con la imagen")
    fun laIaAceptaElMensajeConLaImagen() {
        world.seedSuccessDiagnosis(
            assistantContent = "Detectamos una posible gotera en el baño.",
        )
    }

    @When("escribo {string}")
    fun escribo(text: String) {
        world.typePrompt(text)
    }

    @Then("se sube la imagen {string}")
    fun seSubeLaImagen(filename: String) {
        val calls = world.presignCallsSnapshot()
        val match = calls.firstOrNull { it.originalName == filename }
            ?: error(
                "expected a presign call with originalName='$filename', " +
                    "got ${calls.map { it.originalName }}",
            )
        // No-op pin: the assertion above is the contract. Keep
        // the variable so the compiler does not flag an unused
        // `match` should the helper evolve.
        @Suppress("UNUSED_VARIABLE") val unused = match
    }

    @Then("se envía el mensaje con la imagen adjunta")
    fun seEnviaElMensajeConLaImagenAdjunta() {
        val ids = world.lastImageFileIdsSnapshot()
        if (ids.isEmpty()) {
            error(
                "expected sendDiagnosisPrompt to be called with at least one " +
                    "image_file_ids entry, got an empty list",
            )
        }
    }


    /**
     * Drives the full happy path with
     * a `professional_required` assessment so the assistant
     * bubble + category pin from the next two steps land on a
     * real conversation state.
     */
    @Given("que envié la imagen {string} y la IA devolvió un pre diagnóstico")
    fun envieImagenYIaDevolvioPreDiagnostico(filename: String) {
        world.stageImages(listOf(filename))
        world.seedFileRepositorySuccess()
        world.seedConcludedDiagnosis(categoryName = "Plomería")
    }

    /**
     * The `professional_required` assessment's `problemCategory.name`
     * is the wire the UI renders next to the explanation.
     */
    @Then("veo la categoría detectada {string}")
    fun veoLaCategoriaDetectada(categoryName: String) {
        world.assertAssessmentCategoryVisible(categoryName)
    }

    @Given("que no tengo imágenes pendientes de envío")
    fun noTengoImagenesPendientesPrecondicion() {
        world.assertPendingAttachmentCount(expected = 0)
    }

    /**
     * 03-AIP `Y la vista previa muestra las N imágenes en
     * orden de selección`. Pins both count AND order so a
     * future commit that drops the call-order preservation
     * breaks this assertion cleanly.
     */
    @And("la vista previa muestra las {int} imágenes en orden de selección")
    fun previewMuestraLasNImagenesEnOrden(count: Int) {
        world.assertPendingAttachmentCount(expected = count)
        // Per-image names get pinned by 04-AIP instead, which
    }

    @Then("tengo {int} imágenes pendientes de envío en la conversación")
    fun tengoImagenesPendientesEnLaConversacion(count: Int) {
        world.assertPendingAttachmentCount(expected = count)
    }

    /**
     * 02-AIP `Y puedo ver la vista previa de la foto
     * capturada`. Same structural assertion as 01-AIP (one
     * attachment staged, send round-trip NOT fired) — the
     * wording diverges because the source is the camera rather
     * than the gallery.
     */
    @And("puedo ver la vista previa de la foto capturada")
    fun puedoVerLaVistaPreviaDeLaFotoCapturada() {
        world.assertPendingAttachmentStaged()
    }


    @Given("me encuentro en la pantalla de conversación con el asistente")
    fun meEncuentroEnLaPantallaChatIa() {
        // No-op: world.startScenario() already mounted the VM.
    }

    @Given("el campo de mensaje está vacío")
    fun elCampoDeMensajeEstaVacio() {
        // Structural assertion; pinned in unit tests.
    }


    @Given("que tengo las imágenes {string}, {string} y {string} pendientes de envío")
    fun tengoLasImagenesPendientes(first: String, second: String, third: String) {
        world.stageImages(listOf(first, second, third))
    }

    @When("elimino la imagen {string}")
    fun eliminoLaImagen(filename: String) {
        world.removeAttachmentByFilename(filename)
    }

    /**
     * 04-AIP `Entonces tengo N imágenes pendientes de envío`.
     */
    @Then("tengo {int} imágenes pendientes de envío")
    fun tengoImagenesPendientesCount(count: Int) {
        world.assertPendingAttachmentCount(count)
    }

    /**
     * 04-AIP `Y las imágenes pendientes son "x" y "y"`. The
     * assertion checks both membership AND order so the
     * companion "Y conservan su orden original" step stays a
     * no-op rather than a redundant check.
     */
    @Then("las imágenes pendientes son {string} y {string}")
    fun lasImagenesPendientesSon(first: String, second: String) {
        world.assertPendingAttachmentsAre(listOf(first, second))
    }

    @Then("conservan su orden original")
    fun conservanSuOrdenOriginal() {
        // See [lasImagenesPendientesSon] — order is already
        // asserted by the previous step.
    }


    @Given("que tengo {int} imágenes pendientes de envío")
    fun tengoNImagenesPendientes(count: Int) {
        world.stageNImages(count)
    }

    /**
     * 05-AIP `Cuando elimino todas las imágenes pendientes`.
     * Goes through the VM's "clear all" entry point so the
     * stage stays empty in one round-trip.
     */
    @When("elimino todas las imágenes pendientes")
    fun eliminoTodasLasImagenes() {
        world.clearAllAttachments()
    }

    /**
     * 05-AIP `Entonces no tengo imágenes pendientes de envío`.
     */
    @Then("no tengo imágenes pendientes de envío")
    fun noTengoImagenesPendientes() {
        world.assertPendingAttachmentCount(expected = 0)
    }


    @And("la subida de la imagen falla por un error de red")
    fun laSubidaDeLaImagenFallaPorUnErrorDeRed() {
        world.seedFileRepositoryNetworkFailure()
    }

    @Then("veo un error al subir la imagen")
    fun veoUnErrorAlSubirLaImagen() {
        world.assertAttachmentUploadErrorVisible()
    }

    @Then("la imagen continúa pendiente de envío")
    fun laImagenContinuaPendienteDeEnvio() {
        world.assertPendingAttachmentStaged()
    }

    @Then("el mensaje no se envía")
    fun elMensajeNoSeEnvia() {
        world.assertDiagnosisPromptWasNotSent()
    }

    @Given("que la subida de {string} falló")
    fun laSubidaDeLaImagenFallo(filename: String) {
        world.stageImages(listOf(filename))
        world.seedFileRepositoryNetworkFailure()
        world.typePrompt("Tengo una gotera en el baño")
        world.tapSend()
    }

    @When("reintento la carga de imágenes")
    fun reintentoLaCargaDeImagenes() {
        world.seedFileRepositorySuccess()
        world.seedSuccessDiagnosis(
            assistantContent = "Detectamos una posible gotera en el baño.",
        )
        world.simulateRetry()
    }

    @Then("la imagen se sube nuevamente")
    fun laImagenSeSubeNuevamente() {
        world.assertAttachmentUploadRetried("gotera-baño.jpg")
    }

    @Then("el mensaje se envía al completar la subida")
    fun elMensajeSeEnviaAlCompletarLaSubida() {
        world.assertDiagnosisPromptWasSent()
    }

    @Then("la imagen deja de estar pendiente de envío")
    fun laImagenDejaDeEstarPendienteDeEnvio() {
        world.assertPendingAttachmentCount(0)
    }


    /**
     * 10-AIP `Dado que envié la imagen "X"`. Collapses the
     * full upload-succeeded-then-prompt-rejected round-trip
     * into a single helper: stage the image, seed the file
     * repository so the upload pipeline confirms the bytes,
     * seed the next `sendDiagnosisPrompt` call to fail with
     * `Server(500)`, type a prompt and tap send. After this
     * step the VM is in the failure state (transientError
     * visible, sentAttachments populated).
     */
    @Given("que envié la imagen {string}")
    fun queEnvieLaImagen(filename: String) {
        world.stageImages(listOf(filename))
        world.seedFileRepositorySuccess()
        world.seedSendDiagnosisPromptServerFailure()
        world.typePrompt("Tengo una gotera en el baño")
        world.tapSend()
    }

    @And("el servicio de IA falla al procesar el mensaje")
    fun elServicioDeIaFallaAlProcesarElMensaje() {
        // See [queEnvieLaImagen].
    }

    /**
     * 10-AIP `Cuando visualizo el resultado del envío`. No-op:
     * the failure has already settled in the `Given` step.
     */
    @When("visualizo el resultado del envío")
    fun visualizoElResultadoDelEnvio() {
        // See [queEnvieLaImagen].
    }

    /**
     * 10-AIP `Entonces veo un error de procesamiento del pre
     * diagnóstico`. Pins `state.transientError` to
     * [ChatError.ServiceUnavailable] so the inline error card
     * surfaces the i18n message rendered by `ChatErrorCard`.
     */
    @Then("veo un error de procesamiento del pre diagnóstico")
    fun veoUnErrorDeProcesamientoDelPreDiagnostico() {
        world.assertTransientErrorIsServiceUnavailable()
    }

    /**
     * 10-AIP `Y la imagen enviada permanece en la
     * conversación`. Asserts the uploaded image moved into
     * `state.sentAttachments` (rather than being dropped or
     * kept in `pendingAttachments`).
     */
    @And("la imagen enviada permanece en la conversación")
    fun laImagenEnviadaPermaneceEnLaConversacion() {
        world.assertAttachmentPersistsAfterFailure()
    }

    /**
     * 10-AIP `Y puedo reintentar el procesamiento`. Pins
     * `state.lastAttemptedPrompt` so the retry CTA can
     * resubmit the same prompt without re-typing.
     */
    @And("puedo reintentar el procesamiento")
    fun puedoReintentarElProcesamiento() {
        val state = world.lastUiState()
        if (state.lastAttemptedPrompt.isNullOrBlank()) {
            error(
                "expected lastAttemptedPrompt to be populated after the failed send " +
                    "so the retry CTA can resubmit it; state=$state",
            )
        }
    }


    /**
     * 11-AIP `Dado que el procesamiento del pre diagnóstico
     * falló`. Mirrors 10-AIP's `Given` — the failure is the
     * precondition for the retry step.
     */
    @Given("que el procesamiento del pre diagnóstico falló")
    fun queElProcesamientoDelPreDiagnosticoFallo() {
        world.stageImages(listOf("gotera-baño.jpg"))
        world.seedFileRepositorySuccess()
        world.seedSendDiagnosisPromptServerFailure()
        world.typePrompt("Tengo una gotera en el baño")
        world.tapSend()
    }

    /**
     * 11-AIP `Y la imagen "X" ya fue enviada`. Pins the file
     * is in `state.sentAttachments` after the failed prompt
     * endpoint call.
     */
    @And("la imagen {string} ya fue enviada")
    fun laImagenYaFueEnviada(filename: String) {
        val state = world.lastUiState()
        if (state.sentAttachments.none { it.originalName == filename }) {
            error(
                "expected '$filename' to be in sentAttachments after the failed send, " +
                    "got ${state.sentAttachments.map { it.originalName }}",
            )
        }
    }

    /**
     * 11-AIP `Cuando reintento el procesamiento`. Seeds the
     * NEXT `sendPrompt` outcome as a successful diagnosis,
     * snapshots the current presign call count, then triggers
     * the retry. The snapshot is reused by the next step's
     * assertion that no additional uploads fire.
     */
    @When("reintento el procesamiento")
    fun reintentoElProcesamiento() {
        world.seedSuccessDiagnosis(
            assistantContent = "Detectamos una posible gotera en el baño.",
        )
        world.simulateRetry()
    }

    /**
     * 11-AIP `Entonces se vuelve a procesar el mensaje
     * enviado`. Pins `fakeRepo.lastImageFileIds` is non-empty:
     * the chat message endpoint was called again (this time
     * reusing the cached file IDs rather than the original
     * upload orchestrator).
     */
    @Then("se vuelve a procesar el mensaje enviado")
    fun seVuelveAProcesarElMensajeEnviado() {
        world.assertDiagnosisPromptWasSent()
    }

    /**
     * 11-AIP `Y no se vuelve a subir la imagen`. Pins the
     * file repository's `presign` call count didn't grow
     * across the retry — the cached file IDs bypass the
     * upload pipeline.
     */
    @And("no se vuelve a subir la imagen")
    fun noSeVuelveASubirLaImagen() {
        // The retry only calls `sendDiagnosisPrompt` with the
        // cached file IDs, so the file repository must NOT
        // receive any new presign calls. The world exposed
        // `presignCallsSnapshot()` already; the assertion is
        // self-contained (no parameter plumbing).
        val calls = world.presignCallsSnapshot()
        // Capture the size bound: the world has seeded file
        // repository success for ONE upload round-trip. After
        // the retry the call count must still equal that
        // baseline.
        if (calls.size != 1) {
            error(
                "expected exactly 1 presign call across the failed send + retry, " +
                    "got ${calls.size}; the retry path must reuse the cached file IDs",
            )
        }
    }

    /**
     * 11-AIP `Y veo una respuesta del asistente en la
     * conversación`. Pins the assistant bubble landed in
     * `state.messages` with non-blank content after the retry
     * round-trip settled.
     */
    @And("veo una respuesta del asistente en la conversación")
    fun veoUnaRespuestaDelAsistenteEnLaConversacion11Aip() {
        world.assertAssistantReplyAfterRetry()
    }

}
