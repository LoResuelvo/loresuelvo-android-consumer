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
    fun scenario09_runs_all_eight_examples() {
        val report = File("build/reports/cucumber/us20-exact-navigation.json")
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
                "^09-CPN Abrir el destino exacto desde un aviso vigente$",
                "classpath:features/notifications/consumer-push-notifications.feature",
            ),
            Thread.currentThread().contextClassLoader,
        )

        assertEquals(0, result.toInt())
        val exactExampleCount = if (report.isFile) {
            countExactExamples(report)
        } else {
            0
        }
        assertEquals(8, exactExampleCount)
    }

    private fun countExactExamples(report: File): Int {
        val features = JSONArray(report.readText())
        return (0 until features.length())
            .flatMap { featureIndex ->
                val elements = features.getJSONObject(featureIndex).optJSONArray("elements") ?: JSONArray()
                (0 until elements.length()).map(elements::getJSONObject)
            }
            .count { it.optString("name").startsWith("09-CPN Abrir el destino exacto desde un aviso vigente") }
    }
}
