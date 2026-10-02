package com.loresuelvo.consumer.bdd.navigation

import com.loresuelvo.consumer.ui.components.bottomnav.BottomDestination
import com.loresuelvo.consumer.ui.navigation.Route
import com.loresuelvo.consumer.ui.navigation.paymentResultPathFor

class NavigationWorld {

    var currentRoute: String = Route.Home.path
        private set

    var conversationRoute: String? = null
        private set

    fun processPaymentReturn(externalReference: String) {
        currentRoute = paymentResultPathFor(
            path = Route.PAYMENT_RETURN_SUCCESS_PATH,
            externalReference = externalReference,
        ) ?: error("payment return was not mapped to the internal route")
    }

    fun openConversation(conversationId: String) {
        conversationRoute = Route.Conversation.buildPath(conversationId)
    }

    fun recomposeConversation() {
        // A stable conversation id is the key used by the route host's
        // LaunchedEffect and remembered ActivityResult launchers.
    }

    fun assertPaymentResult(externalReference: String) {
        check(currentRoute == Route.PaymentResult.buildPath(externalReference)) {
            "expected payment result route, got $currentRoute"
        }
    }

    fun assertBottomBarHidden() {
        check(!BottomDestination.shouldShow(currentRoute)) {
            "bottom bar should be hidden for $currentRoute"
        }
    }

    fun assertConversationStable(conversationId: String) {
        check(conversationRoute == Route.Conversation.buildPath(conversationId)) {
            "conversation route changed after recomposition: $conversationRoute"
        }
    }
}
