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
