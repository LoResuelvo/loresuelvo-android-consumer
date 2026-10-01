package com.loresuelvo.consumer.ui.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import com.loresuelvo.consumer.ui.screens.profile.CompleteProfileAction
import com.loresuelvo.consumer.ui.screens.profile.CompleteProfileScreen
import com.loresuelvo.consumer.ui.screens.profile.CompleteProfileViewModel
import com.loresuelvo.consumer.ui.screens.profile.ConsumerProfileScreen
import com.loresuelvo.consumer.ui.screens.profile.ConsumerProfileViewModel

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
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(viewModel::onProfilePhotoSelected) }

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
        onAction = { action ->
            if (action == CompleteProfileAction.PickPhotoClicked) {
                photoPicker.launch(
                    PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly,
                    ),
                )
            } else {
                viewModel.onAction(action)
            }
        },
    )
}

@Composable
internal fun ConsumerProfileRoute() {
    val viewModel: ConsumerProfileViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsState()

    ConsumerProfileScreen(
        state = state,
        onRetryClick = viewModel::load,
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
