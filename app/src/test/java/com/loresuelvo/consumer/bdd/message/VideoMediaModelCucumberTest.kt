package com.loresuelvo.consumer.bdd.message

import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberOptions
import org.junit.runner.RunWith

/** Executable specification for the Android video media model. */
@RunWith(Cucumber::class)
@CucumberOptions(
    features = ["classpath:features/message/video-media-model.feature"],
    glue = ["com.loresuelvo.consumer.bdd.message"],
    plugin = ["pretty", "summary"],
)
class VideoMediaModelCucumberTest
