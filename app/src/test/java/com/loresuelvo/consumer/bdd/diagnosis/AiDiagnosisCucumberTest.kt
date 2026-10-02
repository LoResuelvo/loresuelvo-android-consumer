package com.loresuelvo.consumer.bdd.diagnosis

import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberOptions
import org.junit.runner.RunWith

@RunWith(Cucumber::class)
@CucumberOptions(
    features = ["classpath:features/diagnosis/ai_diagnosis.feature"],
    glue = ["com.loresuelvo.consumer.bdd.diagnosis"],
    plugin = ["pretty", "summary"],
    // Tag filter `not @wip` is set globally via the system property
    // `cucumber.filter.tags` in `app/build.gradle.kts`. Pending steps
    // `io.cucumber.java.PendingException` so the JUnit run classifies
    // them as pending (skipped), not failing.
)
class AiDiagnosisCucumberTest
