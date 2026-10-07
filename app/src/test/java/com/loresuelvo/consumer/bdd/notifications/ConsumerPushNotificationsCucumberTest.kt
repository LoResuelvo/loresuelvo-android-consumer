package com.loresuelvo.consumer.bdd.notifications

import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberOptions
import org.junit.runner.RunWith

@RunWith(Cucumber::class)
@CucumberOptions(
    features = ["classpath:features/notifications/consumer-push-notifications.feature"],
    glue = ["com.loresuelvo.consumer.bdd.notifications"],
    name = ["^01-CPN .*"],
    plugin = ["pretty", "summary"],
)
class ConsumerPushNotificationsCucumberTest
