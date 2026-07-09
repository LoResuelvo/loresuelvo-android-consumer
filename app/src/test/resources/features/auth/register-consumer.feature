# language: en
#
# Executable specification for the "Complete consumer profile on first
# login" user journey. Runs as a Cucumber JVM scenario per scenario on
# the JVM classpath; each scenario exercises the
# `CompleteProfileViewModel` directly through `CucumberWorld`, fakes
# `FakeAuthSessionStore` / `FakeUserRepository`, and asserts the user-
# observable behaviour (state, events, side effects) without rendering
# Compose or hitting a real backend. The user-visible Spanish strings
# are tested in the Compose UI unit tests, not here.
#
# Update the `register-consumer.feature` together with `strings.xml`
# and the `CompleteProfileScreen` mapping whenever the wording
# changes.

Feature: Complete consumer profile on first login
  As an authenticated consumer whose profile is incomplete
  I want to enter my first name and last name and submit
  So that the backend records me as a consumer and the app lets me in

  Background:
    Given I am already authenticated with Auth0
    And my app session has no profile yet
    And I am on the "Complete profile" screen

  # --- Local validation: errors must never reach the backend ----------

  Scenario: Empty first name triggers a "first name required" error and never calls the backend
    When I leave the first name field blank
    And I type "Pérez" in the last name field
    And I tap the "Continuar" button
    Then I see a "first name required" error
    And no POST is sent to "/consumers"

  Scenario: Empty last name triggers a "last name required" error and never calls the backend
    When I type "Juan" in the first name field
    And I leave the last name field blank
    And I tap the "Continuar" button
    Then I see a "last name required" error
    And no POST is sent to "/consumers"

  # --- Happy path: register, persist, navigate home -------------------

  Scenario: Valid submission registers me and navigates home
    Given the backend will accept the registration
    When I type "Juan" in the first name field
    And I type "Pérez" in the last name field
    And I tap the "Continuar" button
    Then a POST is sent to "/consumers" with first name "Juan" and last name "Pérez"
    And the "Continuar" button is enabled again
    And the app signals "Navigate to Home"

  # --- Backend failures: visible errors, no navigation ----------------

  Scenario: A 401 from the backend clears the session and shows a "session expired" error
    Given the backend rejects the registration with status 401 and "Token expired"
    When I type "Juan" in the first name field
    And I type "Pérez" in the last name field
    And I tap the "Continuar" button
    Then a POST is sent to "/consumers" with first name "Juan" and last name "Pérez"
    And I see a "session expired" error
    And the local session is cleared
    And the "Continuar" button is enabled again

  Scenario: A 409 from the backend surfaces the conflict message as a "server" error
    Given the backend rejects the registration with status 409 and "Email already registered"
    When I type "Juan" in the first name field
    And I type "Pérez" in the last name field
    And I tap the "Continuar" button
    Then I see a "server" error containing the message "Email already registered"
    And the "Continuar" button is enabled again

  Scenario: A 500 from the backend surfaces the failure as a "server" error
    Given the backend rejects the registration with status 500 and "internal"
    When I type "Juan" in the first name field
    And I type "Pérez" in the last name field
    And I tap the "Continuar" button
    Then I see a "server" error containing the message "internal"
    And the "Continuar" button is enabled again

  Scenario: An unreachable backend shows a "network" error
    Given the backend is unreachable
    When I type "Juan" in the first name field
    And I type "Pérez" in the last name field
    And I tap the "Continuar" button
    Then I see a "network" error
    And the "Continuar" button is enabled again

  # --- Idempotent submit: re-entrancy must not double-fire ----------

  Scenario: Double-tapping "Continuar" sends only one POST
    Given the backend will accept the registration
    When I type "Juan" in the first name field
    And I type "Pérez" in the last name field
    And I tap the "Continuar" button twice in a row
    Then only one POST is sent to "/consumers"
