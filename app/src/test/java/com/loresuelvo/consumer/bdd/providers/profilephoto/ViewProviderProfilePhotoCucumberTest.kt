package com.loresuelvo.consumer.bdd.providers.profilephoto

import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberOptions
import org.junit.runner.RunWith

/**
 * JUnit 4 entry point for the Cucumber JVM scenarios in
 * `src/test/resources/features/provider/view-provider-profile-photo.feature`.
 * Each scenario in that file is reported as a single JUnit test by
 * Gradle's `testDevDebugUnitTest` task; the per-scenario glue lives
 * in [ViewProviderProfilePhotoSteps] and the per-scenario world in
 * [com.loresuelvo.consumer.bdd.providers.search.CucumberWorld]
 * (shared with `search-providers.feature`'s runner — they drive the
 * same `ProfessionalsViewModel` and just load different feature
 * files).
 *
 * The BDD layer exercises the [com.loresuelvo.consumer.ui.professional.ProfessionalsViewModel]
 * directly through a fake repository and session store. The UI /
 * Compose rendering surface is covered separately by unit tests in
 * `src/test/java/com/loresuelvo/consumer/ui/professional/`.
 */
@RunWith(Cucumber::class)
@CucumberOptions(
    features = ["classpath:features/provider/view-provider-profile-photo.feature"],
    glue = ["com.loresuelvo.consumer.bdd.providers.profilephoto"],
    plugin = ["pretty", "summary"],
)
class ViewProviderProfilePhotoCucumberTest