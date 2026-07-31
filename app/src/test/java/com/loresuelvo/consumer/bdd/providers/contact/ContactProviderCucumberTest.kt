package com.loresuelvo.consumer.bdd.providers.contact

import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberOptions
import org.junit.runner.RunWith

/**
 * JUnit 4 entry point for the Cucumber JVM scenarios in
 * `src/test/resources/features/provider/contact-provider.feature`.
 * The per-scenario glue lives in [PendingSteps] (placeholder) and,
 * once the scenarios land, in `ContactProviderSteps.kt`.
 *
 * Mirrors the convention documented at the top of the feature
 * file: both scenarios start `@wip`. The `cucumber.filter.tags`
 * system property is set globally in `app/build.gradle.kts` to
 * `not @wip`, so only the green scenarios execute. Each commit
 * removes the `@wip` marker from exactly one scenario.
 *
 * The `glue` package list includes the `search` package so the
 * shared Background steps (`I am logged in as a consumer`,
 * `the following categories exist:`, `the following providers
 * exist:`, `I tap the {string} category card`) are reachable
 * without duplicating the world setup across BDD packages.
 */
@RunWith(Cucumber::class)
@CucumberOptions(
    features = ["classpath:features/provider/contact-provider.feature"],
    glue = [
        "com.loresuelvo.consumer.bdd.providers.contact",
        "com.loresuelvo.consumer.bdd.providers.search",
    ],
    plugin = ["pretty", "summary"],
)
class ContactProviderCucumberTest
