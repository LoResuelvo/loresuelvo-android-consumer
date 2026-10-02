package com.loresuelvo.consumer.bdd.message

import com.loresuelvo.consumer.domain.conversation.ConversationStatus
import com.loresuelvo.consumer.ui.professional.ProfessionalsUiState
import com.loresuelvo.consumer.ui.screens.chat.ConversationUiState
import com.loresuelvo.consumer.ui.screens.messages.MessagesListUiState
import com.loresuelvo.consumer.ui.screens.professional.ContactProviderEvent
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class SendMessagesSteps {

    private val world: SendMessagesWorld = SendMessagesWorld()


    @Given("I am searching for providers by category")
    fun iAmSearchingForProvidersByCategory() {
        world.startScenario()
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
        // that backs the affordance; the visual rendering per row
        // is covered by `ProfessionalsInstrumentedTest`.
        val state = world.lastUiState()
        assertTrue(
            "expected the providers list to be populated, was $state",
            state is ProfessionalsUiState.Ready && state.providers.isNotEmpty(),
        )
    }


    @Given("I want to start a chat with a provider from the search results")
    fun iWantToStartAChatWithAProviderFromTheSearchResults() {
        world.startScenario()
        world.loadProvidersForCategory("Plomería")
    }

    @When("I tap the {string} button on the provider")
    fun iTapTheButtonOnTheProvider(buttonLabel: String) {
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
        // integration test in `ProfessionalsInstrumentedTest`.
    }


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
        // backing that badge — `status is Pending` — and
        // trusts the Compose test for the visual rendering.
        assertTrue(
            "expected the conversation to be Pending, was ${conversation.status}",
            conversation.status is ConversationStatus.Pending,
        )
    }


    /**
     * "I started a chat with a provider and it was not accepted"
     * — the consumer has opened a thread (visible in the
     * messages list AND accessible via the detail endpoint).
     * The world seeds both endpoints from the same source data
     * so opening the conversation lands on a populated thread.
     */
    @Given("I started a chat with a provider and it was not accepted")
    fun iStartedAChatWithAProviderAndItWasNotAccepted() {
        world.startScenario()
        world.enqueueConversation(
            counterpartName = "Juan",
            counterpartSurname = "Pérez",
            categoryName = "Plomería",
            status = ConversationStatus.Pending,
            lastMessageContent = "Hola Juan, necesito una mano",
        )
        world.openConversation("1")
    }

    @When("I write a new message")
    fun iWriteANewMessage() {
        world.typeMessage("¿Podés venir mañana a las 10?")
        world.tapSend()
    }

    @Then("I can send additional messages to the provider without restrictions")
    fun iCanSendAdditionalMessagesToTheProviderWithoutRestrictions() {
        val state = world.lastConversationUiState()
        assertTrue(
            "expected ConversationUiState.Ready, was $state",
            state is ConversationUiState.Ready,
        )
        val ready = state as ConversationUiState.Ready

        // The server-persisted bubble landed in the thread — that
        // proves the round-trip completed end-to-end.
        val sentContent = "¿Podés venir mañana a las 10?"
        assertTrue(
            "expected the sent message to be in the thread, was " +
                ready.detail.messages,
            ready.detail.messages.any { it.content == sentContent },
        )

        // The send call hit the fake repo with the right id +
        // content (pin that the right message was sent, not
        // just that some message went through).
        val calls = world.observedSendCalls()
        assertEquals(
            "expected exactly one send call, was $calls",
            1,
            calls.size,
        )
        assertEquals("1", calls.single().first)
        assertEquals(sentContent, calls.single().second)

        // "Without restrictions" — pin that the conversation
        // was Pending when the send went through (so a future
        // commit that gates the composer on `status == Accepted`
        assertEquals(
            ConversationStatus.Pending,
            ready.detail.status,
        )
    }


    /**
     * "I started a chat with a provider and sent a message" — the
     * conversation already has the consumer's first message
     * persisted on the backend (carried in the seeded detail's
     * `messages[]`). The world seeds the detail so the screen
     * renders a populated thread on first load.
     */
    @Given("I started a chat with a provider and sent a message")
    fun iStartedAChatWithAProviderAndSentAMessage() {
        world.startScenario()
        world.enqueueConversation(
            counterpartName = "Juan",
            counterpartSurname = "Pérez",
            categoryName = "Plomería",
            status = ConversationStatus.Pending,
            lastMessageContent = "Hola Juan, necesito una mano",
        )
        world.openConversation("1")
    }

    /**
     * "I navigate to the home page" — the consumer leaves the
     * conversation screen. At the VM level this means the
     * NavBackStackEntry is popped and Hilt's scoped VM is
     * discarded; the world simulates that by replacing the
     * [ConversationViewModel] reference with a fresh instance
     * (no observer, no state) so a stale reference cannot leak
     * into the `Then` assertion.
     */
    @When("I navigate to the home page")
    fun iNavigateToTheHomePage() {
        world.leaveConversationScreen()
    }

    /**
     * "I return to the messages section with the same provider"
     * — the consumer taps the conversation row again. A new
     * [ConversationViewModel] is built (Hilt semantics) and
     * [ConversationViewModel.load] fires against the seeded
     * detail; the previously-sent message must surface in the
     * fresh state stream.
     */
    @And("I return to the messages section with the same provider")
    fun iReturnToTheMessagesSectionWithTheSameProvider() {
        world.reenterConversationScreen("1")
    }

    @Then("I still see the message I sent earlier in the conversation")
    fun iStillSeeTheMessageISentEarlierInTheConversation() {
        val state = world.lastConversationUiState()
        assertTrue(
            "expected ConversationUiState.Ready after re-entry, was $state",
            state is ConversationUiState.Ready,
        )
        val ready = state as ConversationUiState.Ready
        // The previously-sent message is part of the seeded
        // detail's `messages[]`. After the VM is rebuilt and
        // re-loads, the message must be present in the new
        // state's `detail.messages` — the persistence contract.
        val expectedContent = "Hola Juan, necesito una mano"
        assertTrue(
            "expected the previously-sent message to survive the navigation cycle, " +
                "was ${ready.detail.messages}",
            ready.detail.messages.any { it.content == expectedContent },
        )
    }


    /**
     * "I am viewing a conversation with a provider" — the
     * consumer has opened the chat. Seed the detail with one
     * message so the screen has something to render before the
     * provider pushes a new one.
     */
    @Given("I am viewing a conversation with a provider")
    fun iAmViewingAConversationWithAProvider() {
        world.startScenario()
        world.enqueueConversation(
            counterpartName = "Juan",
            counterpartSurname = "Pérez",
            categoryName = "Plomería",
            status = ConversationStatus.Pending,
            lastMessageContent = "Hola Juan, necesito una mano",
        )
        world.openConversation("1")
    }

    /**
     * "The provider sends me a new message via WebSocket" — at
     * the data layer this is the backend pushing a
     * `conversation.message.created` frame. The world emits the
     * event into the same flow the VM subscribed to in `init {}`,
     * so the test exercises the production decoding + filtering
     * path end-to-end.
     */
    @When("the provider sends me a new message via WebSocket")
    fun theProviderSendsMeANewMessageViaWebSocket() {
        world.providerSendsViaWebSocket(
            conversationId = "1",
            messageId = "200",
            content = "¿El jueves por la mañana te queda cómodo?",
        )
    }

    @Then("I see the provider's message in the chat")
    fun iSeeTheProvidersMessageInTheChat() {
        val state = world.lastConversationUiState()
        assertTrue(
            "expected ConversationUiState.Ready, was $state",
            state is ConversationUiState.Ready,
        )
        val messages = (state as ConversationUiState.Ready).detail.messages
        val expectedContent = "¿El jueves por la mañana te queda cómodo?"
        assertTrue(
            "expected the provider's message to be in the thread after the WS push, " +
                "was $messages",
            messages.any { it.content == expectedContent },
        )
    }


    /**
     * "I am viewing a conversation with one provider" — same
     * precondition as 07-IC (open a chat, seeded with one
     * message). The world seeds conversation id `"1"`; the
     * different-conversation event below uses id `"99"` so the
     * VM's filter on `state.detail.id == event.conversationId`
     * rejects it.
     */
    @Given("I am viewing a conversation with one provider")
    fun iAmViewingAConversationWithOneProvider() {
        world.startScenario()
        world.enqueueConversation(
            counterpartName = "Juan",
            counterpartSurname = "Pérez",
            categoryName = "Plomería",
            status = ConversationStatus.Pending,
            lastMessageContent = "Hola Juan, necesito una mano",
        )
        world.openConversation("1")
    }

    /**
     * "A different conversation receives a new message via
     * WebSocket" — emit a `WsEvent` for a conversation id other
     * than the one the consumer is viewing. The VM's filter
     * must drop the frame before it can mutate the state.
     */
    @When("a different conversation receives a new message via WebSocket")
    fun aDifferentConversationReceivesANewMessageViaWebSocket() {
        world.providerSendsViaWebSocket(
            conversationId = "99",
            messageId = "300",
            content = "Mensaje de otra conversación que no debería aparecer",
        )
    }

    @Then("that message does not appear in the chat I am viewing")
    fun thatMessageDoesNotAppearInTheChatIAmViewing() {
        val state = world.lastConversationUiState()
        assertTrue(
            "expected ConversationUiState.Ready, was $state",
            state is ConversationUiState.Ready,
        )
        val messages = (state as ConversationUiState.Ready).detail.messages
        val foreignContent = "Mensaje de otra conversación que no debería aparecer"
        assertTrue(
            "messages from another conversation must not leak into the current chat, " +
                "was $messages",
            messages.none { it.content == foreignContent },
        )
    }


    /**
     * "I am viewing a conversation and I am at the bottom of the
     * chat" — same precondition as 07-IC plus the scroll
     * position. The VM's default `isAtBottom = true` already
     * matches this state, so we just assert the seed.
     */
    @Given("I am viewing a conversation and I am at the bottom of the chat")
    fun iAmViewingAConversationAndIAmAtTheBottomOfTheChat() {
        world.startScenario()
        world.enqueueConversation(
            counterpartName = "Juan",
            counterpartSurname = "Pérez",
            categoryName = "Plomería",
            status = ConversationStatus.Pending,
            lastMessageContent = "Hola Juan, necesito una mano",
        )
        world.openConversation("1")
    }

    /**
     * "A new message arrives via WebSocket" — generic variant
     * used by 09-IC / 10-IC where the conversation id is implicit
     * (the one the consumer is viewing).
     */
    @When("a new message arrives via WebSocket")
    fun aNewMessageArrivesViaWebSocket() {
        world.providerSendsViaWebSocket(
            conversationId = "1",
            messageId = "200",
            content = "Confirmado para el jueves.",
        )
    }

    /**
     * "The chat scrolls to show the new message" — when the user
     * is at the bottom, the new bubble lands without raising the
     * "↓ nuevo mensaje" banner (the screen auto-scrolls into it
     * directly). The state assertion: `hasUnreadIncoming` must be
     * `false`. The visual scroll behaviour itself is pinned by
     * the Compose test suite.
     */
    @Then("the chat scrolls to show the new message")
    fun theChatScrollsToShowTheNewMessage() {
        val state = world.lastConversationUiState()
        assertTrue(
            "expected ConversationUiState.Ready, was $state",
            state is ConversationUiState.Ready,
        )
        val ready = state as ConversationUiState.Ready
        assertTrue(
            "the new bubble must be in the thread, was ${ready.detail.messages}",
            ready.detail.messages.any { it.content == "Confirmado para el jueves." },
        )
        assertEquals(
            "at-bottom user must NOT see the unread banner",
            false,
            ready.hasUnreadIncoming,
        )
    }


    /**
     * "I am viewing a conversation and I am scrolled up reading
     * older messages" — same precondition as 09-IC plus the
     * scroll position flipped to `false`. The VM persists this
     * in `Ready.isAtBottom` and the next incoming WS event will
     * raise the unread banner.
     */
    @Given("I am viewing a conversation and I am scrolled up reading older messages")
    fun iAmViewingAConversationAndIAmScrolledUpReadingOlderMessages() {
        world.startScenario()
        world.enqueueConversation(
            counterpartName = "Juan",
            counterpartSurname = "Pérez",
            categoryName = "Plomería",
            status = ConversationStatus.Pending,
            lastMessageContent = "Hola Juan, necesito una mano",
        )
        world.openConversation("1")
        world.scrolledUpOfTheChat()
    }

    @Then("I see an indicator telling me there is a new message")
    fun iSeeAnIndicatorTellingMeThereIsANewMessage() {
        val state = world.lastConversationUiState()
        assertTrue(
            "expected ConversationUiState.Ready, was $state",
            state is ConversationUiState.Ready,
        )
        val ready = state as ConversationUiState.Ready
        assertTrue(
            "scrolled-up user must see the unread banner flag",
            ready.hasUnreadIncoming,
        )
    }

    /**
     * "The chat does not auto-scroll" — companion assertion to
     * `I see an indicator…`. The VM keeps `hasUnreadIncoming = true`
     * exactly because it deliberately did NOT auto-scroll; if the
     * scroll had happened, the screen would have reported
     * `isAtBottom = true` back and the flag would be `false`.
     * Pinning `hasUnreadIncoming == true` is therefore equivalent
     * to pinning "no auto-scroll".
     */
    @And("the chat does not auto-scroll")
    fun theChatDoesNotAutoScroll() {
        val state = world.lastConversationUiState()
        assertTrue(
            "expected ConversationUiState.Ready, was $state",
            state is ConversationUiState.Ready,
        )
        val ready = state as ConversationUiState.Ready
        assertEquals(
            "no auto-scroll means hasUnreadIncoming stays true",
            true,
            ready.hasUnreadIncoming,
        )
    }
}
