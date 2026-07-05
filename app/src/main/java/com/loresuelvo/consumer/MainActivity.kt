package com.loresuelvo.consumer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.rememberNavController
import com.loresuelvo.consumer.data.auth.Auth0AuthProvider
import com.loresuelvo.consumer.data.auth.SharedPreferencesAuthSessionStore
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.ui.auth.WelcomeViewModel
import com.loresuelvo.consumer.ui.navigation.LoResuelvoNavHost
import com.loresuelvo.consumer.ui.navigation.Route
import com.loresuelvo.consumer.ui.screens.auth.CompleteProfileScreen
import com.loresuelvo.consumer.ui.screens.auth.WelcomeScreen
import com.loresuelvo.consumer.ui.screens.home.HomeScreen
import com.loresuelvo.consumer.ui.session.SessionViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val sessionStore = remember {
                SharedPreferencesAuthSessionStore(this)
            }

            val sessionViewModel: SessionViewModel by viewModels(
                factoryProducer = {
                    viewModelFactory {
                        initializer {
                            SessionViewModel(sessionStore)
                        }
                    }
                }
            )
            val sessionState by sessionViewModel.uiState.collectAsState()

            var authSession by remember {
                mutableStateOf<AuthSession?>(
                    sessionStore.getSession()
                )
            }

            var firstName by remember {
                mutableStateOf(
                    authSession?.user?.firstName ?: ""
                )
            }

            var lastName by remember {
                mutableStateOf(
                    authSession?.user?.lastName ?: ""
                )
            }

            var profileError by remember {
                mutableStateOf<String?>(null)
            }

            val authProvider = remember {
                Auth0AuthProvider(context = this)
            }

            val welcomeViewModel: WelcomeViewModel by viewModels(
                factoryProducer = {
                    viewModelFactory {
                        initializer {
                            WelcomeViewModel(
                                authProvider = authProvider,
                                onAuthenticated = { session ->
                                    runOnUiThread {
                                        sessionStore.saveSession(session)
                                        authSession = session
                                        sessionViewModel.refresh()
                                    }
                                }
                            )
                        }
                    }
                }
            )
            val welcomeState by welcomeViewModel.uiState.collectAsState()

            val navController = rememberNavController()

            LaunchedEffect(Unit) {
                sessionViewModel.refresh()
            }

            val currentRoute = when {
                !sessionState.authenticated -> Route.Welcome.path
                !sessionState.profileCompleted -> Route.CompleteProfile.path
                else -> Route.Home.path
            }

            val startDestination = remember { currentRoute }

            LaunchedEffect(currentRoute) {
                if (navController.currentDestination?.route != currentRoute) {
                    navController.navigate(currentRoute) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }

            LoResuelvoNavHost(
                navController = navController,
                startDestination = startDestination,
                welcome = {
                    WelcomeScreen(
                        errorMessage = welcomeState.error,
                        onRegisterClick = welcomeViewModel::signup
                    )
                },
                completeProfile = {
                    val activeSession = authSession
                    if (activeSession != null) {
                        CompleteProfileScreen(
                            firstName = firstName,
                            lastName = lastName,
                            errorMessage = profileError,

                            onFirstNameChange = {
                                firstName = it
                                profileError = null
                            },

                            onLastNameChange = {
                                lastName = it
                                profileError = null
                            },

                            onContinueClick = {

                                completeProfile(
                                    authSession = activeSession,
                                    firstName = firstName,
                                    lastName = lastName,
                                    sessionStore = sessionStore,

                                    onValidationError = {
                                        profileError = it
                                    },

                                    onProfileCompleted = {
                                        profileError = null
                                        authSession = it
                                        sessionViewModel.refresh()
                                    }
                                )
                            }
                        )
                    }
                },
                home = {
                    authSession?.let { activeSession ->
                        HomeScreen(
                            authSession = activeSession,
                            onLogoutClick = {

                                sessionStore.clearSession()

                                profileError = null
                                authSession = null
                                sessionViewModel.refresh()
                            }
                        )
                    }
                }
            )
        }
    }

    private fun completeProfile(
        authSession: AuthSession,
        firstName: String,
        lastName: String,
        sessionStore: SharedPreferencesAuthSessionStore,
        onValidationError: (String) -> Unit,
        onProfileCompleted: (AuthSession) -> Unit
    ) {

        when {

            firstName.isBlank() -> {
                onValidationError("El nombre es obligatorio")
                return
            }

            lastName.isBlank() -> {
                onValidationError("El apellido es obligatorio")
                return
            }
        }

        val updatedSession = authSession.copy(
            user = authSession.user.copy(
                firstName = firstName,
                lastName = lastName
            )
        )

        sessionStore.saveSession(updatedSession)

        onProfileCompleted(updatedSession)
    }
}