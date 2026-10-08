package com.loresuelvo.consumer.bdd.notifications

import io.cucumber.core.cli.Main
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ConsumerPushNotificationsCucumberTest {

    @Test
    fun legacy_notification_scenarios_run_without_exact_navigation() {
        val report = File("build/reports/cucumber/us20-legacy-notifications.json")
        assertTrue(report.parentFile?.mkdirs() == true || report.parentFile?.isDirectory == true)
        report.delete()

        val result = Main.run(
            arrayOf(
                "--glue",
                "com.loresuelvo.consumer.bdd.notifications",
                "--plugin",
                "summary",
                "--plugin",
                "json:${report.absolutePath}",
                "--name",
                "^0[1-8]-CPN .*",
                "--tags",
                "not @wip",
                "classpath:features/notifications/consumer-push-notifications.feature",
            ),
            Thread.currentThread().contextClassLoader,
        )

        assertEquals(0, result.toInt())
        val exampleNames = readExampleNames(report)
        assertEquals(20, exampleNames.size)
        assertTrue(exampleNames.all(::isLegacyExample))
    }

    private fun readExampleNames(report: File): List<String> {
        assertTrue(report.isFile)
        val features = Json.parseToJsonElement(report.readText()).jsonArray
        return features.flatMap { feature ->
                val elements = feature.jsonObject["elements"]?.jsonArray ?: return@flatMap emptyList()
                elements.map { it.jsonObject["name"]?.jsonPrimitive?.contentOrNull.orEmpty() }
            }
    }

    private fun isLegacyExample(name: String): Boolean {
        val scenarioNumber = name.substringBefore("-CPN").toIntOrNull() ?: return false
        return scenarioNumber in 1..8
    }
}
