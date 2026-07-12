package com.loresuelvo.consumer.bdd.providers.search

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

/**
 * Step defs for `features/provider/search-providers.feature`. Each
 * step is intentionally thin: the heavy lifting lives in
 * [CucumberWorld] (which pins `Dispatchers.Main` and drives the
 * `ProfessionalsViewModel` through a `StandardTestDispatcher`).
 *
 * Cucumber instantiates this class with its zero-arg constructor
 * (DefaultObjectFactory). The [CucumberWorld] is owned per scenario
 * via a field initializer; `close()` runs from `@After` /
 * teardown via the JVM shutdown hook.
 */
class SearchProvidersSteps {

    private val world: CucumberWorld = CucumberWorld()

    @Given("I am logged in as a consumer")
    fun iAmLoggedInAsAConsumer() {
        world.startScenario()
    }

    @Given("the following categories exist:")
    fun theFollowingCategoriesExist(table: DataTable) {
        world.loadCategories(
            table.asMaps(String::class.java, String::class.java),
        )
    }

    @Given("the following providers exist:")
    fun theFollowingProvidersExist(table: DataTable) {
        world.loadProviders(
            table.asMaps(String::class.java, String::class.java),
        )
    }

    @Given("I am on the consumer home")
    fun iAmOnTheConsumerHome() {
        // Already on the screen; navigation is captured via the VM.
    }

    @Given("no providers exist for category {string}")
    fun noProvidersExistForCategory(categoryName: String) {
        world.observeEmptyForCategory(categoryName)
    }

    @Given("the providers endpoint will fail with a network error")
    fun theProvidersEndpointWillFailWithANetworkError() {
        world.configureNetworkFailure()
    }

    @When("I tap the {string} category card")
    fun iTapTheCategoryCard(categoryName: String) {
        world.tapCategoryCard(categoryName)
    }

    @When("the providers list loads")
    fun theProvidersListLoads() {
        // No-op: Background starts on home; visiting the list happens
        // through `tap_category_card`. Kept for the one scenario that
        // exercises the list view directly during the BDD jump.
        world.visitProvidersFor(world.currentCategoryName() ?: "Electricidad")
    }

    @Then("I am taken to the providers list for category {string}")
    fun iAmTakenToTheProvidersListForCategory(categoryName: String) {
        world.lastUiState() // ensures the VM has produced a final state
        assertEquals(categoryName, world.currentCategoryName())
    }

    @Then("I see the provider {string} for category {string}")
    fun iSeeTheProviderForCategory(providerFullName: String, categoryName: String) {
        val state = world.lastUiState()
        assertTrue(
            "state must be Ready, was $state",
            state is com.loresuelvo.consumer.ui.professional.ProfessionalsUiState.Ready,
        )
        val names = (state as com.loresuelvo.consumer.ui.professional.ProfessionalsUiState.Ready)
            .providers
            .map { "${it.name} ${it.surname}" }
        assertTrue(
            "expected provider '$providerFullName' in $names",
            names.contains(providerFullName),
        )
    }

    @Then("I see the empty message {string}")
    fun iSeeTheEmptyMessage(expected: String) {
        val state = world.lastUiState()
        assertTrue(
            "state must be Empty, was $state",
            state is com.loresuelvo.consumer.ui.professional.ProfessionalsUiState.Empty,
        )
        assertEquals(expected, world.expectedEmptyMessage())
    }

    @Then("I see the error message {string}")
    fun iSeeTheErrorMessage(expected: String) {
        val state = world.lastUiState()
        assertTrue(
            "state must be Error, was $state",
            state is com.loresuelvo.consumer.ui.professional.ProfessionalsUiState.Error,
        )
        assertEquals(expected, world.expectedErrorMessage())
    }
}
