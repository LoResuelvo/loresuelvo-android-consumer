package com.loresuelvo.consumer.bdd.providers.contact

import io.cucumber.java.PendingException
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When

/**
 * Placeholder step definitions for the scenarios in
 * `features/provider/contact-provider.feature` that are still marked
 * `@wip`. Each method throws [PendingException] so the Cucumber
 * JUnit reporter classifies the scenarios as **pending** (yellow)
 * instead of failing the build with `UndefinedStepException`.
 *
 * Each commit that goes green for one scenario deletes the
 * corresponding method(s) here and replaces them with the real
 * step implementation in [ContactProviderSteps]. Once the last
 * `@wip` is removed, this file is empty and can be removed.
 *
 * The shared Background steps (`I am logged in as a consumer`,
 * `the following categories exist:`, `the following providers
 * exist:`, `I tap the {string} category card`) live in
 * `com.loresuelvo.consumer.bdd.providers.search.SearchProvidersSteps`
 * and are reachable via the runner's `glue` package list.
 *
 * Method naming follows the wording of each Gherkin step verbatim
 * so the mapping is obvious during code review.
 */
@Suppress("unused", "UNUSED_PARAMETER")
class PendingSteps {

    // ---- Scenario 01-SRP: Open the contact modal ----------------

    @When("I tap the {string} button on the provider {string}")
    fun iTapTheButtonOnTheProvider(buttonLabel: String, providerFullName: String) {
        throw PendingException("Contact-provider scenario pending")
    }

    @Then("the {string} modal opens")
    fun theModalOpens(modalTitle: String) {
        throw PendingException("Contact-provider scenario pending")
    }

    @And("I see the provider name {string}")
    fun iSeeTheProviderName(providerFullName: String) {
        throw PendingException("Contact-provider scenario pending")
    }

    @And("I see the required fields {string} and {string}")
    fun iSeeTheRequiredFields(firstFieldLabel: String, secondFieldLabel: String) {
        throw PendingException("Contact-provider scenario pending")
    }

    // ---- Scenario 02-SRP: Send the work request -----------------

    @Given("the {string} modal is open for {string}")
    fun theModalIsOpenForProvider(modalTitle: String, providerFullName: String) {
        throw PendingException("Contact-provider scenario pending")
    }

    @When("I enter a title, a description and tap the {string} button")
    fun iEnterTitleDescriptionAndTapButton(buttonLabel: String) {
        throw PendingException("Contact-provider scenario pending")
    }

    @Then("a loading state is shown")
    fun aLoadingStateIsShown() {
        throw PendingException("Contact-provider scenario pending")
    }

    @And("the modal closes")
    fun theModalCloses() {
        throw PendingException("Contact-provider scenario pending")
    }

    @And("I am redirected to the messages screen with {string}")
    fun iAmRedirectedToTheMessagesScreenWith(providerFullName: String) {
        throw PendingException("Contact-provider scenario pending")
    }

    // ---- Scenario 03-SRP: Contact list -------------------------

    @Given("I already sent the work request to {string}")
    fun iAlreadySentTheWorkRequestTo(providerFullName: String) {
        throw PendingException("Contact-provider scenario pending")
    }

    @When("I access the messages section")
    fun iAccessTheMessagesSection() {
        throw PendingException("Contact-provider scenario pending")
    }

    @Then("I see the provider {string} as a contact in my list")
    fun iSeeTheProviderAsAContactInMyList(providerFullName: String) {
        throw PendingException("Contact-provider scenario pending")
    }

    // ---- Scenario 04-SRP: Additional messages ------------------

    @Given("I started the conversation with {string}")
    fun iStartedTheConversationWith(providerFullName: String) {
        throw PendingException("Contact-provider scenario pending")
    }

    @And("the provider has not yet accepted the conversation")
    fun theProviderHasNotYetAcceptedTheConversation() {
        throw PendingException("Contact-provider scenario pending")
    }

    @When("I write a new message")
    fun iWriteANewMessage() {
        throw PendingException("Contact-provider scenario pending")
    }

    @Then("I can send additional messages to the provider")
    fun iCanSendAdditionalMessagesToTheProvider() {
        throw PendingException("Contact-provider scenario pending")
    }
}
