package com.loresuelvo.consumer.bdd.home

import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberOptions
import org.junit.runner.RunWith

@RunWith(Cucumber::class)
@CucumberOptions(
    features = ["classpath:features/home"],
    glue = ["com.loresuelvo.consumer.bdd.home"],
    plugin = ["pretty", "summary"],
)
class HomeCucumberTest
