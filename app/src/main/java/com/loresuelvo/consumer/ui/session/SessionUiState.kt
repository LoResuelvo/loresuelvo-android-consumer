package com.loresuelvo.consumer.ui.session

data class SessionUiState(
    val loading: Boolean = true,
    val authenticated: Boolean = false,
    val profileCompleted: Boolean = false,
)