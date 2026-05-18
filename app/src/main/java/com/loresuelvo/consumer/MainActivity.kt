package com.loresuelvo.consumer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.loresuelvo.consumer.data.auth.Auth0AuthProvider
import com.loresuelvo.consumer.data.auth.SharedPreferencesAuthSessionStore
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.ui.screens.auth.WelcomeScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val sessionStore = remember {
                SharedPreferencesAuthSessionStore(this)
            }
            var authSession by remember {
                mutableStateOf<AuthSession?>(sessionStore.getSession())
            }
            val authProvider = remember {
                Auth0AuthProvider(
                    context = this,
                    onAuthenticated = { session ->
                        sessionStore.saveSession(session)
                        runOnUiThread {
                            authSession = session
                        }
                    }
                )
            }

            WelcomeScreen(
                authSession = authSession,
                onRegisterClick = authProvider::signup
            )
        }
    }
}
