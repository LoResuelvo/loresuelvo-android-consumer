# language: en

Feature: Consumer my profile
  As an authenticated consumer
  I want to open My profile
  So that I can review my account data and profile photo

  @wip
  Scenario: Consumer opens My profile with a confirmed photo
    Given an authenticated consumer has a complete profile with a photo
    When the consumer opens "Mi perfil"
    Then the profile screen shows the consumer name and email
    And the profile screen shows the confirmed profile photo

  @wip
  Scenario: Consumer profile falls back to initials without a photo
    Given an authenticated consumer has a complete profile without a photo
    When the consumer opens "Mi perfil"
    Then the profile screen shows the consumer initials

  @wip
  Scenario: Consumer profile can retry after a session error
    Given loading the consumer profile fails because the session expired
    When the consumer taps "Reintentar"
    Then the profile screen requests the profile again
