package com.loresuelvo.consumer.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.loresuelvo.consumer.ui.auth.WelcomeViewModel
import com.loresuelvo.consumer.ui.components.bottomnav.BottomDestination
import com.loresuelvo.consumer.ui.components.bottomnav.LoResuelvoBottomBar
import com.loresuelvo.consumer.ui.professional.ProfessionalsViewModel
import com.loresuelvo.consumer.ui.screens.assistant.AssistantScreen
import com.loresuelvo.consumer.ui.screens.auth.WelcomeScreen
import com.loresuelvo.consumer.ui.screens.home.HomeScreen
import com.loresuelvo.consumer.ui.screens.home.HomeViewModel
import com.loresuelvo.consumer.ui.screens.chat.ChatRoute
import com.loresuelvo.consumer.ui.screens.messages.MessagesScreen
import com.loresuelvo.consumer.ui.screens.profile.CompleteProfileEvent
import com.loresuelvo.consumer.ui.screens.profile.CompleteProfileScreen
import com.loresuelvo.consumer.ui.screens.profile.CompleteProfileViewModel
import com.loresuelvo.consumer.ui.session.SessionViewModel

/**
 * Composition root for the app. Hosts the navigation graph, the
 * smart-router logic (which screen is the start destination, based
 * on the session), the bottom-nav [Scaffold] slot, and the
 * per-route ViewModel wiring.
 *
 * `MainActivity` is a thin shell that calls
 * `setContent { LoResuelvoNav() }`. All `LaunchedEffect`,
 * `popUpTo(graph.id) { inclusive = true }` and `navController.navigate`
 * calls live here.
 *
 * Smart-route logic reuses [SessionViewModel] instead of
 * subscribing to `AuthSessionStore` directly — the navigation graph
 * stays a pure consumer of the UDF state that the rest of the UI
 * uses.
 *
 * Bottom-bar visibility is derived from the current
 * `NavBackStackEntry` route via
 * [BottomDestination.shouldShow]; the bar is rendered in the
 * Scaffold's `bottomBar` slot and observes the [Route.Messages] /
 * [Route.Assistant] / [Route.Home] triad.
 */
@Composable
fun LoResuelvoNav() {
    val navController = androidx.navigation.compose.rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val navCurrentRoute = backStackEntry?.destination?.route

    val sessionViewModel: SessionViewModel = hiltViewModel()
    val sessionState by sessionViewModel.uiState.collectAsState()

    val currentRoute = when {
        !sessionState.authenticated -> Route.Welcome.path
        !sessionState.profileCompleted -> Route.CompleteProfile.path
        else -> Route.Home.path
    }

    val currentDestination = navController.currentDestination

    androidx.compose.runtime.LaunchedEffect(currentRoute, currentDestination) {
        // Re-navigate whenever the derived start destination changes.
        // Wait until the NavHost has attached its graph and exposed a
        // current destination before navigating, otherwise `navigate`
        // fails with "graph has not been set".
        if (currentDestination != null && currentDestination.route != currentRoute) {
            navController.navigate(currentRoute) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (BottomDestination.shouldShow(navCurrentRoute)) {
                LoResuelvoBottomBar(
                    currentRoute = navCurrentRoute,
                    onNavigate = { destination ->
                        // Bottom-nav navigation follows the Instagram
                        // pattern: popUpTo the start destination to keep
                        // the back stack flat, launchSingleTop to avoid
                        // duplicate instances of the same tab, and
                        // restoreState to remember scroll positions.
                        navController.navigate(destination.route) {
                            if (navController.currentDestination != null) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { padding ->
        LoResuelvoNavHost(
            navController = navController,
            startDestination = Route.Welcome.path,
            contentPadding = padding,
            welcome = { WelcomeRoute() },
            completeProfile = { CompleteProfileRoute(navController = navController) },
            home = { HomeRoute(navController = navController) },
            professionals = { categoryId, categoryName ->
                ProfessionalsRoute(navController, categoryId, categoryName)
            },
            chat = { ChatRoute(navController = navController) },
            conversation = { conversationId ->
                com.loresuelvo.consumer.ui.screens.chat.ConversationScreen(
                    conversationId = conversationId,
                )
            },
            messages = { MessagesScreen() },
            assistant = { AssistantScreen() },
        )
    }
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
                        if (navController.currentDestination != null) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
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
 * Provider list for a single category. Reads the `categoryId` and
 * `categoryName` from the back-stack entry and forwards them to the
 * [ProfessionalsViewModel] on first composition; subsequent
 * navigation to the same category reuses the same VM instance.
 *
 * Also hosts the [ContactProviderViewModel] that drives the
 * contact-form bottom sheet. The VM emits
 * [com.loresuelvo.consumer.ui.screens.professional.ContactProviderEvent.NavigateToConversation]
 * when `POST /job-requests` succeeds — this route forwards the
 * event to the [androidx.navigation.NavHostController].
 */
@Composable
private fun ProfessionalsRoute(
    navController: androidx.navigation.NavHostController,
    categoryId: Int,
    categoryName: String,
) {
    val viewModel: ProfessionalsViewModel = hiltViewModel()
    val contactViewModel: com.loresuelvo.consumer.ui.screens.professional.ContactProviderViewModel =
        hiltViewModel()
    androidx.compose.runtime.LaunchedEffect(categoryId, categoryName) {
        viewModel.loadProviders(categoryId, categoryName)
    }
    val state by viewModel.uiState.collectAsState()
    val contactState by contactViewModel.uiState.collectAsState()

    // Forward the navigation event emitted by the contact form
    // (Phase 5 / scenario 02-SRP). The VM closes the modal before
    // sending the event, so the user lands on the chat screen
    // directly without an intermediate "submitted" state.
    androidx.compose.runtime.LaunchedEffect(contactViewModel) {
        contactViewModel.events.collect { event ->
            when (event) {
                is com.loresuelvo.consumer.ui.screens.professional.ContactProviderEvent.NavigateToConversation ->
                    navController.navigate(
                        Route.Conversation.buildPath(event.conversationId),
                    )
            }
        }
    }

    com.loresuelvo.consumer.ui.screens.professional.ProfessionalsScreen(
        state = state,
        contactFormState = contactState,
        onRetryClick = { viewModel.loadProviders(categoryId, categoryName) },
        onContactarClick = contactViewModel::onOpenContact,
        onContactTitleChange = contactViewModel::onTitleChange,
        onContactDescriptionChange = contactViewModel::onDescriptionChange,
        onContactSubmit = contactViewModel::onSubmit,
        onContactCancel = contactViewModel::onCancel,
    )
}

/**
 * Home screen — entry point of the authenticated consumer. Reads the
 * navigation session (via `SessionViewModel`) and delegates it to the
 * new `HomeScreen` as a plain `displayName`. Category clicks navigate
 * to the [Route.Professionals] route; the rest of the actions are
 * placeholders for upcoming features (AI search, notifications,
 * logout).
 */
@Composable
private fun HomeRoute(
    navController: androidx.navigation.NavHostController,
) {
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val homeViewModel: HomeViewModel = hiltViewModel()
    val context = LocalContext.current
    val sessionState by sessionViewModel.uiState.collectAsState()
    // HomeViewModel kicks off `loadCategories()` in its `init { }`
    // block, so we don't repeat it via LaunchedEffect here.
    val homeState by homeViewModel.uiState.collectAsState()

    HomeScreen(
        state = homeState,
        displayName = sessionState.session?.user?.firstName,
        onCategoryClick = { categoryId, categoryName ->
            navController.navigate(
                Route.Professionals.buildPath(categoryId, categoryName),
            )
        },
        onNotificationsClick = { /* TODO */ },
        onAiSendClick = { navController.navigate(Route.Chat.path) },
        onRetryClick = { homeViewModel.loadCategories() },
        onLogoutClick = { sessionViewModel.signOut(context) },
    )
}
