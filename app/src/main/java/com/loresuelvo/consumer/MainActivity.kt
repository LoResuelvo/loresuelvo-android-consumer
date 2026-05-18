package com.loresuelvo.consumer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.loresuelvo.consumer.data.auth.Auth0AuthProvider
import com.loresuelvo.consumer.ui.screens.auth.WelcomeScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var authenticatedUserName by remember { mutableStateOf<String?>(null) }
            val authProvider = remember {
                Auth0AuthProvider(
                    context = this,
                    onAuthenticated = { user ->
                        runOnUiThread {
                            authenticatedUserName = user.name
                        }
                    }
                )
            }

            WelcomeScreen(
                authenticatedUserName = authenticatedUserName,
                onRegisterClick = authProvider::signup
            )
        }
    }
}
