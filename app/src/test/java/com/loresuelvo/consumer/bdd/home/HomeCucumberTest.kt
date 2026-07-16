package com.loresuelvo.consumer.bdd.home

import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberOptions
import org.junit.runner.RunWith

/**
 * JUnit 4 entry point for the Cucumber JVM scenarios in
 * `src/test/resources/features/home/`. Each scenario in that folder
 * is reported as a single JUnit test by Gradle's
 * `testDevDebugUnitTest` task; the per-scenario glue lives in
 * [HomeSteps].
 *
 * The BDD layer exercises the [HomeViewModel] directly against a
 * fake [CategoryRepository]; the UI / Compose rendering surface is
 * covered separately by unit tests in `src/test/java/.../ui/home/`.
 */
@RunWith(Cucumber::class)
@CucumberOptions(
    features = ["classpath:features/home"],
    glue = ["com.loresuelvo.consumer.bdd.home"],
    plugin = ["pretty", "summary"],
)
class HomeCucumberTest