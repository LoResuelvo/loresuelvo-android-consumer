package com.loresuelvo.consumer.bdd.providers.search

import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberOptions
import org.junit.runner.RunWith

/**
 * JUnit 4 entry point for the Cucumber JVM scenarios in
 * `src/test/resources/features/provider/`. Each scenario in that
 * folder is reported as a single JUnit test by Gradle's
 * `testDevDebugUnitTest` task; the per-scenario glue lives in
 * [SearchProvidersSteps].
 *
 * The BDD layer exercises the [ProfessionalsViewModel] directly
 * through a fake repository and session store (see
 * [com.loresuelvo.consumer.bdd.providers.search.CucumberWorld]).
 * The UI / Compose rendering surface is covered separately by unit
 * tests in `src/test/java/com/loresuelvo/consumer/ui/professional/`.
 */
@RunWith(Cucumber::class)
@CucumberOptions(
    features = ["classpath:features/provider"],
    glue = ["com.loresuelvo.consumer.bdd.providers.search"],
    plugin = ["pretty", "summary"],
)
class SearchProvidersCucumberTest
