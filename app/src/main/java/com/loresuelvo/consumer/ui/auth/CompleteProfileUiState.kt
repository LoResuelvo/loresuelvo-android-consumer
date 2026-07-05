package com.loresuelvo.consumer.ui.auth

data class CompleteProfileUiState(
    val firstName: String = "",
    val lastName: String = "",
    val error: String? = null,
)