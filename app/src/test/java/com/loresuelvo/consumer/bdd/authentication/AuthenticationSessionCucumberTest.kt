package com.loresuelvo.consumer.bdd.authentication

import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberOptions
import org.junit.runner.RunWith

@RunWith(Cucumber::class)
@CucumberOptions(
    features = ["classpath:features/auth/authentication-session.feature"],
    glue = ["com.loresuelvo.consumer.bdd.authentication"],
    plugin = ["pretty", "summary"],
)
class AuthenticationSessionCucumberTest
