package com.loresuelvo.consumer.platform.auth

import android.content.Context
import com.loresuelvo.consumer.domain.auth.AuthenticationOutcome
import com.loresuelvo.consumer.domain.auth.LogoutOutcome

/**
 * Application boundary for the identity-provider SDK.
 *
 * The Activity-bound [Context] is intentional: Auth0 launches an
 * external browser/custom-tab flow and therefore cannot use an
 * application context. Keeping this contract under `platform.auth`
 * prevents Android lifecycle concerns from leaking into `domain`.
 */
interface AuthProvider {
    suspend fun login(context: Context): AuthenticationOutcome
    suspend fun signup(context: Context): AuthenticationOutcome
    suspend fun loginWithGoogle(context: Context): AuthenticationOutcome
    suspend fun logout(context: Context): LogoutOutcome
}
