package com.loresuelvo.consumer.bdd.navigation

import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberOptions
import org.junit.runner.RunWith

/**
 * Executable specification for the navigation composition refactor.
 * Scenarios start as `@wip` while their isolated route-host assertions
 * are implemented; Gradle's global Cucumber filter validates the feature
 * without running unfinished glue.
 */
@RunWith(Cucumber::class)
@CucumberOptions(
    features = ["classpath:features/navigation/navigation.feature"],
    glue = ["com.loresuelvo.consumer.bdd.navigation"],
    plugin = ["pretty", "summary"],
)
class NavigationCucumberTest
