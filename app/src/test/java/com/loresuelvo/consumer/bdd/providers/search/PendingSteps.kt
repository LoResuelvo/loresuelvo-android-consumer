package com.loresuelvo.consumer.bdd.providers.search

import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.PendingException

/**
 * Placeholder step definitions for the profile-photo scenarios in
 * `features/provider/search-providers.feature` that are still marked
 * `@wip`. Each method throws [PendingException] so the Cucumber JUnit
 * reporter classifies the scenario as **pending** (yellow) instead of
 * failing the build with `UndefinedStepException`.
 *
 * Each commit that goes green for one of these scenarios deletes the
 * corresponding method(s) here and replaces them with the real step
 * implementation in [SearchProvidersSteps]. Once the last `@wip` is
 * removed this file should be empty and can be removed.
 */
@Suppress("unused", "UNUSED_PARAMETER")
class PendingSteps {

    // ---- Scenario: Provider profile photo URL flows through to UI --

    @Given("a provider in {string} has a profile photo assigned")
    fun aProviderInHasAProfilePhotoAssigned(categoryName: String) {
        throw PendingException("Provider-photo-URL scenario pending")
    }

    @Then("the provider card for {string} exposes the assigned profile photo")
    fun theProviderCardExposesTheAssignedProfilePhoto(providerFullName: String) {
        throw PendingException("Provider-photo-URL scenario pending")
    }

    // ---- Scenario: Provider without photo URL falls back to initial -

    @Given("a provider in {string} has no profile photo assigned")
    fun aProviderInHasNoProfilePhotoAssigned(categoryName: String) {
        throw PendingException("Provider-fallback-initial scenario pending")
    }

    @Then("the provider card for {string} falls back to the initial {string}")
    fun theProviderCardFallsBackToTheInitial(
        providerFullName: String,
        expectedInitial: String,
    ) {
        throw PendingException("Provider-fallback-initial scenario pending")
    }
}