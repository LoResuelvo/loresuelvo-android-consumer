package com.loresuelvo.consumer.bdd.notifications

import io.cucumber.java.After
import io.cucumber.java.es.Cuando
import io.cucumber.java.es.Dado
import io.cucumber.java.es.Entonces
import org.junit.Assert.assertNull

class ConsumerPushNotificationsSteps {
    private val world = ConsumerPushNotificationsWorld()

    @Dado("que todavía no inicié sesión en este teléfono")
    fun startWithoutSession() {
        world.start()
        assertNull(world.session())
    }

    @Cuando("inicio sesión y LoResuelvo verifica mi cuenta de consumidor")
    fun loginWithVerifiedConsumerAccount() {
        world.login()
    }

    @Entonces("este teléfono queda habilitado para recibir avisos de mi cuenta")
    fun phoneIsRegisteredForCurrentAccount() {
        world.assertPhoneRegistered()
    }

    @After
    fun close() {
        world.close()
    }
}
