package com.loresuelvo.consumer.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.ui.auth.CompleteProfileEvent
import com.loresuelvo.consumer.ui.auth.CompleteProfileViewModel
import com.loresuelvo.consumer.ui.auth.WelcomeViewModel
import com.loresuelvo.consumer.ui.screens.auth.CompleteProfileScreen
import com.loresuelvo.consumer.ui.screens.auth.WelcomeScreen
import com.loresuelvo.consumer.ui.screens.home.HomeScreen
import com.loresuelvo.consumer.ui.session.SessionViewModel

/**
 * Composition root for the app. Hosts the navigation graph, the
 * smart-router logic (which screen is the start destination, based
 * on the session), and the per-route ViewModel wiring.
 *
 * `MainActivity` is a thin shell that calls
 * `setContent { LoResuelvoNav() }`. All `LaunchedEffect`,
 * `popUpTo(graph.id) { inclusive = true }` and `navController.navigate`
 * calls live here.
 *
 * Smart-route logic reuses [SessionViewModel] instead of subscribing
 * to `AuthSessionStore` directly — the navigation graph stays a pure
 * consumer of the UDF state that the rest of the UI uses.
 */
@Composable
fun LoResuelvoNav() {
    val navController = androidx.navigation.compose.rememberNavController()

    val sessionViewModel: SessionViewModel = hiltViewModel()
    val sessionState by sessionViewModel.uiState.collectAsState()

    val currentRoute = when {
        !sessionState.authenticated -> Route.Welcome.path
        !sessionState.profileCompleted -> Route.CompleteProfile.path
        else -> Route.Home.path
    }

    androidx.compose.runtime.LaunchedEffect(currentRoute) {
        // Re-navigate whenever the derived start destination changes.
        // `popUpTo(graph.id)` clears the back stack so the user can't
        // press Back and return to the previous auth state.
        if (navController.currentDestination?.route != currentRoute) {
            navController.navigate(currentRoute) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    LoResuelvoNavHost(
        navController = navController,
        startDestination = Route.Welcome.path,
        welcome = { WelcomeRoute() },
        completeProfile = { CompleteProfileRoute(navController = navController) },
        home = { HomeRoute() },
    )
}

/**
 * Welcome screen with its Hilt-provided ViewModel. The Composable
 * bridge passes the activity `Context` (`LocalContext.current`) to
 * the selected ViewModel action: Auth0 requires an Activity-bound
 * context to start its browser flow.
 */
@Composable
private fun WelcomeRoute() {
    val viewModel: WelcomeViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    WelcomeScreen(
        error = state.error,
        categories = state.categories,
        onRegisterClick = { viewModel.signup(context) },
        onLoginClick = { viewModel.login(context) },
        onGoogleClick = { viewModel.loginWithGoogle(context) },
    )
}

/**
 * Complete-profile screen + the `NavigateToHome` event listener
 * that owns the success-side navigation. Kept inside `LoResuelvoNav`
 * so the navigation graph is the only place that calls
 * `navController.navigate(Route.Home.path)`.
 */
@Composable
private fun CompleteProfileRoute(
    navController: androidx.navigation.NavHostController,
) {
    val viewModel: CompleteProfileViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsState()
    androidx.compose.runtime.LaunchedEffect(viewModel) {
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
}

/**
 * Home screen — phase 9 of the master plan will replace this with a
 * real screen. For now we read the current [SessionUiState]
 * (provided by the same `SessionViewModel` the navigation graph
 * uses) and performs the Auth0 + local logout sequence.
 */
@Composable
private fun HomeRoute() {
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val state by sessionViewModel.uiState.collectAsState()
    val context = LocalContext.current
    state.session?.let { session ->
        HomeScreen(
            authSession = session,
            signingOut = state.signingOut,
            logoutError = state.error,
            onLogoutClick = { sessionViewModel.signOut(context) },
        )
    }
}
