package com.loresuelvo.consumer.bdd.message

import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberOptions
import org.junit.runner.RunWith

/**
 * JUnit 4 entry point for the Cucumber JVM scenarios in
 * `src/test/resources/features/message/send-media.feature`.
 *
 * Mirrors the convention documented at the top of the feature
 * file: every scenario starts `@wip`. The
 * `cucumber.filter.tags` system property is set globally in
 * `app/build.gradle.kts` to `not @wip`, so only the green
 * scenarios execute. Each commit removes the `@wip` marker
 * from exactly one scenario.
 *
 * The `features` path is the SPECIFIC file (not the directory)
 * to avoid the cross-runner duplication bug we fixed for
 * `SendMessagesCucumberTest` — see commit history on that one.
 */
@RunWith(Cucumber::class)
@CucumberOptions(
    features = ["classpath:features/message/send-media.feature"],
    glue = ["com.loresuelvo.consumer.bdd.message"],
    plugin = ["pretty", "summary"],
)
class SendMediaCucumberTest