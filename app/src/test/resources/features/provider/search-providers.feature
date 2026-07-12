# language: en
#
# Executable specification for the "Search providers by category"
# user journey. Each scenario exercises the
# `ProfessionalsViewModel` directly through `CucumberWorld` with a
# `FakeProviderRepository` (no Hilt, no Compose, no backend). The
# user-visible strings are tested in the Compose acceptance tests in
# `src/androidTest/.../acceptance/`, not here.
#
# Update this file together with `strings.xml` and the screen
# whenever copy or behavior changes.
#
# How we drive progress: every scenario starts marked `@wip`
# (skipped). Each commit removes the `@wip` from exactly one
# scenario, makes its assertions green, and leaves the rest at
# `@wip`. When the last `@wip` is removed the feature is done.

Feature: Search providers by category

  Background:
    Given I am logged in as a consumer
    And the following categories exist:
      | id | name         |
      | 1  | Plomería     |
      | 2  | Electricidad |
      | 3  | Gas          |
    And the following providers exist:
      | id       | name  | surname | category_name | category_id |
      | prov-001 | Laura | Gómez   | Electricidad  | 2           |
      | prov-002 | Juan  | Pérez   | Plomería      | 1           |
      | prov-003 | Pedro | Dib     | Plomería      | 1           |
    And I am on the consumer home

  Scenario: View providers for a category with one provider
    When I tap the "Electricidad" category card
    Then I am taken to the providers list for category "Electricidad"
    And I see the provider "Laura Gómez" for category "Electricidad"

  @wip
  Scenario: View providers for a category with multiple providers
    When I tap the "Plomería" category card
    Then I am taken to the providers list for category "Plomería"
    And I see the provider "Juan Pérez" for category "Plomería"
    And I see the provider "Pedro Dib" for category "Plomería"

  @wip
  Scenario: Empty state when no providers exist for the category
    Given no providers exist for category "Gas"
    When I tap the "Gas" category card
    Then I am taken to the providers list for category "Gas"
    And I see the empty message "No se encontraron profesionales especializados en esta categoría"

  @wip
  Scenario: Error when the providers endpoint fails
    Given the providers endpoint will fail with a network error
    When I tap the "Electricidad" category card
    Then I am taken to the providers list for category "Electricidad"
    And I see the error message "No pudimos cargar los profesionales. Revisá tu conexión e intentá de nuevo."
