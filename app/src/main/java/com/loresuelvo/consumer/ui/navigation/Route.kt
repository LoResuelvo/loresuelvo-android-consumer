package com.loresuelvo.consumer.ui.navigation

sealed class Route(val path: String) {
    data object Welcome : Route("welcome")
    data object CompleteProfile : Route("complete_profile")
    data object Home : Route("home")
}