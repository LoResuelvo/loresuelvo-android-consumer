package com.loresuelvo.consumer.bdd.onboarding.registerconsumer

import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberOptions
import org.junit.runner.RunWith

/**
 * JUnit 4 entry point for the Cucumber JVM scenarios in
 * `src/test/resources/features/auth/`. Each scenario in that folder is
 * reported as a single JUnit test by Gradle's `testDevDebugUnitTest`
 * task; the per-scenario glue lives in [RegisterConsumerSteps].
 *
 * No Robolectric, no instrumentation, no emulator — the BDD layer
 * exercises the `CompleteProfileViewModel` directly through a fake
 * session store and repository (see [CucumberWorld]). The UI / Compose
 * rendering surface is covered separately by unit tests in
 * `src/test/java/com/loresuelvo/consumer/ui/auth/`.
 */
@RunWith(Cucumber::class)
@CucumberOptions(
    features = ["classpath:features/auth"],
    glue = ["com.loresuelvo.consumer.bdd.onboarding.registerconsumer"],
    plugin = ["pretty", "summary"],
)
class RegisterConsumerCucumberTest
