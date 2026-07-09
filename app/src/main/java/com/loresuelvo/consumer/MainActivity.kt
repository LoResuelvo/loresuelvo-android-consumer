package com.loresuelvo.consumer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.rememberNavController
import com.loresuelvo.consumer.data.auth.Auth0AuthProvider
import com.loresuelvo.consumer.data.auth.EncryptedAuthSessionStore
import com.loresuelvo.consumer.data.auth.createEncryptedSessionPrefs
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.ui.auth.CompleteProfileEvent
import com.loresuelvo.consumer.ui.auth.CompleteProfileViewModel
import com.loresuelvo.consumer.ui.auth.WelcomeViewModel
import com.loresuelvo.consumer.ui.navigation.LoResuelvoNavHost
import com.loresuelvo.consumer.ui.navigation.Route
import com.loresuelvo.consumer.ui.screens.auth.CompleteProfileScreen
import com.loresuelvo.consumer.ui.screens.auth.WelcomeScreen
import com.loresuelvo.consumer.ui.screens.home.HomeScreen
import com.loresuelvo.consumer.ui.session.SessionViewModel

/**
 * Single Activity. Holds the composition root and the top-level
 * navigation graph. Stays thin (≤ 80 lines) — the rest of the
 * navigation and business logic lives in the [LoResuelvoNavHost],
 * the ViewModels, and the use cases.
 *
 * NOTE: Fase 8 of the master plan will collapse this into
 * `setContent { LoResuelvoNav() }` once the smart-navigation logic
 * (currentRoute from SessionViewModel) moves into LoResuelvoNav.
 * For now, the activity keeps the navigation glue so the
 * CompleteProfile success event has somewhere to land.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // Hilt provides AuthSessionStore from
            // data/auth/SessionStoreModule.kt.
            val sessionStore: AuthSessionStore = remember { createSessionStore() }
            val activeSession by sessionStore.sessionFlow.collectAsState()

            val authProvider = remember {
                Auth0AuthProvider(context = this)
            }

            // SessionViewModel has no @Inject dependencies today; the
            // factory passes the SessionStateHolder-backed sessionStore
            // so the VM can keep the same StateFlow contract.
            val sessionViewModel: SessionViewModel by viewModels(
                factoryProducer = {
                    viewModelFactory {
                        initializer { SessionViewModel() }
                    }
                }
            )

            val navController = rememberNavController()

            val currentRoute = when {
                activeSession == null -> Route.Welcome.path
                activeSession?.user?.isProfileComplete() == true -> Route.Home.path
                else -> Route.CompleteProfile.path
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
                    val viewModel: WelcomeViewModel = hiltViewModel()
                    val state by viewModel.uiState.collectAsState()
                    WelcomeScreen(
                        errorMessage = state.error,
                        onRegisterClick = viewModel::signup,
                    )
                },
                completeProfile = {
                    val viewModel: CompleteProfileViewModel = hiltViewModel()
                    val state by viewModel.uiState.collectAsState()
                    LaunchedEffect(viewModel) {
                        viewModel.events.collect { event ->
                            when (event) {
                                CompleteProfileEvent.NavigateToHome ->
                                    navController.navigate(Route.Home.path) {
                                        popUpTo(navController.graph.id) { inclusive = true }
                                        launchSingleTop = true
                                    }
                            }
                        }
                    }
                    CompleteProfileScreen(
                        firstName = state.firstName,
                        lastName = state.lastName,
                        loading = state.loading,
                        error = state.error,
                        onFirstNameChange = viewModel::onFirstNameChange,
                        onLastNameChange = viewModel::onLastNameChange,
                        onContinueClick = viewModel::onContinueClick,
                        onEvent = { /* navigation handled above */ },
                    )
                },
                home = {
                    activeSession?.let { session ->
                        HomeScreen(
                            authSession = session,
                            onLogoutClick = { sessionStore.clearSession() },
                        )
                    }
                }
            )
        }
    }

    /**
     * Single place that knows how to build the production
     * [AuthSessionStore]. Kept in the activity (not the di/ module)
     * so the dependency on `createEncryptedSessionPrefs` (which
     * needs a [Context]) doesn't need to be exposed at module
     * boundary. The Hilt-provided [AuthSessionStore] from
     * [com.loresuelvo.consumer.data.auth.SessionStoreModule] is the
     * preferred path; this helper is the fallback for the welcome /
     * complete-profile / home composables that need to collect
     * `sessionFlow` directly from the activity scope.
     */
    private fun createSessionStore(): AuthSessionStore =
        EncryptedAuthSessionStore(createEncryptedSessionPrefs(this))
}
