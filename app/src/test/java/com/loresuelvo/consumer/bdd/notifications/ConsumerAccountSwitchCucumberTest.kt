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
class ConsumerAccountSwitchCucumberTest {

    @Test
    fun scenario12_runsAllExamples() {
        val report = File("build/reports/cucumber/us20-account-switch.json")
        assertTrue(report.parentFile?.mkdirs() == true || report.parentFile?.isDirectory == true)
        report.delete()
        val arguments = arrayOf(
            "--glue",
            "com.loresuelvo.consumer.bdd.notifications",
            "--plugin",
            "summary",
            "--plugin",
            "json:${report.absolutePath}",
            "--name",
            "^12-CPN Recibir solamente avisos de la cuenta actual después de reiniciar$",
            "classpath:features/notifications/consumer-push-notifications.feature",
        )
        val result = Main.run(arguments, Thread.currentThread().contextClassLoader)
        assertEquals(0, result.toInt())
        assertEquals(1, countExamples(report, "12-CPN Recibir solamente avisos de la cuenta actual después de reiniciar"))
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
