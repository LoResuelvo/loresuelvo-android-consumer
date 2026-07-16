package com.loresuelvo.consumer.bdd.home

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.Assert.assertEquals

/**
 * Step defs for `features/home/home.feature`. Each step is
 * intentionally thin: the heavy lifting lives in [HomeWorld]
 * (which pins `Dispatchers.Main` and drives the `HomeViewModel`
 * through a `StandardTestDispatcher`).
 *
 * Cucumber instantiates this class with its zero-arg constructor
 * (DefaultObjectFactory). The [HomeWorld] is owned per scenario
 * via a field initializer; `close()` runs from the JVM shutdown
 * hook.
 */
class HomeSteps {

    private val world: HomeWorld = HomeWorld()

    @Given("I am logged in as a consumer")
    fun iAmLoggedInAsAConsumer() {
        world.startScenario()
    }

    @Given("the backend exposes the following categories:")
    fun theBackendExposesTheFollowingCategories(table: DataTable) {
        world.loadCategories(
            table.asMaps(String::class.java, String::class.java),
        )
    }

    @When("the consumer opens the Home screen")
    fun theConsumerOpensTheHomeScreen() {
        world.openHome()
    }

    @Then("the visible categories are exactly these 6, in alphabetical order:")
    fun theVisibleCategoriesAreExactlyThese6InAlphabeticalOrder(table: DataTable) {
        val expected = table.asMaps(String::class.java, String::class.java)
            .map { it.getValue("name") }
        val actual = world.visibleCategoryNames()
        assertEquals(expected, actual)
    }
}