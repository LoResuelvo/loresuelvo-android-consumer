package com.loresuelvo.consumer.bdd.message

import com.loresuelvo.consumer.ui.screens.professional.ContactProviderEvent
import com.loresuelvo.consumer.ui.professional.ProfessionalsUiState
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.Assert.assertTrue

/**
 * Real step implementations for the scenarios in
 * `features/message/send-messages.feature`. The methods for the
 * still-`@wip` scenarios (03-IC onward) remain in [PendingSteps]
 * and throw [io.cucumber.java.PendingException]; they move here
 * one commit at a time, mirroring the convention documented at
 * the top of the feature file.
 *
 * Per the team discipline ("one scenario per commit, ≤ 400 lines"),
 * the comments at each step flag whether the assertion is at the
 * state level (this file) or the visual / integration level (covered
 * separately by the Compose test suite).
 */
class SendMessagesSteps {

    private val world: SendMessagesWorld = SendMessagesWorld()

    // ---- Scenario 01-IC --------------------------------------

    @Given("I am searching for providers by category")
    fun iAmSearchingForProvidersByCategory() {
        world.startScenario()
        // Hard-coded to Plomería for 01-IC; later scenarios pivot the
        // category via dedicated Given steps.
        world.loadProvidersForCategory("Plomería")
    }

    @When("I view the results list")
    fun iViewTheResultsList() {
        // No-op: the state is already populated by the Given step.
    }

    @Then("I see a message icon to contact them")
    fun iSeeAMessageIconToContactThem() {
        // The "message icon" in this US is the per-provider
        // `Contactar` button (US-39). The BDD asserts the data layer
        // that backs the affordance; the visual rendering per row
        // is covered by `ProfessionalsAcceptanceTest`.
        val state = world.lastUiState()
        assertTrue(
            "expected the providers list to be populated, was $state",
            state is ProfessionalsUiState.Ready && state.providers.isNotEmpty(),
        )
    }

    // ---- Scenario 02-IC --------------------------------------

    @Given("I want to start a chat with a provider from the search results")
    fun iWantToStartAChatWithAProviderFromTheSearchResults() {
        world.startScenario()
        world.loadProvidersForCategory("Plomería")
    }

    /**
     * The Gherkin says "I tap the 'Contactar' button on the
     * provider" — a single user-visible action. The implementation
     * drills all the way through because the navigation event is
     * only emitted after the contact form is submitted (the
     * `CreateJobRequestUseCase` round-trip is the trigger for
     * `NavigateToConversation`). The BDD:
     *  - opens the contact form (modal)
     *  - pre-loads a success outcome on the fake repo so the submit
     *    lands cleanly
     *  - fills the required fields (the VM's `canSubmit` is gated
     *    on non-blank title + description)
     *  - submits the form
     */
    @When("I tap the {string} button on the provider")
    fun iTapTheButtonOnTheProvider(buttonLabel: String) {
        // Hard-coded to "Juan Pérez" because the Gherkin doesn't pin
        // a provider name; the BDD fixture has only this provider
        // in Plomería, so the lookup is unambiguous.
        world.openContactFor("Juan Pérez")
        world.preLoadSuccess()
        world.typeTitle("Fuga en el lavamanos")
        world.typeDescription("Hay una gotera debajo del lavamanos del baño")
        world.submitContact()
    }

    @Then("I am redirected to the messages screen with the selected provider")
    fun iAmRedirectedToTheMessagesScreenWithTheSelectedProvider() {
        val event = world.observedContactEvents().lastOrNull()
        assertTrue(
            "expected a NavigateToConversation event, was $event",
            event is ContactProviderEvent.NavigateToConversation,
        )
        // The selected provider surfaces in the event payload
        // (the contact VM carries the provider through the form
        // state); the actual destination route is wired in
        // `LoResuelvoNav.kt` and is verified by the Compose
        // integration test in `ProfessionalsAcceptanceTest`.
    }
}
