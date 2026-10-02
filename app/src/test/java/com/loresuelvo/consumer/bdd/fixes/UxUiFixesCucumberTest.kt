package com.loresuelvo.consumer.bdd.fixes

import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberOptions
import org.junit.runner.RunWith

@RunWith(Cucumber::class)
@CucumberOptions(
    features = ["classpath:features/fixes/ux_ui_fixes.feature"],
    glue = ["com.loresuelvo.consumer.bdd.fixes"],
    plugin = ["pretty", "summary"],
    // Tag filter `not @wip` is set globally via the system property
    // `cucumber.filter.tags` in `app/build.gradle.kts`. Pending
    // and throw `io.cucumber.java.PendingException` so the JUnit
    // run classifies them as pending (skipped), not failing.
)
class UxUiFixesCucumberTest
