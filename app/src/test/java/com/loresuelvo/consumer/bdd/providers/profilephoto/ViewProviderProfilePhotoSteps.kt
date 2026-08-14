package com.loresuelvo.consumer.bdd.providers.profilephoto

import com.loresuelvo.consumer.bdd.providers.search.CucumberWorld
import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

/**
 * Spanish step defs for
 * `features/provider/view-provider-profile-photo.feature`. Mirrors
 * the English `SearchProvidersSteps.kt` in
 * `bdd.providers.search` but writes the Gherkin regex in Spanish
 * (per the project's BDD rule: "Steps de BDD: español"). Each step
 * is intentionally thin: the heavy lifting lives in
 * [CucumberWorld] (which pins `Dispatchers.Main` and drives the
 * `ProfessionalsViewModel` through a `StandardTestDispatcher`).
 *
 * Cucumber instantiates this class with its zero-arg constructor
 * (DefaultObjectFactory). The [CucumberWorld] is owned per scenario
 * via a field initializer; `close()` runs from `@After` /
 * teardown via the JVM shutdown hook.
 *
 * Only the steps exercised by scenario 01-VFP are defined here.
 * Scenarios 02-VFP and 03-VFP stay `@wip`; their step defs land in
 * a follow-up commit when each scenario goes green.
 */
class ViewProviderProfilePhotoSteps {

    private val world: CucumberWorld = CucumberWorld()

    @Given("estoy autenticado como consumidor")
    fun estoyAutenticadoComoConsumidor() {
        world.startScenario()
    }

    @Given("existen las siguientes categorías:")
    fun existenLasSiguientesCategorias(table: DataTable) {
        world.loadCategories(
            table.asMaps(String::class.java, String::class.java),
        )
    }

    @Given("existen los siguientes prestadores:")
    fun existenLosSiguientesPrestadores(table: DataTable) {
        world.loadProviders(
            table.asMaps(String::class.java, String::class.java),
        )
    }

    @Given("estoy en la pantalla Home del consumidor")
    fun estoyEnLaPantallaHomeDelConsumidor() {
        // No-op: el mundo arranca ya posicionado en Home; la
        // navegación al listado se captura vía el ViewModel en el
        // `Cuando toco la tarjeta…`.
    }

    @When("toco la tarjeta de la categoría {string}")
    fun tocoLaTarjetaDeLaCategoria(categoryName: String) {
        world.tapCategoryCard(categoryName)
    }

    @Then("llego a la lista de prestadores del rubro {string}")
    fun llegoALaListaDePrestadoresDelRubro(categoryName: String) {
        world.lastUiState() // asegura que el VM produjo un estado final
        assertEquals(categoryName, world.currentCategoryName())
    }

    @Then("la tarjeta del prestador {string} expone la foto de perfil {string}")
    fun laTarjetaDelPrestadorExponeLaFotoDePerfil(
        providerFullName: String,
        expectedPhotoUrl: String,
    ) {
        // Read from the VM's UiState, NOT from the world seed. This
        // pins the data flow end-to-end: the URL that the user sees
        // on screen must be the same URL the backend produced, with
        // no transformation or drop along the
        // `repo → use case → VM → Ready.providers[]` chain.
        val state = world.lastUiState()
        assertTrue(
            "state must be Ready, was $state",
            state is com.loresuelvo.consumer.ui.professional.ProfessionalsUiState.Ready,
        )
        val providers = (state as com.loresuelvo.consumer.ui.professional.ProfessionalsUiState.Ready)
            .providers
        val providerInState = providers
            .firstOrNull { "${it.name} ${it.surname}" == providerFullName }
        assertNotNull(
            "expected provider '$providerFullName' in state.providers=$providers",
            providerInState,
        )
        val actualPhotoUrl = providerInState!!.profilePhotoUrl
        assertEquals(
            "expected provider '$providerFullName' to expose profile photo " +
                "'$expectedPhotoUrl' in the VM state, was '$actualPhotoUrl'. " +
                "If this fails the URL is being dropped or transformed between " +
                "the repository and the UiState — the visual fallback " +
                "(initial letter) is what the consumer actually sees.",
            expectedPhotoUrl,
            actualPhotoUrl,
        )
    }
}