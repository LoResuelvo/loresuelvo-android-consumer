# language: en

Feature: Connect Google Calendar from consumer profile
  As an authenticated consumer
  I want to connect my Google Calendar from My profile
  So that future turns can be synchronized with my calendar

  Scenario: Consumer connects Google Calendar with event permission
    Given the consumer profile reports Google Calendar as "disconnected"
    When the consumer taps "Vincular Google Calendar"
    Then Android requests Google Calendar event permission
    And the authorization result sends the server auth code to the backend
    And the profile refresh shows Google Calendar as "connected"

  Scenario: Consumer cancels Google Calendar authorization
    Given the consumer profile reports Google Calendar as "disconnected"
    When the consumer cancels Google Calendar authorization
    Then the profile remains disconnected
    And the profile shows that Google Calendar was not linked

  Scenario: Consumer reauthorizes a calendar that requires attention
    Given the consumer profile reports Google Calendar as "action_required"
    When the consumer taps "Volver a vincular Google Calendar"
    And Google authorization returns an invalid server auth code
    Then the profile remains requiring attention
    And the profile offers retrying the Google Calendar connection
