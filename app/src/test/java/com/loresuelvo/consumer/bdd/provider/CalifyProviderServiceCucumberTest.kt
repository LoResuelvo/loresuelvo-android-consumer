package com.loresuelvo.consumer.bdd.provider

import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberOptions
import org.junit.runner.RunWith

/**
 * JUnit 4 entry point for the Cucumber JVM scenarios in
 * `src/test/resources/features/work_order/calify-provider-service.feature`
 * (US-30 "Calificar servicio"). The per-scenario glue lives in
 * [CalifyProviderServiceSteps]; the world (and the fake
 * repositories) live in [CalifyProviderServiceWorld].
 *
 * The `features` path is the SPECIFIC file (not the directory)
 * to avoid cross-runner duplication. The global filter
 * `cucumber.filter.tags = "not @wip"` set in `app/build.gradle.kts`
 * skips the not-yet-implemented scenarios — every commit in
 * this work stream removes the `@wip` marker from exactly one
 * scenario, after which the step definitions needed by that
 * scenario land in `CalifyProviderServiceSteps.kt`.
 */
@RunWith(Cucumber::class)
@CucumberOptions(
    features = ["classpath:features/work_order/calify-provider-service.feature"],
    glue = ["com.loresuelvo.consumer.bdd.provider"],
    plugin = ["pretty", "summary"],
)
class CalifyProviderServiceCucumberTest
