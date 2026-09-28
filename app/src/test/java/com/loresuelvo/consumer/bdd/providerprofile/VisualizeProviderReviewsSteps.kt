package com.loresuelvo.consumer.bdd.providerprofile

import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class VisualizeProviderReviewsSteps {

    private val world = VisualizeProviderReviewsWorld()

    @Given("estoy autenticado como consumidor")
    fun estoyAutenticadoComoConsumidor() {
        world.startScenario()
    }

    @Given("estoy visualizando prestadores recomendados por el chat con IA")
    fun estoyVisualizandoPrestadoresRecomendadosPorElChatConIa() {
        world.showRecommendedProviders()
    }

    @Given("estoy visualizando prestadores de una categoría")
    fun estoyVisualizandoPrestadoresDeUnaCategoria() {
        world.showCategoryProviders()
    }

    @Given("uno de los prestadores tiene calificaciones recibidas")
    fun unoDeLosPrestadoresTieneCalificacionesRecibidas() {
        world.markProviderAsRated()
    }

    @When("accedo al perfil del prestador recomendado")
    fun accedoAlPerfilDelPrestadorRecomendado() {
        world.openProviderProfile()
    }

    @When("accedo al perfil de ese prestador")
    fun accedoAlPerfilDeEsePrestador() {
        world.openProviderProfile()
    }

    @Then("veo su calificación promedio representada con estrellas")
    fun veoSuCalificacionPromedioRepresentadaConEstrellas() {
        assertTrue(world.readyProfile().ratingAverage in 0.0..5.0)
    }

    @Then("veo el valor numérico de su calificación promedio")
    fun veoElValorNumericoDeSuCalificacionPromedio() {
        assertEquals(4.5, world.readyProfile().ratingAverage, 0.0)
    }

    @Then("veo la cantidad total de reseñas recibidas")
    fun veoLaCantidadTotalDeResenasRecibidas() {
        assertEquals(2, world.readyProfile().ratingCount)
    }
}
