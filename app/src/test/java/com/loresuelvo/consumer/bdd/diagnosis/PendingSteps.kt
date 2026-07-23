package com.loresuelvo.consumer.bdd.diagnosis

import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.cucumber.java.PendingException

/**
 * Placeholder step definitions for scenarios in
 * `features/diagnosis/ai_diagnosis.feature` that are still marked
 * `@wip`. Each method throws [PendingException] so the Cucumber
 * JUnit reporter classifies the scenario as **pending** (yellow)
 * instead of failing the build with `UndefinedStepException`.
 *
 * Each commit that goes green for one scenario deletes the
 * corresponding method(s) here and replaces them with the real
 * step implementation in [AiDiagnosisSteps]. Once the last
 * `@wip` lands, this file should be empty and can be removed.
 *
 * Method naming follows the wording of each Gherkin step verbatim
 * so the mapping is obvious during code review.
 */
@Suppress("unused", "UNUSED_PARAMETER")
class PendingSteps {

    // ---- Scenario: 02-DIA Recibir respuesta del asistente ----------
    @Given("inicié una conversación con el asistente")
    fun inicieUnaConversacionConElAsistente() {
        throw PendingException("02-DIA pendiente")
    }

    @When("el asistente procesa mi mensaje")
    fun elAsistenteProcesaMiMensaje() {
        throw PendingException("02-DIA pendiente")
    }

    @Then("veo una respuesta del asistente en el chat")
    fun veoUnaRespuestaDelAsistenteEnElChat() {
        throw PendingException("02-DIA pendiente")
    }

    // ---- Scenario: 03-DIA Mostrar indicador de carga ----------------
    @Given("estoy en una conversación con el asistente")
    fun estoyEnUnaConversacionConElAsistente() {
        throw PendingException("03-DIA pendiente")
    }

    @When("envío un nuevo mensaje y la respuesta tarda en llegar")
    fun envioUnNuevoMensajeConRespuestaTardia() {
        throw PendingException("03-DIA pendiente")
    }

    @Then("veo un indicador de carga")
    fun veoUnIndicadorDeCarga() {
        throw PendingException("03-DIA pendiente")
    }

    @Then("no puedo enviar un nuevo mensaje hasta recibir una respuesta")
    fun noPuedoEnviarNuevoMensajeHastaRecibirRespuesta() {
        throw PendingException("03-DIA pendiente")
    }

    // ---- Scenario: 04-DIA Mostrar error de servicio ----------------
    @When("envío un nuevo mensaje y el servicio falla")
    fun envioUnNuevoMensajeYElServicioFalla() {
        throw PendingException("04-DIA pendiente")
    }

    @Then("veo el mensaje del asistente {string}")
    fun veoElMensajeDelAsistente(mensaje: String) {
        throw PendingException("04-DIA pendiente")
    }

    @Then("puedo volver a intentarlo")
    fun puedoVolverAIntentarlo() {
        throw PendingException("04-DIA pendiente")
    }

    // ---- Scenario: 05-DIA Mostrar advertencia de orientación
    @When("visualizo la conversación con el asistente")
    fun visualizoLaConversacionConElAsistente() {
        throw PendingException("05-DIA pendiente")
    }

    // ---- Scenario: 07-DIA Expandir campo de texto automáticamente ---
    @Given("me encuentro escribiendo un mensaje para el asistente")
    fun meEncuentroEscribiendoUnMensaje() {
        throw PendingException("07-DIA pendiente")
    }

    @When("el contenido supera una línea")
    fun elContenidoSuperaUnaLinea() {
        throw PendingException("07-DIA pendiente")
    }

    @Then("el campo de texto aumenta su altura automáticamente")
    fun elCampoDeTextoAumentaSuAltura() {
        throw PendingException("07-DIA pendiente")
    }

    @Then("permite visualizar hasta 6 líneas de contenido sin scroll")
    fun permiteVisualizarHasta6Lineas() {
        throw PendingException("07-DIA pendiente")
    }

    // ---- Scenario: 08-DIA Utilizar scroll en mensajes extensos -------
    @When("el contenido supera las 6 líneas visibles")
    fun elContenidoSuperaLas6Lineas() {
        throw PendingException("08-DIA pendiente")
    }

    @Then("el campo de texto mantiene una altura máxima de 6 líneas")
    fun elCampoMantieneAlturaMaximaDe6() {
        throw PendingException("08-DIA pendiente")
    }

    @Then("puedo desplazarme mediante scroll dentro del campo")
    fun puedoDesplazarmePorScroll() {
        throw PendingException("08-DIA pendiente")
    }

    @Then("el contenido completo permanece accesible")
    fun contenidoCompletoAccesible() {
        throw PendingException("08-DIA pendiente")
    }

    // ---- Scenario: 09-DIA Visualizar diagnóstico concluido -----------
    @Given("la IA concluyó el diagnóstico y recomienda prestadores del rubro {string}")
    fun laIaConcluyoDiagnosticoYRubro(rubro: String) {
        throw PendingException("09-DIA pendiente")
    }

    @When("visualizo la respuesta del asistente")
    fun visualizoLaRespuestaDelAsistente() {
        throw PendingException("09-DIA pendiente")
    }

    @Then("veo la explicación del problema detectado")
    fun veoLaExplicacionDelProblema() {
        throw PendingException("09-DIA pendiente")
    }

    @Then("veo los prestadores recomendados del rubro {string}")
    fun veoLosPrestadoresRecomendados(rubro: String) {
        throw PendingException("09-DIA pendiente")
    }

    // ---- Scenario: 10-DIA Visualizar datos de cada prestador --------
    @Then("cada prestador muestra nombre y apellido")
    fun cadaPrestadorMuestraNombreYApellido() {
        throw PendingException("10-DIA pendiente")
    }

    @Then("cada prestador muestra el rubro {string}")
    fun cadaPrestadorMuestraElRubro(rubro: String) {
        throw PendingException("10-DIA pendiente")
    }

    @Then("cada prestador muestra su foto de perfil")
    fun cadaPrestadorMuestraSuFotoDePerfil() {
        throw PendingException("10-DIA pendiente")
    }

    // ---- Scenario: 11-DIA Conversación sin recomendaciones ------------
    @Given("la IA respondió sin recomendar prestadores")
    fun laIaRespondioSinRecomendarPrestadores() {
        throw PendingException("11-DIA pendiente")
    }

    @Then("no veo la sección de prestadores recomendados")
    fun noVeoLaSeccionDePrestadores() {
        throw PendingException("11-DIA pendiente")
    }

    @Then("la conversación continúa normalmente")
    fun laConversacionContinuaNormalmente() {
        throw PendingException("11-DIA pendiente")
    }
}
