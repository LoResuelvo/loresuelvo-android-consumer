package com.loresuelvo.consumer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.rememberNavController
import com.loresuelvo.consumer.data.auth.Auth0AuthProvider
import com.loresuelvo.consumer.data.auth.EncryptedAuthSessionStore
import com.loresuelvo.consumer.data.auth.createEncryptedSessionPrefs
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.ui.auth.CompleteProfileViewModel
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
            val encryptedPrefs = remember {
                createEncryptedSessionPrefs(this)
            }

            val sessionStore: AuthSessionStore = remember {
                EncryptedAuthSessionStore(encryptedPrefs)
            }

            val sessionViewModel: SessionViewModel by viewModels(
                factoryProducer = {
                    viewModelFactory {
                        initializer {
                            SessionViewModel()
                        }
                    }
                }
            )
            val sessionState by sessionViewModel.uiState.collectAsState()

            val authProvider = remember {
                Auth0AuthProvider(context = this)
            }

            val welcomeViewModel: WelcomeViewModel by viewModels(
                factoryProducer = {
                    viewModelFactory {
                        initializer {
                            WelcomeViewModel(
                                authProvider = authProvider,
                                sessionStore = sessionStore,
                            )
                        }
                    }
                }
            )
            val welcomeState by welcomeViewModel.uiState.collectAsState()

            val navController = rememberNavController()

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
                    val activeSession by sessionStore.sessionFlow.collectAsState()
                    if (activeSession != null) {
                        val completeProfileViewModel: CompleteProfileViewModel by viewModels(
                            factoryProducer = {
                                viewModelFactory {
                                    initializer {
                                        CompleteProfileViewModel(
                                            sessionStore = sessionStore,
                                        )
                                    }
                                }
                            }
                        )
                        val completeProfileState by completeProfileViewModel.uiState.collectAsState()

                        CompleteProfileScreen(
                            firstName = completeProfileState.firstName,
                            lastName = completeProfileState.lastName,
                            errorMessage = completeProfileState.error,

                            onFirstNameChange = completeProfileViewModel::onFirstNameChange,
                            onLastNameChange = completeProfileViewModel::onLastNameChange,
                            onContinueClick = completeProfileViewModel::onContinueClick,
                        )
                    }
                },
                home = {
                    val activeSession by sessionStore.sessionFlow.collectAsState()
                    activeSession?.let { session ->
                        HomeScreen(
                            authSession = session,
                            onLogoutClick = {
                                sessionStore.clearSession()
                            }
                        )
                    }
                }
            )
        }
    }
}