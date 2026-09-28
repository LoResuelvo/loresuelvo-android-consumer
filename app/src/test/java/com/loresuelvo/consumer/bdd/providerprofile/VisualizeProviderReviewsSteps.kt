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

    @Given("estoy visualizando el perfil de un prestador")
    fun estoyVisualizandoElPerfilDeUnPrestador() {
        world.showProviderProfile()
    }

    @Given("estoy visualizando el historial de un prestador")
    fun estoyVisualizandoElHistorialDeUnPrestador() {
        world.showProviderProfile()
    }

    @Given("el prestador tiene trabajos completados")
    fun elPrestadorTieneTrabajosCompletados() {
        world.markProviderAsCompletedWork()
    }

    @Given("el prestador tiene un trabajo completado")
    fun elPrestadorTieneUnTrabajoCompletado() {
        world.markProviderAsSingleCompletedWork()
    }

    @Given("el prestador tiene un trabajo completado con una reseña")
    fun elPrestadorTieneUnTrabajoCompletadoConUnaResena() {
        world.markProviderAsReviewedWork()
    }

    @Given("el prestador tiene múltiples trabajos completados")
    fun elPrestadorTieneMultiplesTrabajosCompletados() {
        world.markProviderAsMultipleCompletedWork()
    }

    @Given("algunos de los trabajos tienen reseñas")
    fun algunosDeLosTrabajosTienenResenas() {
        world.markSomeWorkAsReviewed()
    }

    @Given("el prestador no tiene calificaciones recibidas")
    fun elPrestadorNoTieneCalificacionesRecibidas() {
        world.markProviderWithoutReviews()
    }

    @Given("el prestador no tiene trabajos completados")
    fun elPrestadorNoTieneTrabajosCompletados() {
        world.markProviderWithoutCompletedWork()
    }

    @Given("el prestador tiene un trabajo completado sin reseña")
    fun elPrestadorTieneUnTrabajoCompletadoSinResena() {
        world.markProviderAsUnreviewedWork()
    }

    @When("accedo al perfil del prestador recomendado")
    fun accedoAlPerfilDelPrestadorRecomendado() {
        world.openProviderProfile()
    }

    @When("accedo al perfil de ese prestador")
    fun accedoAlPerfilDeEsePrestador() {
        world.openProviderProfile()
    }

    @When("consulto su historial de trabajos")
    fun consultoSuHistorialDeTrabajos() {
        world.openProviderProfile()
    }

    @When("visualizo el trabajo en el historial")
    fun visualizoElTrabajoEnElHistorial() {
        world.openProviderProfile()
    }

    @When("visualizo su resumen de reputación")
    fun visualizoSuResumenDeReputacion() {
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

    @Then("veo los trabajos completados del prestador")
    fun veoLosTrabajosCompletadosDelPrestador() {
        assertEquals(2, world.readyProfile().workOrders.size)
        assertTrue(world.readyProfile().workOrders.all { it.isCompleted() })
    }

    @Then("los trabajos están ordenados desde el más reciente al más antiguo")
    fun losTrabajosEstanOrdenadosDesdeElMasRecienteAlMasAntiguo() {
        assertEquals(
            listOf("newest", "oldest"),
            world.readyProfile().workOrders.map { it.id },
        )
    }

    @Then("veo todos sus trabajos completados")
    fun veoTodosSusTrabajosCompletados() {
        assertEquals(2, world.readyProfile().workOrders.size)
        assertTrue(world.readyProfile().workOrders.all { it.isCompleted() })
    }

    @Then("veo la fecha programada del trabajo")
    fun veoLaFechaProgramadaDelTrabajo() {
        assertEquals(1_755_273_600_000, world.readyProfile().workOrders.first().scheduledOnEpochMillis)
    }

    @Then("veo la descripción del trabajo realizado")
    fun veoLaDescripcionDelTrabajoRealizado() {
        assertEquals("Reparación de pérdida de agua en cocina.", world.readyProfile().workOrders.first().description)
    }

    @Then("veo el reporte de entrega redactado por el prestador")
    fun veoElReporteDeEntregaRedactadoPorElPrestador() {
        assertEquals(
            "Trabajo finalizado y funcionamiento verificado.",
            world.readyProfile().workOrders.first().completionReport?.description,
        )
    }

    @Then("veo la calificación recibida representada con estrellas")
    fun veoLaCalificacionRecibidaRepresentadaConEstrellas() {
        assertEquals(5, world.readyProfile().workOrders.first().review?.rating)
    }

    @Then("veo el comentario de la reseña")
    fun veoElComentarioDeLaResena() {
        assertEquals(
            "Trabajo prolijo y excelente atención.",
            world.readyProfile().workOrders.first().review?.description,
        )
    }

    @Then("veo la calificación y el comentario en los trabajos que tienen una reseña")
    fun veoLaCalificacionYElComentarioEnLosTrabajosQueTienenUnaResena() {
        val reviewed = world.readyProfile().workOrders.filter { it.review != null }
        assertEquals(1, reviewed.size)
        assertEquals(5, reviewed.single().review?.rating)
        assertEquals("Trabajo prolijo y excelente atención.", reviewed.single().review?.description)
    }

    @Then("veo una calificación promedio de 0")
    fun veoUnaCalificacionPromedioDeCero() {
        assertEquals(0.0, world.readyProfile().ratingAverage, 0.0)
    }

    @Then("veo que tiene 0 reseñas")
    fun veoQueTieneCeroResenas() {
        assertEquals(0, world.readyProfile().ratingCount)
    }

    @Then("veo un estado vacío indicando que todavía no tiene trabajos completados")
    fun veoUnEstadoVacioDeTrabajosCompletados() {
        assertTrue(world.readyProfile().workOrders.isEmpty())
    }

    @Then("veo la información del trabajo realizado")
    fun veoLaInformacionDelTrabajoRealizado() {
        assertEquals(
            "Reparación de pérdida de agua en cocina.",
            world.readyProfile().workOrders.first().description,
        )
    }

    @Then("no veo una calificación asociada al trabajo")
    fun noVeoUnaCalificacionAsociadaAlTrabajo() {
        assertTrue(world.readyProfile().workOrders.first().review == null)
    }

    @Then("no veo un comentario de reseña asociado al trabajo")
    fun noVeoUnComentarioDeResenaAsociadoAlTrabajo() {
        assertTrue(world.readyProfile().workOrders.first().review?.description == null)
    }

    private fun com.loresuelvo.consumer.domain.provider.ProviderWorkOrder.isCompleted(): Boolean =
        status in setOf(
            com.loresuelvo.consumer.domain.provider.ProviderWorkOrderStatus.AwaitingPayment,
            com.loresuelvo.consumer.domain.provider.ProviderWorkOrderStatus.Paid,
            com.loresuelvo.consumer.domain.provider.ProviderWorkOrderStatus.Finished,
        )
}
