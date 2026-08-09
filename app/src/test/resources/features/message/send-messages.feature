# language: en
#
# Executable specification for the US-17 "Start a conversation with
# a provider" user journey. The consumer initiates a chat with a
# provider they haven't interacted with yet and continues the
# conversation until the provider accepts (or doesn't).
#
# Each scenario starts marked `@wip` (skipped). Each commit removes
# the `@wip` from exactly one scenario, makes its assertions
# green, and leaves the rest at `@wip`. When the last `@wip` is
# removed the feature is done. The Cucumber JVM runner filters
# `@wip` via the `cucumber.filter.tags` system property set in
# `app/build.gradle.kts`.
#
# Update this file together with `strings.xml` and the screen
# whenever copy or behaviour changes. The user-visible Spanish
# strings are asserted in the Compose UI tests, not here.

Feature: Start a conversation with a provider

  As a consumer
  I want to send a contact request message to a provider I haven't interacted with
  So I can propose a job and start a chat

  Scenario: 01-IC Verify the message icon on the search results
    Given I am searching for providers by category
    When I view the results list
    Then I see a message icon to contact them

  Scenario: 02-IC Redirect to the chats screen when starting a conversation
    Given I want to start a chat with a provider from the search results
    When I tap the "Contactar" button on the provider
    Then I am redirected to the messages screen with the selected provider

  Scenario: 03-IC Verify the provider appears as a contact after the first message
    Given I already sent a message to a provider
    When I access the messages section
    Then I see the provider as a contact in my list

  Scenario: 04-IC Verify the pending request notification
    Given I started a chat with a provider
    And the provider has not yet accepted the conversation
    When I view the contact status
    Then I see a notification indicating that the provider has not yet accepted my request

  Scenario: 05-IC Verify the consumer can send more messages while the provider has not accepted
    Given I started a chat with a provider and it was not accepted
    When I write a new message
    Then I can send additional messages to the provider without restrictions

  Scenario: 06-IC Verify messages persist when navigating away and back
    Given I started a chat with a provider and sent a message
    When I navigate to the home page
    And I return to the messages section with the same provider
    Then I still see the message I sent earlier in the conversation

  Scenario: 07-IC Verify the consumer sees the provider's message arrive in real-time
    Given I am viewing a conversation with a provider
    When the provider sends me a new message via WebSocket
    Then I see the provider's message in the chat

  @wip
  Scenario: 08-IC Verify messages from other conversations do not leak into the current chat
    Given I am viewing a conversation with one provider
    When a different conversation receives a new message via WebSocket
    Then that message does not appear in the chat I am viewing

  @wip
  Scenario: 09-IC Verify the chat auto-scrolls to the new message when at the bottom
    Given I am viewing a conversation and I am at the bottom of the chat
    When a new message arrives via WebSocket
    Then the chat scrolls to show the new message

  @wip
  Scenario: 10-IC Verify a new-message indicator appears when the user is scrolled up
    Given I am viewing a conversation and I am scrolled up reading older messages
    When a new message arrives via WebSocket
    Then I see an indicator telling me there is a new message
    And the chat does not auto-scroll
