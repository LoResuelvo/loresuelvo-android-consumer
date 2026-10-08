package com.loresuelvo.consumer.bdd.notifications

import android.app.Application
import io.cucumber.core.cli.Main
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class ConsumerExactNavigationRobolectricTest {

    @Test
    fun scenarios09And10_runAllExamplesInOneCucumberSession() {
        val report = File("build/reports/cucumber/us20-exact-notification-resolution.json")
        assertTrue(report.parentFile?.mkdirs() == true || report.parentFile?.isDirectory == true)
        report.delete()

        val arguments = arrayOf(
            "--glue",
            "com.loresuelvo.consumer.bdd.notifications",
            "--plugin",
            "summary",
            "--plugin",
            "json:${report.absolutePath}",
            "--tags",
            "@US-20",
            "--name",
            "^(09-CPN Abrir el destino exacto desde un aviso vigente|10-CPN Resolver un aviso que no puedo abrir)$",
            "classpath:features/notifications/consumer-push-notifications.feature",
        )

        val result = Main.run(arguments, Thread.currentThread().contextClassLoader)
        assertEquals(0, result.toInt())
        assertEquals(8, countExamples(report, "09-CPN Abrir el destino exacto desde un aviso vigente"))
        assertEquals(4, countExamples(report, "10-CPN Resolver un aviso que no puedo abrir"))
    }

    private fun countExamples(report: File, scenarioPrefix: String): Int {
        val features = JSONArray(report.readText())
        return (0 until features.length())
            .flatMap { featureIndex ->
                val elements = features.getJSONObject(featureIndex).optJSONArray("elements") ?: JSONArray()
                (0 until elements.length()).map(elements::getJSONObject)
            }
            .count { it.optString("name").startsWith(scenarioPrefix) }
    }
}
