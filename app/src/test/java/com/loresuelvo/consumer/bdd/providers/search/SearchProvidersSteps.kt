package com.loresuelvo.consumer.bdd.providers.search

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue

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


    @Given("a provider in {string} has a profile photo assigned")
    fun aProviderInHasAProfilePhotoAssigned(categoryName: String) {
        world.overridePhotoUrlForFirstProviderIn(
            categoryName,
            photoUrl = PLACEHOLDER_PHOTO_URL,
        )
    }

    /**
     * Mirrors the "has photo assigned" step but explicitly clears
     * the URL so the UI's initial-letter fallback path is exercised.
     */
    @Given("a provider in {string} has no profile photo assigned")
    fun aProviderInHasNoProfilePhotoAssigned(categoryName: String) {
        world.overridePhotoUrlForFirstProviderIn(categoryName, photoUrl = null)
    }

    /**
     * Asserts the provider in the VM state carries the assigned
     * photo URL (non-null). The visual rendering of the photo is
     * verified by the Compose tests in
     * `ui/screens/professional/ProviderAvatarTest.kt`; this step
     * pins the data flow only.
     */
    @Then("the provider card for {string} exposes the assigned profile photo")
    fun theProviderCardExposesTheAssignedProfilePhoto(providerFullName: String) {
        assertNotNull(
            "expected provider '$providerFullName' to expose an assigned " +
                "profile photo URL, was ${world.photoUrlOf(providerFullName)}",
            world.photoUrlOf(providerFullName),
        )
    }

    /**
     * Asserts the provider in the VM state carries `null` as the
     * photo URL, which is the contract that makes the
     * [com.loresuelvo.consumer.ui.screens.professional.ProviderAvatar]
     * render the initial-letter fallback. The [expectedInitial]
     * parameter is informational (the visual pixel is verified by
     * the Compose test).
     */
    @Then("the provider card for {string} falls back to the initial {string}")
    fun theProviderCardFallsBackToTheInitial(
        providerFullName: String,
        expectedInitial: String,
    ) {
        assertNull(
            "expected provider '$providerFullName' to have no profile photo " +
                "(fallback initial '$expectedInitial' would render), " +
                "was ${world.photoUrlOf(providerFullName)}",
            world.photoUrlOf(providerFullName),
        )
    }

    private companion object {
        // Synthetic URL used by the "photo assigned" Given step. The
        // implementation detail owned by the backend / MinIO); any
        // non-null string is enough to drive the photo-URL code path.
        const val PLACEHOLDER_PHOTO_URL: String = "http://example.test/photo.webp"
    }
}
