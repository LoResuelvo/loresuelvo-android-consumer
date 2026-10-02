package com.loresuelvo.consumer.bdd.diagnosis

import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberOptions
import org.junit.runner.RunWith

@RunWith(Cucumber::class)
@CucumberOptions(
    features = ["classpath:features/diagnosis/attach-images-for-pre-diagnosis.feature"],
    glue = ["com.loresuelvo.consumer.bdd.diagnosis"],
    plugin = ["pretty", "summary"],
)
class AttachImagesForPreDiagnosisCucumberTest
