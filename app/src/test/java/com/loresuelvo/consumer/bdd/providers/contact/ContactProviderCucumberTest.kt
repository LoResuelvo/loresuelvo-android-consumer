package com.loresuelvo.consumer.bdd.providers.contact

import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberOptions
import org.junit.runner.RunWith

@RunWith(Cucumber::class)
@CucumberOptions(
    features = ["classpath:features/provider/contact-provider.feature"],
    glue = [
        "com.loresuelvo.consumer.bdd.providers.contact",
        "com.loresuelvo.consumer.bdd.providers.search",
    ],
    plugin = ["pretty", "summary"],
)
class ContactProviderCucumberTest
