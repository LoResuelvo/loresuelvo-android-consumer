package com.loresuelvo.consumer.bdd.diagnosis

import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberOptions
import org.junit.runner.RunWith

/**
 * JUnit 4 entry point for the Cucumber JVM scenarios in
 * `src/test/resources/features/diagnosis/attach-images-for-pre-diagnosis.feature`.
 * Per-scenario glue lives in [AiDiagnosisSteps] + [AiDiagnosisWorld].
 *
 * Each scenario starts `@wip`; this runner uses the global
 * `cucumber.filter.tags = ~@wip` (set in `app/build.gradle.kts`).
 * Each commit removes the `@wip` marker from exactly one
 * scenario and adds its step implementations.
 *
 * The BDD layer drives [com.loresuelvo.consumer.ui.screens.chat.ChatViewModel]
 * directly against a relaxed `MediaReader` mock — no Hilt, no
 * Compose, no network. The visible picker is verified by the
 * Compose acceptance test in `src/androidTest/.../acceptance/diagnosis/`.
 */
@RunWith(Cucumber::class)
@CucumberOptions(
    features = ["classpath:features/diagnosis/attach-images-for-pre-diagnosis.feature"],
    glue = ["com.loresuelvo.consumer.bdd.diagnosis"],
    plugin = ["pretty", "summary"],
)
class AttachImagesForPreDiagnosisCucumberTest
