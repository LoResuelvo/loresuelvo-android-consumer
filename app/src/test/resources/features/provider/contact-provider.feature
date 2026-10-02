# language: en

Feature: Contact a provider from the category list

  As a consumer browsing providers for a category
  I want to send a contact request with my problem context
  So the provider can start a conversation about my issue

  Background:
    Given I am logged in as a consumer
    And the following categories exist:
      | id | name         |
      | 1  | Plomería     |
      | 2  | Electricidad |
      | 3  | Gas          |
    And the following providers exist:
      | id       | name  | surname | category_name | category_id |
      | prov-001 | Juan  | Pérez   | Plomería      | 1           |
      | prov-002 | Pedro | Dib     | Plomería      | 1           |
    And I tap the "Plomería" category card

  Scenario: 01-SRP Open the contact modal for a new provider
    When I tap the "Contactar" button on the provider "Juan Pérez"
    Then the "Create Work Request" modal opens
    And I see the provider name "Juan Pérez"
    And I see the required fields "PROBLEM TITLE" and "PROBLEM DESCRIPTION"

  Scenario: 02-SRP Send the work request successfully
    Given the "Create Work Request" modal is open for "Juan Pérez"
    When I enter a title, a description and tap the "Enviar solicitud" button
    Then a loading state is shown
    And the modal closes
    And I am redirected to the messages screen with "Juan Pérez"

  @wip
  Scenario: 03-SRP Verify the provider appears as a contact after the first message
    Given I already sent the work request to "Juan Pérez"
    When I access the messages section
    Then I see the provider "Juan Pérez" as a contact in my list

  @wip
  Scenario: 04-SRP Allow additional messages while the conversation is not yet accepted
    Given I started the conversation with "Juan Pérez"
    And the provider has not yet accepted the conversation
    When I write a new message
    Then I can send additional messages to the provider
