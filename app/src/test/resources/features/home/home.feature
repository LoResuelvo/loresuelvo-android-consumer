Feature: Consumer Home

  Background:
    Given I am logged in as a consumer

  Scenario: Home shows only the first 6 categories, alphabetically ordered
    Given the backend exposes the following categories:
      | id | name             |
      |  1 | Plomería         |
      |  2 | Albañilería      |
      |  3 | Gasista          |
      |  4 | Electricista     |
      |  5 | Climatización    |
      |  6 | Pintura          |
      |  7 | Carpintería      |
      |  8 | Herrería         |
    When the consumer opens the Home screen
    Then the visible categories are exactly these 6, in alphabetical order:
      | name             |
      | Albañilería      |
      | Carpintería      |
      | Climatización    |
      | Electricista     |
      | Gasista          |
      | Herrería         |