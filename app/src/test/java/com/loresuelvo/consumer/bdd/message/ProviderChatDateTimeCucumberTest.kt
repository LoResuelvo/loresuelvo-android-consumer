package com.loresuelvo.consumer.bdd.message

import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberOptions
import org.junit.runner.RunWith

@RunWith(Cucumber::class)
@CucumberOptions(
    features = ["classpath:features/message/provider-chat-date-time.feature"],
    glue = ["com.loresuelvo.consumer.bdd.message"],
    plugin = ["pretty", "summary"],
)
class ProviderChatDateTimeCucumberTest
