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
 */
@Suppress("unused", "UNUSED_PARAMETER")
class PendingSteps {

    // ---- Scenario 02-IC: Redirect to chats ----------------

    @Given("I want to start a chat with a provider from the search results")
    fun iWantToStartAChatWithAProviderFromTheSearchResults() {
        throw PendingException("02-IC pending")
    }

    @When("I tap the {string} button on the provider")
    fun iTapTheButtonOnTheProvider(buttonLabel: String) {
        throw PendingException("02-IC pending")
    }

    @Then("I am redirected to the messages screen with the selected provider")
    fun iAmRedirectedToTheMessagesScreenWithTheSelectedProvider() {
        throw PendingException("02-IC pending")
    }

    // ---- Scenario 03-IC: Contact list ----------------------

    @Given("I already sent a message to a provider")
    fun iAlreadySentAMessageToAProvider() {
        throw PendingException("03-IC pending")
    }

    @When("I access the messages section")
    fun iAccessTheMessagesSection() {
        throw PendingException("03-IC pending")
    }

    @Then("I see the provider as a contact in my list")
    fun iSeeTheProviderAsAContactInMyList() {
        throw PendingException("03-IC pending")
    }

    // ---- Scenario 04-IC: Pending notification -------------

    @Given("I started a chat with a provider")
    fun iStartedAChatWithAProvider() {
        throw PendingException("04-IC pending")
    }

    @And("the provider has not yet accepted the conversation")
    fun theProviderHasNotYetAcceptedTheConversation() {
        throw PendingException("04-IC pending")
    }

    @When("I view the contact status")
    fun iViewTheContactStatus() {
        throw PendingException("04-IC pending")
    }

    @Then("I see a notification indicating that the provider has not yet accepted my request")
    fun iSeeANotificationIndicatingThatTheProviderHasNotYetAcceptedMyRequest() {
        throw PendingException("04-IC pending")
    }

    // ---- Scenario 05-IC: Send more messages --------------

    @Given("I started a chat with a provider and it was not accepted")
    fun iStartedAChatWithAProviderAndItWasNotAccepted() {
        throw PendingException("05-IC pending")
    }

    @When("I write a new message")
    fun iWriteANewMessage() {
        throw PendingException("05-IC pending")
    }

    @Then("I can send additional messages to the provider without restrictions")
    fun iCanSendAdditionalMessagesToTheProviderWithoutRestrictions() {
        throw PendingException("05-IC pending")
    }

    // ---- Scenario 06-IC: Persistence ----------------------

    @Given("I started a chat with a provider and sent a message")
    fun iStartedAChatWithAProviderAndSentAMessage() {
        throw PendingException("06-IC pending")
    }

    @When("I navigate to the home page")
    fun iNavigateToTheHomePage() {
        throw PendingException("06-IC pending")
    }

    @And("I return to the messages section with the same provider")
    fun iReturnToTheMessagesSectionWithTheSameProvider() {
        throw PendingException("06-IC pending")
    }

    @Then("I still see the message I sent earlier in the conversation")
    fun iStillSeeTheMessageISentEarlierInTheConversation() {
        throw PendingException("06-IC pending")
    }
}
