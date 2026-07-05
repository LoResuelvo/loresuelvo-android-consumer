package com.loresuelvo.consumer.domain.auth

interface AuthProvider {

    suspend fun signup(): SignupOutcome
}