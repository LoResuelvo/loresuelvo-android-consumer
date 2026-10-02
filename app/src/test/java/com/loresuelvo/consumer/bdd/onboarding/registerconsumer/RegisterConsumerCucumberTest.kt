package com.loresuelvo.consumer.bdd.onboarding.registerconsumer

import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberOptions
import org.junit.runner.RunWith

@RunWith(Cucumber::class)
@CucumberOptions(
    features = ["classpath:features/auth/register-consumer.feature"],
    glue = ["com.loresuelvo.consumer.bdd.onboarding.registerconsumer"],
    plugin = ["pretty", "summary"],
)
class RegisterConsumerCucumberTest
