package com.loresuelvo.consumer.data.auth

import android.content.Context
import com.auth0.android.Auth0
import com.loresuelvo.consumer.BuildConfig
import com.loresuelvo.consumer.domain.auth.AuthProvider

class Auth0AuthProvider(
    private val context: Context,
    private val webAuthLauncher: Auth0WebAuthLauncher = Auth0SdkWebAuthLauncher(
        account = Auth0(
            BuildConfig.AUTH0_CLIENT_ID,
            BuildConfig.AUTH0_DOMAIN
        ),
        scheme = BuildConfig.AUTH0_SCHEME
    )
) : AuthProvider {

    override fun signup() {
        webAuthLauncher.startSignup(context)
    }
}
