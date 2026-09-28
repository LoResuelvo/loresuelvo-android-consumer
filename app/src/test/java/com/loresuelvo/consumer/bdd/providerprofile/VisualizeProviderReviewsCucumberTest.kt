package com.loresuelvo.consumer.bdd.providerprofile

import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberOptions
import org.junit.runner.RunWith

@RunWith(Cucumber::class)
@CucumberOptions(
    features = ["classpath:features/provider/visualize-provider-reviews.feature"],
    glue = ["com.loresuelvo.consumer.bdd.providerprofile"],
    plugin = ["pretty", "summary"],
)
class VisualizeProviderReviewsCucumberTest
