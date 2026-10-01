package com.loresuelvo.consumer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.loresuelvo.consumer.ui.auth.WelcomeViewModel
import com.loresuelvo.consumer.ui.screens.auth.WelcomeScreen
import com.loresuelvo.consumer.ui.screens.profile.CompleteProfileEvent
import com.loresuelvo.consumer.ui.screens.profile.CompleteProfileScreen
import com.loresuelvo.consumer.ui.screens.profile.CompleteProfileViewModel

/** Route hosts for authentication and consumer/provider profiles. */

@Composable
internal fun WelcomeRoute() {
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

/** Owns profile completion effects while the graph owns navigation. */
@Composable
internal fun CompleteProfileRoute(
    navController: NavHostController,
) {
    val viewModel: CompleteProfileViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                CompleteProfileEvent.NavigateToHome ->
                    navController.navigate(Route.Home.path) {
                        if (navController.currentDestination != null) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                        launchSingleTop = true
                    }
            }
        }
    }

    CompleteProfileScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}

@Composable
internal fun ProviderProfileRoute(
    navController: NavHostController,
    providerId: Int,
) {
    val viewModel: com.loresuelvo.consumer.ui.screens.providerprofile.ProviderProfileViewModel =
        hiltViewModel()
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(providerId) {
        viewModel.load(providerId)
    }

    com.loresuelvo.consumer.ui.screens.providerprofile.ProviderProfileScreen(
        state = state,
        onRetryClick = { viewModel.load(providerId) },
        onBackClick = { navController.popBackStack() },
    )
}
