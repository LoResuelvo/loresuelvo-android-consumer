package com.loresuelvo.consumer.bdd.diagnosis

import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberOptions
import org.junit.runner.RunWith

/**
 * JUnit 4 entry point for the Cucumber JVM scenarios in
 * `src/test/resources/features/diagnosis/ai_diagnosis.feature`. The
 * per-scenario glue lives in [AiDiagnosisSteps].
 *
 * Mirrors the convention documented at the top of the feature file:
 * every scenario starts `@wip`; this runner uses `tags = "~@wip"`
 * so only the green scenarios execute. Each commit removes the
 * `@wip` marker from exactly one scenario.
 *
 * The BDD layer drives [com.loresuelvo.consumer.ui.screens.chat.ChatViewModel]
 * directly through [AiDiagnosisWorld] — no Hilt, no Compose, no
 * network. Compose / acceptance coverage lives in
 * `src/androidTest/.../acceptance/diagnosis/`.
 */
@RunWith(Cucumber::class)
@CucumberOptions(
    features = ["classpath:features/diagnosis/ai_diagnosis.feature"],
    glue = ["com.loresuelvo.consumer.bdd.diagnosis"],
    plugin = ["pretty", "summary"],
    // Tag filter `not @wip` is set globally via the system property
    // `cucumber.filter.tags` in `app/build.gradle.kts`. Pending steps
    // for `@wip` scenarios live in [PendingSteps] and throw
    // `io.cucumber.java.PendingException` so the JUnit run classifies
    // them as pending (skipped), not failing.
)
class AiDiagnosisCucumberTest
