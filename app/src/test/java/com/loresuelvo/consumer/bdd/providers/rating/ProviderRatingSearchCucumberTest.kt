package com.loresuelvo.consumer.bdd.providers.rating

import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberOptions
import org.junit.runner.RunWith

@RunWith(Cucumber::class)
@CucumberOptions(
    features = ["classpath:features/provider/provider-rating-search.feature"],
    glue = ["com.loresuelvo.consumer.bdd.providers.rating"],
    plugin = ["pretty", "summary"],
)
class ProviderRatingSearchCucumberTest
