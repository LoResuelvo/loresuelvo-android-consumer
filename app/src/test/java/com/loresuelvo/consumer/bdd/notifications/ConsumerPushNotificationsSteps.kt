package com.loresuelvo.consumer.bdd.notifications

import io.cucumber.java.After
import io.cucumber.java.es.Cuando
import io.cucumber.java.es.Dado
import io.cucumber.java.es.Entonces
import org.junit.Assert.assertNull

class ConsumerPushNotificationsSteps {
    private val world = ConsumerPushNotificationsWorld()
    private val messages = ConsumerMessageNotificationsWorld()
    private val services = ConsumerServiceNotificationsWorld()
    private var registrationStarted = false

    @Dado("que todavía no inicié sesión en este teléfono")
    fun startWithoutSession() {
        registrationStarted = true
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

    @Dado("que tengo una sesión activa y permití los avisos en este teléfono")
    fun activeSessionWithNotificationsAllowed() {
        messages.start()
        services.start()
    }

    @Dado("estoy {string}")
    fun outsideVisibleChat(situation: String) = messages.setSituation(situation)

    @Cuando("llega un aviso de mensaje con {string} del prestador")
    fun receiveProviderMessage(content: String) = messages.receive(content)

    @Entonces("veo un único aviso de nuevo mensaje que permite abrir esa conversación")
    fun seeOneMessageNotification() = messages.assertPublished()

    @Entonces("el aviso no expone contenido del mensaje ni datos personales")
    fun notificationPreservesPrivacy() = messages.assertPrivate()

    @Dado("no estoy usando LoResuelvo")
    fun notUsingLoResuelvo() = services.notUsingLoResuelvo()

    @Cuando("llega un aviso de {string} de uno de mis servicios")
    fun receiveServiceNotice(novelty: String) = services.receive(novelty)

    @Entonces("veo un aviso de {string} que permite abrir {string}")
    fun seeServiceNotification(notice: String, destination: String) = services.assertPublished(notice, destination)

    @Entonces("el aviso no expone nombres, direcciones ni importes")
    fun serviceNotificationPreservesPrivacy() = services.assertPrivate()

    @After
    fun close() {
        if (registrationStarted) world.close()
    }
}
