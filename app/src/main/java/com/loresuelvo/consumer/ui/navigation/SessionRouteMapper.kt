package com.loresuelvo.consumer.ui.navigation

import com.loresuelvo.consumer.ui.session.SessionUiState

object SessionRouteMapper {
    fun routeFor(state: SessionUiState): String = when {
        !state.authenticated -> Route.Welcome.path
        !state.profileCompleted -> Route.CompleteProfile.path
        else -> Route.Home.path
    }
}
