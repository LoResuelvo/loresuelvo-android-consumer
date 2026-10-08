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
class ConsumerSessionLogoutCucumberTest {

    @Test
    fun scenario11_runsAllExamples() {
        val arguments = arrayOf(
            "--glue",
            "com.loresuelvo.consumer.bdd.notifications",
            "--plugin",
            "summary",
            "--name",
            "^11-CPN Dejar de recibir avisos al cerrar sesión$",
            "classpath:features/notifications/consumer-push-notifications.feature",
        )
        val result = Main.run(arguments, Thread.currentThread().contextClassLoader)
        assertEquals(0, result.toInt())
    }
}
