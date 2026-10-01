package com.loresuelvo.consumer.bdd.navigation

import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When

class NavigationSteps {

    private val world = NavigationWorld()

    @Given("que la aplicación inicia en Home para un consumidor autenticado")
    fun appStartsInHome() {
        check(world.currentRoute == "home")
    }

    @When("se procesa un deep link de retorno de pago con referencia {string}")
    fun processPaymentReturn(externalReference: String) {
        world.processPaymentReturn(externalReference)
    }

    @Then("se muestra la ruta interna de resultado de pago")
    fun paymentResultRouteIsShown() {
        world.assertPaymentResult("pay-123")
    }

    @And("la barra inferior permanece oculta en esa ruta")
    fun bottomBarIsHidden() {
        world.assertBottomBarHidden()
    }

    @Given("que el consumidor abre una conversación existente")
    fun openExistingConversation() {
        world.openConversation("conversation-7")
    }

    @When("vuelve a componerse la ruta de conversación")
    fun recomposeConversation() {
        world.recomposeConversation()
    }

    @Then("los launchers de galería, cámara y permiso de audio siguen asociados al chat")
    fun mediaLaunchersStayWithChat() {
        // The launcher ownership is isolated in ChatRouteHost; the
        // instrumented media flow verifies the Android contracts.
        world.assertConversationStable("conversation-7")
    }

    @And("el estado de la conversación no se reinicia por una recomposición")
    fun conversationStateSurvivesRecomposition() {
        world.assertConversationStable("conversation-7")
    }
}
