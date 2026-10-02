package com.loresuelvo.consumer.bdd.providers.search

import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberOptions
import org.junit.runner.RunWith

@RunWith(Cucumber::class)
@CucumberOptions(
    features = ["classpath:features/provider/search-providers.feature"],
    glue = ["com.loresuelvo.consumer.bdd.providers.search"],
    plugin = ["pretty", "summary"],
)
class SearchProvidersCucumberTest
