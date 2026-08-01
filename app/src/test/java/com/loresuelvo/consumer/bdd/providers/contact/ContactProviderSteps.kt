package com.loresuelvo.consumer.bdd.providers.contact

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import com.loresuelvo.consumer.ui.screens.professional.ContactProviderEvent
import com.loresuelvo.consumer.ui.screens.professional.ContactProviderUiState
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When

/**
 * Step definitions for the `contact-provider.feature` scenarios.
 *
 * The Background uses the shared `I am logged in as a consumer`
 * + `the following categories exist:` + `the following providers
 * exist:` + `I tap the {string} category card` steps, which live
 * in `com.loresuelvo.consumer.bdd.providers.search.SearchProvidersSteps`
 * (the runner's `glue` includes both packages). This class owns
 * only the contact-specific steps.
 *
 * The BDD asserts the [ContactProviderViewModel] state machine,
 * not the visual rendering. The actual UI pixels (the "Create
 * Work Request" title, the field labels, the submit button
 * enabled/disabled state) are verified by `ContactProviderBottomSheetTest`
 * in `src/test/.../ui/screens/professional/`.
 */
class ContactProviderSteps {

    private val world: ContactProviderWorld = ContactProviderWorld()

    @When("I tap the {string} button on the provider {string}")
    fun iTapTheButtonOnTheProvider(buttonLabel: String, providerFullName: String) {
        world.openContactFor(providerFullName)
    }

    @Then("the {string} modal opens")
    fun theModalOpens(modalTitle: String) {
        val state = world.lastUiState()
        assertTrue(
            "expected the contact form to be open, was $state",
            state is ContactProviderUiState.Open,
        )
    }

    @And("I see the provider name {string}")
    fun iSeeTheProviderName(providerFullName: String) {
        val state = world.lastUiState() as ContactProviderUiState.Open
        val provider = world.providerNamed(providerFullName)
        assertEquals(provider, state.provider)
    }

    @And("I see the required fields {string} and {string}")
    fun iSeeTheRequiredFields(firstFieldLabel: String, secondFieldLabel: String) {
        // The BDD doesn't read the literal field labels (those are
        // visual / locale-dependent — covered by the Compose UI
        // test). It asserts the structurally required fields exist
        // on the form state and start empty.
        val state = world.lastUiState() as ContactProviderUiState.Open
        assertEquals("", state.title)
        assertEquals("", state.description)
    }

    // ---- Scenario 02-SRP: submission flow ------------------------

    @Given("the {string} modal is open for {string}")
    fun theModalIsOpenFor(modalTitle: String, providerFullName: String) {
        world.openContactFor(providerFullName)
    }

    @When("I enter a title, a description and tap the {string} button")
    fun iEnterTitleDescriptionAndTapButton(buttonLabel: String) {
        // Seed the fake repo so the submit returns the success
        // path. The narrative of the Gherkin step implies the user
        // typed the values used in the Background.
        world.enqueueSuccess()
        world.typeTitle("Fuga en el lavamanos")
        world.typeDescription("Hay una gotera debajo del lavamanos del baño")
        world.submit()
    }

    @Then("a loading state is shown")
    fun aLoadingStateIsShown() {
        // The VM flips `isSubmitting = true` synchronously inside
        // `onSubmit` before the `viewModelScope.launch` is
        // scheduled. With `StandardTestDispatcher + advanceUntilIdle`,
        // that intermediate state is captured in the observed history.
        val showedLoading = world.observedStates().any { state ->
            state is ContactProviderUiState.Open && state.isSubmitting
        }
        assertTrue(
            "expected a loading state in the observed history, " +
                "got ${world.observedStates()}",
            showedLoading,
        )
    }

    @And("the modal closes")
    fun theModalCloses() {
        assertEquals(ContactProviderUiState.Closed, world.lastUiState())
    }

    @And("I am redirected to the messages screen with {string}")
    fun iAmRedirectedToTheMessagesScreenWith(providerFullName: String) {
        val event = world.observedEvents().lastOrNull()
        assertTrue(
            "expected a NavigateToConversation event, was $event",
            event is ContactProviderEvent.NavigateToConversation,
        )
        // The event's conversationId is opaque at the BDD layer
        // (the contact feature wires it to `Route.Conversation` in
        // `LoResuelvoNav`; the actual chat surface is out of scope).
        // We still capture the payload for any future assertion
        // when the navigation surface is fleshed out.
    }
}
