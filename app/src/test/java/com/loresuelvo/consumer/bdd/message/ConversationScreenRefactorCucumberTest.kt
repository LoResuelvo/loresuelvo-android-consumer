package com.loresuelvo.consumer.bdd.message

import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberOptions
import org.junit.runner.RunWith

/**
 * Executable specification for the ConversationScreen modularization.
 * The scenarios stay under @wip until the grouped contracts are wired
 * through the production route and their regression assertions are green.
 */
@RunWith(Cucumber::class)
@CucumberOptions(
    features = ["classpath:features/message/conversation-screen-refactor.feature"],
    glue = ["com.loresuelvo.consumer.bdd.message"],
    plugin = ["pretty", "summary"],
)
class ConversationScreenRefactorCucumberTest
