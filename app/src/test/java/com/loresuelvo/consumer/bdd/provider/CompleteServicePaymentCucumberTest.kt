package com.loresuelvo.consumer.bdd.provider

import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberOptions
import org.junit.runner.RunWith

/**
 * JUnit 4 entry point for the Cucumber JVM scenarios in
 * `src/test/resources/features/work_order/complete-service-payment.feature`
 * (US-21 "Confirmar acuerdo de servicio"). The per-scenario glue
 * lives in `CompleteServicePaymentSteps.kt`; the world (and the
 * fake repositories) live in `CompleteServicePaymentWorld.kt`.
 *
 * The `features` path is the SPECIFIC file (not the directory)
 * to avoid cross-runner duplication.
 */
@RunWith(Cucumber::class)
@CucumberOptions(
    features = ["classpath:features/work_order/complete-service-payment.feature"],
    glue = ["com.loresuelvo.consumer.bdd.provider"],
    plugin = ["pretty", "summary"],
)
class CompleteServicePaymentCucumberTest
