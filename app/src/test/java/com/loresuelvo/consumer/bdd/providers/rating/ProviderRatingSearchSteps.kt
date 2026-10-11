package com.loresuelvo.consumer.bdd.providers.rating

import io.cucumber.java.After
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

class ProviderRatingSearchSteps {

    private val world = ProviderRatingSearchWorld()

    @Given("que estoy autenticado como consumidor")
    fun queEstoyAutenticadoComoConsumidor() {
        world.startScenario()
    }

    @Given("existen los rubros {string} y {string}")
    fun existenLosRubros(primerRubro: String, segundoRubro: String) {
        world.addCategories(primerRubro, segundoRubro)
    }

    @Given("estoy viendo los prestadores del rubro {string}")
    fun estoyViendoLosPrestadoresDelRubro(rubro: String) {
        world.showCategory(rubro)
    }

    @Given("que {string} tiene una calificación promedio de {double} con {int} reseñas")
    fun tieneUnaCalificacionPromedioConResenas(
        prestador: String,
        promedio: Double,
        cantidad: Int,
    ) {
        world.setProviderRating(prestador, promedio, cantidad)
    }

    @Given("{string} tiene una calificación promedio de {double} con {int} reseñas")
    fun tieneUnaCalificacionPromedioConResenasSinQue(
        prestador: String,
        promedio: Double,
        cantidad: Int,
    ) {
        world.setProviderRating(prestador, promedio, cantidad)
    }

    @Given("que {string} no tiene calificaciones recibidas")
    fun noTieneCalificacionesRecibidas(prestador: String) {
        world.removeProviderRating(prestador)
    }

    @Given("que {string} de {string} tiene una calificación promedio de {double} con {int} reseñas")
    fun deTieneUnaCalificacionPromedioConResenas(
        prestador: String,
        rubro: String,
        promedio: Double,
        cantidad: Int,
    ) {
        world.setProviderRating(prestador, promedio, cantidad, rubro)
    }

    @Given("{string} de {string} tiene una calificación promedio de {double} con {int} reseñas")
    fun deTieneUnaCalificacionPromedioConResenasSinQue(
        prestador: String,
        rubro: String,
        promedio: Double,
        cantidad: Int,
    ) {
        world.setProviderRating(prestador, promedio, cantidad, rubro)
    }

    @Given("que hay prestadores con distintas calificaciones en el rubro {string}")
    fun hayPrestadoresConDistintasCalificaciones(rubro: String) {
        world.setProviderRating("Juan Pérez", 4.8, 12, rubro)
        world.setProviderRating("Pedro Díaz", 3.9, 5, rubro)
    }

    @Given("tengo aplicado un filtro de calificación mínima de {double} estrellas")
    fun tengoAplicadoUnFiltroDeCalificacionMinima(estrellas: Double) {
        world.selectMinimumRating(estrellas)
    }

    @Given("que tengo seleccionado ordenar por {string}")
    fun queTengoSeleccionadoOrdenarPor(opcion: String) {
        assertEquals("Mejor calificación", opcion)
        world.selectBestRatingOrder()
    }

    @Given("tengo seleccionado ordenar por {string}")
    fun tengoSeleccionadoOrdenarPor(opcion: String) {
        assertEquals("Mejor calificación", opcion)
        world.selectBestRatingOrder()
    }

    @Given("que ningún prestador del rubro {string} tiene una calificación de al menos {double} estrellas")
    fun ningunPrestadorTieneUnaCalificacionDeAlMenos(rubro: String, estrellas: Double) {
        world.setProviderRating("Juan Pérez", estrellas - 0.1, 2, rubro)
    }

    @When("visualizo el listado de prestadores")
    fun visualizoElListadoDePrestadores() {
        world.loadCurrentCategory()
    }

    @When("selecciono ordenar por {string}")
    fun seleccionoOrdenarPor(opcion: String) {
        assertEquals("Mejor calificación", opcion)
        world.selectBestRatingOrder()
    }

    @When("filtro por una calificación mínima de {double} estrellas")
    fun filtroPorUnaCalificacionMinima(estrellas: Double) {
        world.selectMinimumRating(estrellas)
    }

    @When("quito el filtro de rating")
    fun quitoElFiltroDeRating() {
        world.clearRatingFilter()
    }

    @Then("la tarjeta de {string} muestra {double} estrellas")
    fun laTarjetaMuestraEstrellas(prestador: String, promedio: Double) {
        assertEquals(promedio, world.provider(prestador).ratingAverage, 0.0)
        assertTrue(world.provider(prestador).ratingCount > 0)
    }

    @Then("muestra que tiene {int} reseñas")
    fun muestraQueTieneResenas(cantidad: Int) {
        val providers = world.visibleProviderNames().map { world.provider(it) }
        assertTrue(providers.any { it.ratingCount == cantidad })
    }

    @Then("la tarjeta de {string} muestra {string}")
    fun laTarjetaMuestraTexto(prestador: String, texto: String) {
        assertEquals("Sin calificaciones", texto)
        val provider = world.provider(prestador)
        assertEquals(0.0, provider.ratingAverage, 0.0)
        assertEquals(0, provider.ratingCount)
    }

    @Then("el prestador continúa disponible en el listado")
    fun elPrestadorContinuaDisponibleEnElListado() {
        assertTrue(world.visibleProviderNames().contains("Pedro Díaz"))
    }

    @Then("veo primero a {string}")
    fun veoPrimeroA(prestador: String) {
        world.assertNextProvider(prestador)
    }

    @Then("luego veo a {string}")
    fun luegoVeoA(prestador: String) {
        world.assertNextProvider(prestador)
    }

    @Then("veo a {string} en el listado")
    fun veoEnElListado(prestador: String) {
        assertTrue(world.visibleProviderNames().contains(prestador))
    }

    @Then("no veo a {string} en el listado")
    fun noVeoEnElListado(prestador: String) {
        assertFalse(
            "Visible providers: ${world.visibleProviderNames()}",
            world.visibleProviderNames().contains(prestador),
        )
    }

    @Then("solo veo prestadores de {string}")
    fun soloVeoPrestadoresDe(rubro: String) {
        assertEquals(setOf(rubro), world.visibleProvidersCategory())
    }

    @Then("vuelvo a ver todos los prestadores del rubro {string}")
    fun vuelvoAVerTodosLosPrestadoresDelRubro(rubro: String) {
        assertTrue(world.hasAllProvidersVisible())
        assertEquals(setOf(rubro), world.visibleProvidersCategory())
    }

    @Then("se conserva el orden seleccionado previamente")
    fun seConservaElOrdenSeleccionadoPreviamente() {
        assertTrue(world.preservesRatingOrderAfterClear())
    }

    @Then("veo un estado vacío específico para el filtro de rating")
    fun veoUnEstadoVacioEspecificoParaElFiltroDeRating() {
        assertTrue(world.isFilteredEmpty())
    }

    @Then("puedo quitar el filtro para volver a ver los prestadores")
    fun puedoQuitarElFiltroParaVolverAVerLosPrestadores() {
        assertTrue(world.hasMinimumRatingFilter())
        world.clearRatingFilter()
        assertTrue(world.hasAllProvidersVisible())
    }

    @After
    fun close() {
        world.close()
    }
}
