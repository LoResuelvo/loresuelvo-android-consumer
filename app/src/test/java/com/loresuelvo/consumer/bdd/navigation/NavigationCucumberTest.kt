package com.loresuelvo.consumer.bdd.navigation

import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberOptions
import org.junit.runner.RunWith

@RunWith(Cucumber::class)
@CucumberOptions(
    features = ["classpath:features/navigation/navigation.feature"],
    glue = ["com.loresuelvo.consumer.bdd.navigation"],
    plugin = ["pretty", "summary"],
)
class NavigationCucumberTest
