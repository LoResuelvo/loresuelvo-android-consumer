package com.loresuelvo.consumer.bdd.notifications

import android.app.Application
import io.cucumber.core.cli.Main
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class ConsumerAccountSwitchCucumberTest {

    @Test
    fun scenario12_runsAllExamples() {
        val arguments = arrayOf(
            "--glue",
            "com.loresuelvo.consumer.bdd.notifications",
            "--plugin",
            "summary",
            "--name",
            "^12-CPN Recibir solamente avisos de la cuenta actual después de reiniciar$",
            "classpath:features/notifications/consumer-push-notifications.feature",
        )
        val result = Main.run(arguments, Thread.currentThread().contextClassLoader)
        assertEquals(0, result.toInt())
    }
}
