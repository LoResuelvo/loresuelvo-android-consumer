package com.loresuelvo.consumer.ui.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.loresuelvo.consumer.BuildConfig
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
    val context = LocalContext.current
    val authorizationClient = remember(context) { Identity.getAuthorizationClient(context) }
    val authorizationRequest = remember {
        BuildConfig.GOOGLE_CALENDAR_SERVER_CLIENT_ID
            .takeIf(String::isNotBlank)
            ?.let { serverClientId -> googleCalendarAuthorizationRequest(serverClientId) }
    }
    val googleAuthorizationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val authorizationResult = result.data?.let {
            runCatching { authorizationClient.getAuthorizationResultFromIntent(it) }.getOrNull()
        }
        val serverAuthCode = authorizationResult?.serverAuthCode
        if (!serverAuthCode.isNullOrBlank()) {
            viewModel.connectCalendar(serverAuthCode)
        } else {
            viewModel.onCalendarAuthorizationCancelled()
        }
    }

    ConsumerProfileScreen(
        state = state,
        onRetryClick = viewModel::load,
        onCalendarConnectClick = {
            authorizationRequest?.let { request ->
                authorizationClient.authorize(request)
                    .addOnSuccessListener { authorizationResult ->
                        if (authorizationResult.hasResolution()) {
                            authorizationResult.pendingIntent?.let { pendingIntent ->
                                googleAuthorizationLauncher.launch(
                                    IntentSenderRequest.Builder(pendingIntent.intentSender).build(),
                                )
                            } ?: viewModel.onCalendarAuthorizationCancelled()
                        } else {
                            authorizationResult.serverAuthCode
                                ?.takeIf(String::isNotBlank)
                                ?.let(viewModel::connectCalendar)
                                ?: viewModel.onCalendarAuthorizationCancelled()
                        }
                    }
                    .addOnFailureListener { viewModel.onCalendarAuthorizationCancelled() }
            } ?: viewModel.onCalendarAuthorizationUnavailable()
        },
    )
}

private const val GOOGLE_CALENDAR_EVENTS_SCOPE =
    "https://www.googleapis.com/auth/calendar.events"

private fun googleCalendarAuthorizationRequest(
    serverClientId: String,
): AuthorizationRequest = AuthorizationRequest.builder()
    .setRequestedScopes(listOf(Scope(GOOGLE_CALENDAR_EVENTS_SCOPE)))
    .requestOfflineAccess(serverClientId)
    .build()

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
