package com.loresuelvo.consumer.bdd.message

import com.loresuelvo.consumer.ui.professional.ProfessionalsUiState
import org.junit.Assert.assertTrue
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When

/**
 * Real step implementations for the scenarios in
 * `features/message/send-messages.feature`. The methods for the
 * still-`@wip` scenarios (02-IC onward) remain in [PendingSteps]
 * and throw [io.cucumber.java.PendingException]; they move here
 * one commit at a time, mirroring the convention documented at
 * the top of the feature file.
 *
 * The BDD layer asserts the **state** behind the UI:
 *  - 01-IC pins the "search results list is populated" contract
 *    that backs the "Contactar" affordance per provider card. The
 *    visual rendering of each button is verified by the Compose
 *    test in `ProfessionalsAcceptanceTest` (the happy path through
 *    Home → Professionals already exercises it).
 */
class SendMessagesSteps {

    private val world: SendMessagesWorld = SendMessagesWorld()

    @Given("I am searching for providers by category")
    fun iAmSearchingForProvidersByCategory() {
        world.startScenario()
        // Hard-coded to Plomería for 01-IC; later scenarios (02-IC +)
        // will pivot the category via dedicated Given steps.
        world.loadProvidersForCategory("Plomería")
    }

    @When("I view the results list")
    fun iViewTheResultsList() {
        // No-op: the state is already populated by the Given step.
        // The scenario's intent is to observe the UI surface; the
        // BDD-level check is on the state, the rendering is on the
        // Compose test.
    }

    @Then("I see a message icon to contact them")
    fun iSeeAMessageIconToContactThem() {
        // The "message icon" in this US is the per-provider
        // `Contactar` button shipped by US-39
        // (`ProviderCard` in `ProfessionalsScreen`). The BDD
        // asserts the data layer that backs the affordance: the
        // providers list is populated and the consumer can pick
        // any provider to start a chat. The visual rendering of
        // the button per row is verified by
        // `ProfessionalsAcceptanceTest` (nav driven via Hilt).
        val state = world.lastUiState()
        assertTrue(
            "expected the providers list to be populated, was $state",
            state is ProfessionalsUiState.Ready && state.providers.isNotEmpty(),
        )
    }
}
