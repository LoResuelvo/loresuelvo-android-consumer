package com.loresuelvo.consumer.domain.auth

data class User(
    val displayName: String,
    val email: String? = null
)
