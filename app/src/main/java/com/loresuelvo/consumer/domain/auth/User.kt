package com.loresuelvo.consumer.domain.auth

data class User(
    val displayName: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null
) {

    fun isProfileComplete(): Boolean =
        !firstName.isNullOrBlank() &&
                !lastName.isNullOrBlank()
}