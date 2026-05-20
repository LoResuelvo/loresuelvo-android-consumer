package com.loresuelvo.consumer.domain.auth

data class AuthSession(
    val user: User,
    val accessToken: String
)