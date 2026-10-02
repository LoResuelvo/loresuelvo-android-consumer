package com.loresuelvo.consumer.bdd.provider

import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberOptions
import org.junit.runner.RunWith

@RunWith(Cucumber::class)
@CucumberOptions(
    features = ["classpath:features/provider/visualize-service-proposal.feature"],
    glue = ["com.loresuelvo.consumer.bdd.provider"],
    plugin = ["pretty", "summary"],
)
class VisualizeServiceProposalCucumberTest
