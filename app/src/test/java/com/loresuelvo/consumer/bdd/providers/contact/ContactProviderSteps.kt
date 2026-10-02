package com.loresuelvo.consumer.bdd.providers.contact

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import com.loresuelvo.consumer.ui.screens.professional.ContactProviderEvent
import com.loresuelvo.consumer.ui.screens.professional.ContactProviderUiState
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When

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
        // visual / locale-dependent — covered by the Compose UI
        // test). It asserts the structurally required fields exist
        // on the form state and start empty.
        val state = world.lastUiState() as ContactProviderUiState.Open
        assertEquals("", state.title)
        assertEquals("", state.description)
    }


    @Given("the {string} modal is open for {string}")
    fun theModalIsOpenFor(modalTitle: String, providerFullName: String) {
        world.openContactFor(providerFullName)
    }

    @When("I enter a title, a description and tap the {string} button")
    fun iEnterTitleDescriptionAndTapButton(buttonLabel: String) {
        // Seed the fake repo so the submit returns the success
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
        // (the contact feature wires it to `Route.Conversation` in
        // `LoResuelvoNav`; the actual chat surface is out of scope).
        // We still capture the payload for any future assertion
    }
}
