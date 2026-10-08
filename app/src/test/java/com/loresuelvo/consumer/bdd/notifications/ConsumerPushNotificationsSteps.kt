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
    private val permissions = ConsumerNotificationPermissionWorld()
    private val settings = ConsumerSettingsNotificationsWorld()
    private val recovery = ConsumerRecoveryNotificationsWorld()
    private val visibleChat = ConsumerVisibleChatWorld()
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

    @Dado("que uso Android 13 o posterior y no decidí el permiso de avisos")
    fun startWithAndroid13Undecided() = permissions.startWithAndroid13Undecided()

    @Dado("solicité habilitar los avisos desde LoResuelvo")
    fun requestEnablingNotifications() = permissions.requestEnablingNotifications()

    @Cuando("{string} el permiso de notificaciones del teléfono")
    fun decideNotificationPermission(decision: String) = permissions.decidePermission(decision)

    @Entonces("puedo seguir consultando mis mensajes y servicios")
    fun canConsultMessagesAndServices() = permissions.assertCanConsultMessagesAndServices()

    @Entonces("no se vuelve a pedir el permiso automáticamente al abrir LoResuelvo")
    fun notPromptedAutomatically() = permissions.assertNotPromptedAutomatically()

    @Entonces("puedo abrir los ajustes de notificaciones del teléfono desde la aplicación")
    fun canOpenNotificationSettings() = permissions.assertCanOpenNotificationSettings()

    @Dado("que tengo una sesión activa")
    fun activeSession() = settings.start()

    @Dado("{string}")
    fun configureNotificationAdjustment(adjustment: String) = settings.configureAdjustment(adjustment)

    @Cuando("llega un aviso válido de {string}")
    fun receiveValidNotice(noticeType: String) = settings.receiveValidNotice(noticeType)

    @Entonces("no aparece una notificación del teléfono para ese aviso")
    fun assertNoSystemNotificationPublished() = settings.assertNoNotificationPublished()

    @Entonces("puedo consultar la novedad dentro de LoResuelvo")
    fun assertCanConsultNoveltyInsideApp() = settings.assertCanConsultNoveltyInsideApp()

    @Dado("que tengo una sesión verificada de consumidor")
    fun startWithVerifiedSession() = recovery.startWithVerifiedSession()

    @Dado("{string} al habilitar los avisos")
    fun interruptionWhenEnabling(interruption: String) = recovery.interruptionWhenEnabling(interruption)

    @Cuando("vuelvo a usar LoResuelvo con conexión disponible")
    fun resumeWithConnectionAvailable() = recovery.resumeWithConnectionAvailable()

    @Entonces("el teléfono queda habilitado para recibir próximos avisos de mi cuenta")
    fun phoneRegisteredAfterRecovery() = recovery.assertPhoneRegistered()

    @Entonces("puedo consultar mis mensajes y servicios durante la recuperación")
    fun canConsultMessagesAndServicesDuringRecovery() = recovery.assertCanConsultMessagesAndServices()

    @Dado("que estoy leyendo mi conversación con el prestador")
    fun readingConversationWithProvider() = visibleChat.startReadingConversation()

    @Dado("tengo un borrador sin enviar y una posición de lectura elegida")
    fun draftAndReadingPositionChosen() = visibleChat.setDraftAndReadingPosition()

    @Cuando("llega el aviso de un nuevo mensaje del prestador")
    fun incomingProviderMessageNotice() = visibleChat.incomingProviderMessageNotice()

    @Entonces("la conversación se actualiza sin una notificación del teléfono ni un popup adicional")
    fun conversationUpdatedWithoutNotificationOrPopup() = visibleChat.assertUpdatedWithoutNotificationOrPopup()

    @Entonces("conservo mi borrador y mi posición de lectura")
    fun preserveDraftAndReadingPosition() = visibleChat.assertDraftAndReadingPositionPreserved()

    @After
    fun close() {
        if (registrationStarted) world.close()
        recovery.close()
        visibleChat.close()
    }
}
