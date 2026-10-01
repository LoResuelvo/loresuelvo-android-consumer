package com.loresuelvo.consumer.bdd.message

import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberOptions
import org.junit.runner.RunWith

/** Executable specification for the provider-chat video player flow. */
@RunWith(Cucumber::class)
@CucumberOptions(
    features = ["classpath:features/message/video-playback.feature"],
    glue = ["com.loresuelvo.consumer.bdd.message"],
    plugin = ["pretty", "summary"],
)
class VideoPlaybackCucumberTest
