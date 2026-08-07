package com.loresuelvo.consumer.bdd.message

import com.loresuelvo.consumer.domain.conversation.ConversationStatus
import com.loresuelvo.consumer.ui.professional.ProfessionalsUiState
import com.loresuelvo.consumer.ui.screens.messages.MessagesListUiState
import com.loresuelvo.consumer.ui.screens.professional.ContactProviderEvent
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.Assert.assertEquals
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

    // ---- Scenario 03-IC --------------------------------------

    /**
     * "I already sent a message" → the backend already has a
     * conversation for the consumer with the provider. The BDD
     * seeds the fake [ConversationRepository] with that
     * conversation so the next `When` step observes it in the
     * list.
     */
    @Given("I already sent a message to a provider")
    fun iAlreadySentAMessageToAProvider() {
        world.startScenario()
        world.enqueueConversation(
            counterpartName = "Juan",
            counterpartSurname = "Pérez",
            categoryName = "Plomería",
            lastMessageContent = "Hola Juan, necesito una mano",
        )
    }

    /**
     * "I access the messages section" → the consumer enters the
     * `Route.Messages` bottom-bar tab. The VM's `init { load() }`
     * already fired against the empty seed at
     * [world.startScenario] time; this step re-fires `load()`
     * after the seeding so the conversation surfaces.
     */
    @When("I access the messages section")
    fun iAccessTheMessagesSection() {
        world.accessMessagesSection()
    }

    @Then("I see the provider as a contact in my list")
    fun iSeeTheProviderAsAContactInMyList() {
        val state = world.lastMessagesListUiState()
        assertTrue(
            "expected MessagesListUiState.Ready, was $state",
            state is MessagesListUiState.Ready,
        )
        val conversations = (state as MessagesListUiState.Ready).conversations
        assertEquals(
            "expected exactly one conversation in the list",
            1,
            conversations.size,
        )
        val counterpart = conversations.first().counterpart
        assertEquals("Juan", counterpart.name)
        assertEquals("Pérez", counterpart.surname)
        assertEquals("Plomería", counterpart.categoryName)
    }

    // ---- Scenario 04-IC --------------------------------------

    /**
     * "I started a chat with a provider" — the consumer has an
     * active conversation with status=Pending (the provider has
     * not yet accepted). The BDD seeds the conversation directly
     * with `Pending` so the row's notification badge will render.
     *
     * Re-uses the same world helpers as 03-IC; the only
     * difference is the explicit `status = Pending` parameter.
     */
    @Given("I started a chat with a provider")
    fun iStartedAChatWithAProvider() {
        world.startScenario()
        world.enqueueConversation(
            counterpartName = "Juan",
            counterpartSurname = "Pérez",
            categoryName = "Plomería",
            status = ConversationStatus.Pending,
            lastMessageContent = "Hola Juan, necesito una mano",
        )
    }

    /**
     * "The provider has not yet accepted the conversation" —
     * already encoded by the `Pending` status seeded in the
     * `Given` step. No further action; the step exists so the
     * Gherkin flow reads naturally.
     */
    @And("the provider has not yet accepted the conversation")
    fun theProviderHasNotYetAcceptedTheConversation() {
        // No-op: the seed carries the Pending status; the
        // assertion in the `Then` step verifies it surfaces.
    }

    /**
     * "I view the contact status" — entering the messages list
     * re-fetches the conversations and exposes the seeded
     * row's status. The row's notification badge is the
     * "status indicator" the user sees; the visual rendering
     * is pinned by
     * `MessagesScreenTest.ready_state_renders_pending_badge_only_for_pending_conversations`.
     */
    @When("I view the contact status")
    fun iViewTheContactStatus() {
        world.accessMessagesSection()
    }

    @Then("I see a notification indicating that the provider has not yet accepted my request")
    fun iSeeAPendingNotification() {
        val state = world.lastMessagesListUiState()
        assertTrue(
            "expected MessagesListUiState.Ready, was $state",
            state is MessagesListUiState.Ready,
        )
        val conversations = (state as MessagesListUiState.Ready).conversations
        assertEquals(
            "expected exactly one conversation in the list",
            1,
            conversations.size,
        )
        val conversation = conversations.first()
        // The "notification" the user sees is the row's
        // `PendingBadge` (`CONVERSATION_ROW_PENDING_TAG` in
        // `ConversationRow.kt`). The BDD asserts the data
        // backing that badge — `status is Pending` — and
        // trusts the Compose test for the visual rendering.
        assertTrue(
            "expected the conversation to be Pending, was ${conversation.status}",
            conversation.status is ConversationStatus.Pending,
        )
    }
}
