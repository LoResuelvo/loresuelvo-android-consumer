package com.loresuelvo.consumer.bdd.fixes

import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberOptions
import org.junit.runner.RunWith

/**
 * JUnit 4 entry point for the Cucumber JVM scenarios in
 * `src/test/resources/features/fixes/ux_ui_fixes.feature`. The
 * per-scenario glue lives in [UxUiFixesSteps] (implemented) and
 * [UxUiFixesPendingSteps] (still-`@wip` placeholders).
 *
 * Mirrors the convention documented at the top of the feature file:
 * every scenario starts `@wip`; this runner uses `tags = "~@wip"`
 * (set globally via the system property `cucumber.filter.tags` in
 * `app/build.gradle.kts`) so only the green scenarios execute.
 * Each commit removes the `@wip` marker from exactly one scenario
 * and moves its step definition out of `PendingSteps`.
 *
 * The BDD layer drives
 * [com.loresuelvo.consumer.ui.screens.chat.ChatViewModel] directly
 * through [UxUiFixesWorld] — no Hilt, no Compose, no network.
 * Compose / acceptance coverage lives in `src/androidTest/...`.
 */
@RunWith(Cucumber::class)
@CucumberOptions(
    features = ["classpath:features/fixes/ux_ui_fixes.feature"],
    glue = ["com.loresuelvo.consumer.bdd.fixes"],
    plugin = ["pretty", "summary"],
    // Tag filter `not @wip` is set globally via the system property
    // `cucumber.filter.tags` in `app/build.gradle.kts`. Pending
    // steps for `@wip` scenarios live in [UxUiFixesPendingSteps]
    // and throw `io.cucumber.java.PendingException` so the JUnit
    // run classifies them as pending (skipped), not failing.
)
class UxUiFixesCucumberTest
