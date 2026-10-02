package com.loresuelvo.consumer.bdd.home

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.Assert.assertEquals

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
