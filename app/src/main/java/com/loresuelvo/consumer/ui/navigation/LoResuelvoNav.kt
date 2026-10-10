package com.loresuelvo.consumer.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.flow.firstOrNull
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.loresuelvo.consumer.ui.components.bottomnav.BottomDestination
import com.loresuelvo.consumer.ui.components.bottomnav.LoResuelvoBottomBar
import com.loresuelvo.consumer.ui.professional.ProfessionalsViewModel
import com.loresuelvo.consumer.ui.screens.categories.CategoriesScreen
import com.loresuelvo.consumer.ui.screens.categories.CategoriesScreenActions
import com.loresuelvo.consumer.ui.screens.categories.CategoriesViewModel
import com.loresuelvo.consumer.ui.screens.home.HomeScreen
import com.loresuelvo.consumer.ui.screens.home.HomeScreenActions
import com.loresuelvo.consumer.ui.screens.home.HomeScreenConfig
import com.loresuelvo.consumer.ui.screens.home.HomeViewModel
import com.loresuelvo.consumer.ui.screens.chat.ChatRoute
import com.loresuelvo.consumer.ui.screens.misservicios.MisServiciosScreen
import com.loresuelvo.consumer.ui.screens.misservicios.MisServiciosViewModel
import com.loresuelvo.consumer.ui.screens.auth.SessionRestorationScreen
import com.loresuelvo.consumer.ui.session.SessionError
import com.loresuelvo.consumer.ui.session.SessionViewModel
import com.loresuelvo.consumer.ui.payment.PaymentResultRoute
import com.loresuelvo.consumer.ui.payment.ServiceAgreementRoute
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.loresuelvo.consumer.ui.notifications.ConversationNotificationVisibility
import android.util.Log
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

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
    val currentNavRoute = backStackEntry?.destination?.route

    val context = LocalContext.current
    val navigationIntents: NavigationIntentViewModel = hiltViewModel()

    /*
     * External payment App Links are handled here instead of using
     * NavController.handleDeepLink(). Android has already delivered
     * the Intent to MainActivity, so we only translate the external
     * URL into our internal PaymentResult route.
     */
    val sessionViewModel: SessionViewModel = hiltViewModel()
    LaunchedEffect(Unit) {
        navigationIntents.events.collect { intent ->
            if (navigationIntents.isMessageNotification(intent) ||
                navigationIntents.isServiceNotification(intent)
            ) {
                val restored = sessionViewModel.uiState.firstOrNull { !it.loading } ?: return@collect
                if (!restored.authenticated || restored.error != null) return@collect
                navController.currentBackStackEntryFlow.firstOrNull() ?: return@collect
                val route = navigationIntents.routeFor(intent) ?: return@collect
                navController.navigateFromNotification(route)
                return@collect
            }
            val uri = intent.data ?: return@collect

            val paymentResultPath = paymentResultPathFor(uri)
            if (paymentResultPath == null) {
                Log.d(
                    "APP_LINK",
                    "Ignoring unsupported deep link: $uri",
                )
                return@collect
            }

            navController.currentBackStackEntryFlow.firstOrNull()
                ?: return@collect

            navController.navigate(
                paymentResultPath,
            ) {
                launchSingleTop = true
            }

            Log.d(
                "APP_LINK",
                "Navigated to payment result: $paymentResultPath",
            )
        }
    }

    /*
     * Session routing.
     *
     * This router only manages top-level session destinations.
     * It must never replace deeper routes such as PaymentResult,
     * ServiceAgreement, Chat, Professionals, or WorkOrderDetail.
     */
    val sessionState by sessionViewModel.uiState.collectAsStateWithLifecycle()

    if (sessionState.loading || sessionState.error == SessionError.Restoration) {
        SessionRestorationScreen(
            loading = sessionState.loading,
            onRetryClick = sessionViewModel::retryRestoration,
        )
        return
    }

    ConversationNotificationVisibility(
        navigationIntents.conversationVisibility,
        currentNavRoute,
        backStackEntry?.arguments?.getString("conversationId"),
        sessionState.authenticated,
    )

    val sessionRoute = SessionRouteMapper.routeFor(sessionState)

    val lastAppliedSessionRoute = remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(sessionRoute) {
        val sessionRoutes = setOf(
            Route.Welcome.path,
            Route.CompleteProfile.path,
            Route.Home.path,
        )

        val graphReady =
            navController.currentBackStackEntryFlow.firstOrNull() != null

        if (!graphReady) {
            return@LaunchedEffect
        }

        val currentRoute = navController.currentDestination?.route

        if (currentRoute !in sessionRoutes) {
            return@LaunchedEffect
        }

        if (lastAppliedSessionRoute.value == sessionRoute) {
            return@LaunchedEffect
        }

        if (currentRoute != sessionRoute) {
            navController.navigate(sessionRoute) {
                popUpTo(navController.graph.id) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        }

        lastAppliedSessionRoute.value = sessionRoute
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets.navigationBars,
            bottomBar = {},
            containerColor = Color.Transparent,
        ) { padding ->
            LoResuelvoNavHost(
                navController = navController,
                startDestination = sessionRoute,
                contentPadding = padding,
                content = ConsumerNavContent(
                    session = SessionNavContent(
                        welcome = { WelcomeRoute() },
                        completeProfile = { CompleteProfileRoute(navController) },
                    ),
                    account = AccountNavContent(
                        myProfile = { ConsumerProfileRoute() },
                    ),
                    discovery = DiscoveryNavContent(
                        home = { HomeRoute(navController) },
                        categories = { CategoriesRoute(navController) },
                        professionals = { categoryId, categoryName ->
                            ProfessionalsRoute(
                                navController = navController,
                                categoryId = categoryId,
                                categoryName = categoryName,
                            )
                        },
                        providerProfile = { providerId ->
                            ProviderProfileRoute(
                                navController = navController,
                                providerId = providerId,
                            )
                        },
                    ),
                    chat = ChatNavContent(
                        chat = { conversationId ->
                            ChatRoute(
                                navController = navController,
                                conversationId = conversationId,
                            )
                        },
                        conversation = { conversationId ->
                            ConversationRoute(
                                navController = navController,
                                conversationId = conversationId,
                            )
                        },
                        messages = { MessagesRoute(navController) },
                        assistant = { AssistantRoute(navController) },
                    ),
                    work = WorkNavContent(
                        misServicios = { proposalId ->
                            MisServiciosRoute(navController, proposalId)
                        },
                        turnos = { TurnosRoute(navController) },
                        workOrderDetail = { workOrderId, provider ->
                            WorkOrderDetailRoute(
                                navController = navController,
                                workOrderId = workOrderId,
                                provider = provider,
                            )
                        },
                    ),
                    payment = PaymentNavContent(
                        serviceAgreement = { nav ->
                            ServiceAgreementRoute.bind(
                                navController = nav,
                                serviceAgreementViewModel = hiltViewModel(),
                                onReturnHome = {
                                    navController.popBackStack(
                                        Route.Home.path,
                                        inclusive = false,
                                    )
                                },
                            )
                        },
                        paymentResult = { nav, entry ->
                            PaymentResultRoute.bind(
                                navController = nav,
                                backStackEntry = entry,
                                onReturnHome = {
                                    navController.popBackStack(
                                        Route.Home.path,
                                        inclusive = false,
                                    )
                                },
                            )
                        },
                    ),
                ),
            )
        }

        if (BottomDestination.shouldShow(currentNavRoute)) {
            LoResuelvoBottomBar(
                currentRoute = currentNavRoute,
                onNavigate = { destination ->
                    navController.navigate(destination.route) {
                        popUpTo(
                            navController.graph.findStartDestination().id,
                        ) {
                            saveState = true
                        }

                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
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
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val contactState by contactViewModel.uiState.collectAsStateWithLifecycle()

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

    val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        contactViewModel.onAttachImageFromUri(uri)
    }

    com.loresuelvo.consumer.ui.screens.professional.ProfessionalsScreen(
        state = state,
        contactFormState = contactState,
        actions = com.loresuelvo.consumer.ui.screens.professional.ProfessionalsScreenActions(
            onRetry = { viewModel.loadProviders(categoryId, categoryName) },
            onContact = contactViewModel::onOpenContact,
            onViewProfile = { provider ->
                navController.navigate(Route.ProviderProfile.buildPath(provider.id))
            },
            contact = com.loresuelvo.consumer.ui.screens.professional.ProfessionalsScreenActions.ContactFormActions(
                onTitleChange = contactViewModel::onTitleChange,
                onDescriptionChange = contactViewModel::onDescriptionChange,

                onAttachImages = {
                    galleryLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly,
                        ),
                    )
                },
                onRemoveImage = contactViewModel::onRemoveImage,
                onSubmit = contactViewModel::onSubmit,
                onCancel = contactViewModel::onCancel,
            ),
        ),
    )
}

/**
 * Home screen — entry point of the authenticated consumer. Reads the
 * navigation session (via `SessionViewModel`) and delegates it to the
 * new `HomeScreen` as a plain `displayName`. Category clicks navigate
 * to the [Route.Professionals] route and wires the home actions.
 */
@Composable
private fun HomeRoute(
    navController: androidx.navigation.NavHostController,
) {
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val homeViewModel: HomeViewModel = hiltViewModel()

    val detailViewModel: com.loresuelvo.consumer.ui.screens.proposals.ProposalDetailViewModel =
        hiltViewModel()
    val context = LocalContext.current
    val sessionState by sessionViewModel.uiState.collectAsStateWithLifecycle()
    val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val detailState by detailViewModel.uiState.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                homeViewModel.refresh()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val findTurnoById: (String) -> com.loresuelvo.consumer.domain.turno.Turno? = { id ->
        val fromUpcoming = (homeState.turnos as? com.loresuelvo.consumer.ui.screens.home.TurnosState.Ready)?.items
        val fromAwaiting = (homeState.awaitingPaymentTurnos as? com.loresuelvo.consumer.ui.screens.home.TurnosState.Ready)?.items
        (fromUpcoming.orEmpty() + fromAwaiting.orEmpty()).firstOrNull { it.id == id }
    }

    LaunchedEffect(detailViewModel) {
        detailViewModel.checkoutUrl.collect { url ->
            CustomTabsIntent.Builder()
                .build()
                .launchUrl(context, Uri.parse(url))
        }
    }

    HomeScreen(
        state = homeState,
        config = HomeScreenConfig(
            displayName = sessionState.session?.user?.firstName,
            detailState = detailState,
        ),
        actions = HomeScreenActions(
            categories = HomeScreenActions.Categories(
                onCategoryClick = { categoryId, categoryName ->
                    navController.navigate(
                        Route.Professionals.buildPath(categoryId, categoryName),
                    )
                },
                onSeeAll = { navController.navigate(Route.Categories.path) },
                onRetry = { homeViewModel.loadCategories() },
            ),
            turnos = HomeScreenActions.Turnos(
                onSeeAll = { navController.navigate(Route.Turnos.path) },
                onCardClick = { turnoId ->
                    val turno = findTurnoById(turnoId)
                    val provider = turno?.counterpart?.let { counterpart ->
                        com.loresuelvo.consumer.domain.workorder.WorkOrderDetailCounterpart(
                            id = counterpart.id,
                            name = counterpart.name,
                            surname = counterpart.surname,
                            categoryName = counterpart.categoryName,
                            profilePhotoUrl = counterpart.profilePhotoUrl,
                        )
                    }
                    navController.navigate(Route.WorkOrderDetail.buildPath(turnoId, provider))
                },
            ),
            proposals = HomeScreenActions.Proposals(
                onSeeAll = { navController.navigate(Route.MisServicios.path) },
                onSelected = detailViewModel::load,
                detail = HomeScreenActions.Proposals.Detail(
                    onRetry = {
                        val cachedId = (detailState as? com.loresuelvo.consumer.ui.screens.proposals.ProposalDetailUiState.Ready)
                            ?.proposal?.id.orEmpty()
                        detailViewModel.load(cachedId)
                    },
                    onViewConversation = { conversationId ->
                        if (conversationId.isNotBlank()) {
                            navController.navigate(Route.Conversation.buildPath(conversationId))
                        }
                    },
                    onPayNow = detailViewModel::payNow,
                    onDismiss = detailViewModel::reset,
                ),
            ),
            diagnostics = HomeScreenActions.Diagnostics(
                onSend = { navController.navigate(Route.Chat.buildPath()) },
                onSeeAll = { navController.navigate(Route.Assistant.path) },
                onConversationClick = { conversationId ->
                    navController.navigate(Route.Chat.buildPath(conversationId))
                },
                onRetry = homeViewModel::loadRecentAiConversations,
            ),
            account = HomeScreenActions.Account(
                onLogout = { sessionViewModel.signOut(context) },
            ),
        ),
    )
}

/**
 * Dedicated screen that lists every service category available
 * on the platform. Resolves the [CategoriesViewModel] through
 * Hilt and forwards the UDF state to the screen. Tapping a tile
 * reuses the existing [Route.Professionals] navigation.
 */
@Composable
private fun CategoriesRoute(
    navController: androidx.navigation.NavHostController,
) {
    val viewModel: CategoriesViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    CategoriesScreen(
        state = state,
        actions = CategoriesScreenActions(
            onCategoryClick = { categoryId, categoryName ->
                navController.navigate(
                    Route.Professionals.buildPath(categoryId, categoryName),
                )
            },
            onSearchQueryChange = viewModel::onSearchQueryChange,
            onBackClick = { navController.popBackStack() },
            onRetryClick = { viewModel.loadCategories() },
        ),
    )
}

@Composable
private fun MisServiciosRoute(
    navController: androidx.navigation.NavHostController,
    proposalId: String?,
) {
    val viewModel: MisServiciosViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val detailViewModel: com.loresuelvo.consumer.ui.screens.proposals.ProposalDetailViewModel = hiltViewModel()
    val detailState by detailViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(proposalId) {
        if (!proposalId.isNullOrBlank()) {
            detailViewModel.load(proposalId)
        }
    }

    LaunchedEffect(detailViewModel) {
        detailViewModel.checkoutUrl.collect { url ->
            CustomTabsIntent.Builder()
                .build()
                .launchUrl(context, Uri.parse(url))
        }
    }

    MisServiciosScreen(
        state = state,
        detailState = detailState,
        actions = com.loresuelvo.consumer.ui.screens.misservicios.MisServiciosScreenActions(
            filters = com.loresuelvo.consumer.ui.screens.misservicios.MisServiciosScreenActions.Filters(
                onSelected = viewModel::onFilterSelected,
            ),
            proposals = com.loresuelvo.consumer.ui.screens.misservicios.MisServiciosScreenActions.Proposals(
                onRetry = viewModel::load,
                onSelected = detailViewModel::load,
            ),
            detail = com.loresuelvo.consumer.ui.screens.misservicios.MisServiciosScreenActions.Detail(
                onRetry = {
                    val cachedId = (detailState as? com.loresuelvo.consumer.ui.screens.proposals.ProposalDetailUiState.Ready)
                        ?.proposal?.id.orEmpty()
                    detailViewModel.load(cachedId)
                },
                onViewConversation = { conversationId ->
                    if (conversationId.isNotBlank()) {
                        navController.navigate(
                            Route.Conversation.buildPath(conversationId),
                        )
                    }
                },
                onPayNow = detailViewModel::payNow,
                onDismiss = detailViewModel::reset,
            ),
        ),
    )
}
