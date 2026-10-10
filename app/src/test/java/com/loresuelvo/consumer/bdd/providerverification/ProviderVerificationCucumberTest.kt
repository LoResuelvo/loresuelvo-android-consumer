package com.loresuelvo.consumer.bdd.providerverification

import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberOptions
import org.junit.runner.RunWith

@RunWith(Cucumber::class)
@CucumberOptions(
    features = ["classpath:features/provider/provider-verification-and-chat-profile.feature"],
    glue = ["com.loresuelvo.consumer.bdd.providerverification"],
    plugin = ["pretty", "summary"],
)
class ProviderVerificationCucumberTest
