package com.loresuelvo.consumer.bdd.providers.profilephoto

import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberOptions
import org.junit.runner.RunWith

@RunWith(Cucumber::class)
@CucumberOptions(
    features = ["classpath:features/provider/view-provider-profile-photo.feature"],
    glue = ["com.loresuelvo.consumer.bdd.providers.profilephoto"],
    plugin = ["pretty", "summary"],
)
class ViewProviderProfilePhotoCucumberTest
