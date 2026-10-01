# language: en

Feature: Show Google Calendar status in consumer appointments
  As a consumer
  I want my appointments to reflect my Google Calendar connection
  So that I know whether I need to connect or reauthorize it

  Scenario: Connected consumer sees Calendar synchronization status on a future turn
    Given the consumer has a future turn and Google Calendar status is "connected"
    When the consumer opens "Mis turnos"
    Then the turn list shows that Google Calendar is connected
    And Android does not create a local calendar event

  Scenario: Disconnected consumer is guided to My profile
    Given the consumer has a future turn and Google Calendar status is "disconnected"
    When the consumer opens "Mis turnos"
    Then the turn list invites the consumer to connect Google Calendar
    When the consumer chooses the Calendar connection action
    Then the app navigates to "Mi perfil"

  Scenario: Consumer requiring attention is guided to reauthorize
    Given the consumer has a future turn and Google Calendar status is "action_required"
    When the consumer opens "Mis turnos"
    Then the turn list indicates that Google Calendar requires authorization
    And the turn list offers reauthorizing from "Mi perfil"
