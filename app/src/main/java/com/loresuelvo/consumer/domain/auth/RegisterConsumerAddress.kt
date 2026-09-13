package com.loresuelvo.consumer.domain.auth

data class RegisterConsumerAddress(
    val street: String,
    val streetNumber: String,
    val floor: String = "",
    val unit: String = "",
)