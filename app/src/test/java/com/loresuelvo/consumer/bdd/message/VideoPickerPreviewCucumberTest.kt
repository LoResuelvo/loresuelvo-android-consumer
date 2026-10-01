package com.loresuelvo.consumer.bdd.message

import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberOptions
import org.junit.runner.RunWith

/** Executable specification for selecting and previewing chat videos. */
@RunWith(Cucumber::class)
@CucumberOptions(
    features = ["classpath:features/message/video-picker-preview.feature"],
    glue = ["com.loresuelvo.consumer.bdd.message"],
    plugin = ["pretty", "summary"],
)
class VideoPickerPreviewCucumberTest
