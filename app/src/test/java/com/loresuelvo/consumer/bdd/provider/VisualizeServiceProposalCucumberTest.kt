package com.loresuelvo.consumer.bdd.provider

import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberOptions
import org.junit.runner.RunWith

/**
 * JUnit 4 entry point for the Cucumber JVM scenarios in
 * `src/test/resources/features/provider/visualize-service-proposal.feature`.
 *
 * The per-scenario glue lives in `VisualizeServiceProposalSteps.kt`
 * (today: scenario 01-VSP). The other 17 scenarios carry `@wip`
 * until each one lands in its own commit; the runner executes
 * only the green ones because
 * `cucumber.filter.tags = "not @wip"` is set globally in
 * `app/build.gradle.kts`.
 *
 * The `features` path is the SPECIFIC file (not the directory)
 * to avoid the cross-runner duplication bug we fixed for
 * `SearchProvidersCucumberTest` — see commit history on that one.
 */
@RunWith(Cucumber::class)
@CucumberOptions(
    features = ["classpath:features/provider/visualize-service-proposal.feature"],
    glue = ["com.loresuelvo.consumer.bdd.provider"],
    plugin = ["pretty", "summary"],
)
class VisualizeServiceProposalCucumberTest
