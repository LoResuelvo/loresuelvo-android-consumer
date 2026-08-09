package com.loresuelvo.consumer.bdd.message

import io.cucumber.java.PendingException
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When

/**
 * Placeholder step definitions for the scenarios in
 * `features/message/send-messages.feature` that are still marked
 * `@wip`. Each method throws [PendingException] so the Cucumber
 * JUnit reporter classifies the scenarios as **pending** (yellow)
 * instead of failing the build with `UndefinedStepException`.
 *
 * Each commit that goes green for one scenario deletes the
 * corresponding method(s) here and replaces them with the real
 * step implementation in `SendMessagesSteps.kt`. Once the last
 * `@wip` is removed, this file is empty and can be removed.
 *
 * Method naming follows the wording of each Gherkin step verbatim
 * so the mapping is obvious during code review. The
 * `PendingException` message carries the scenario code so the
 * Cucumber "pending" report points at the exact scenario that
 * still needs work.
 *
 * Scenarios 01-06-IC landed. Scenarios 07-10-IC are the
 * real-time-chat follow-up (WebSocket-driven); their step defs
 * live below until each scenario is implemented.
 */
@Suppress("unused", "UNUSED_PARAMETER")
class PendingSteps {

    // ---- Scenario 08-IC: isolation between conversations ---------

    @Given("I am viewing a conversation with one provider")
    fun iAmViewingAConversationWithOneProvider() {
        throw PendingException("08-IC pending")
    }

    @When("a different conversation receives a new message via WebSocket")
    fun aDifferentConversationReceivesANewMessageViaWebSocket() {
        throw PendingException("08-IC pending")
    }

    @Then("that message does not appear in the chat I am viewing")
    fun thatMessageDoesNotAppearInTheChatIAmViewing() {
        throw PendingException("08-IC pending")
    }

    // ---- Scenario 09-IC: auto-scroll at the bottom ---------------

    @Given("I am viewing a conversation and I am at the bottom of the chat")
    fun iAmViewingAConversationAndIAmAtTheBottomOfTheChat() {
        throw PendingException("09-IC pending")
    }

    @When("a new message arrives via WebSocket")
    fun aNewMessageArrivesViaWebSocket() {
        throw PendingException("09-IC pending")
    }

    @Then("the chat scrolls to show the new message")
    fun theChatScrollsToShowTheNewMessage() {
        throw PendingException("09-IC pending")
    }

    // ---- Scenario 10-IC: new-message indicator ------------------

    @Given("I am viewing a conversation and I am scrolled up reading older messages")
    fun iAmViewingAConversationAndIAmScrolledUpReadingOlderMessages() {
        throw PendingException("10-IC pending")
    }

    @Then("I see an indicator telling me there is a new message")
    fun iSeeAnIndicatorTellingMeThereIsANewMessage() {
        throw PendingException("10-IC pending")
    }

    @And("the chat does not auto-scroll")
    fun theChatDoesNotAutoScroll() {
        throw PendingException("10-IC pending")
    }
}