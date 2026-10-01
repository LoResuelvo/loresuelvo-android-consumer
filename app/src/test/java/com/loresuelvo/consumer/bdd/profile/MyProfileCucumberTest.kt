package com.loresuelvo.consumer.bdd.profile

import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberOptions
import org.junit.runner.RunWith

@RunWith(Cucumber::class)
@CucumberOptions(
    features = ["classpath:features/profile/my-profile.feature"],
    glue = ["com.loresuelvo.consumer.bdd.profile"],
    plugin = ["pretty", "summary"],
)
class MyProfileCucumberTest
