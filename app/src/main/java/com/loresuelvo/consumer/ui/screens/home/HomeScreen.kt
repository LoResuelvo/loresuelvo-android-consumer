package com.loresuelvo.consumer.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.ui.screens.home.components.ActiveRequest
import com.loresuelvo.consumer.ui.screens.home.components.ActiveRequestsSection
import com.loresuelvo.consumer.ui.screens.home.components.AiSearchBar
import com.loresuelvo.consumer.ui.screens.home.components.CategoryGrid
import com.loresuelvo.consumer.ui.screens.home.components.HomeHeader
import com.loresuelvo.consumer.ui.screens.home.components.RecentDiagnosesEmpty
import com.loresuelvo.consumer.ui.screens.home.components.SectionTitle
import com.loresuelvo.consumer.ui.theme.LoresuelvoTheme
import com.loresuelvo.consumer.ui.theme.SubtitleGray
import kotlinx.coroutines.launch

/**
 * Home screen for the authenticated consumer. The layout follows
 * the "value-first" principle: greeting + AI-driven entry point on
 * top, the category grid (the primary conversion action) in the
 * middle, and below-fold secondary information (active requests,
 * recent diagnoses).
 *
 * Stateless: every visible value is sourced from [HomeUiState] and
 * every user action is delegated to a callback. The hosting
 * `LoResuelvoNav.HomeRoute` wires navigation and the
 * `HomeViewModel`'s state.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    displayName: String?,
    activeRequests: List<ActiveRequest> = emptyList(),
    onCategoryClick: (categoryId: Int, categoryName: String) -> Unit,
    onSeeAllCategoriesClick: () -> Unit,
    onSeeAllMisServiciosClick: () -> Unit = {},
    onProposalClicked: (proposalId: String) -> Unit = {},
    onNotificationsClick: () -> Unit,
    onAiSendClick: () -> Unit,
    onRetryClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    // Active requests list comes from the caller (HomeViewModel once
    // `/requests` exists). Empty by default today, which surfaces the
    // empty-state copy "No tienes ninguna solicitud en curso."
    val scrollToCategories: () -> Unit = {
        scope.launch { scrollState.animateScrollTo(0) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // 08-UXUI: the outer `LoResuelvoNav.Scaffold` is
            // configured to consume only the nav bar inset so
            // screens with their own `topBar` (Chat,
            // Conversation) can take the status bar inset
            // themselves. Bottom-nav screens must apply
            // `statusBarsPadding()` explicitly to keep the
            // greeting below the status bar.
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HomeHeader(
            displayName = displayName,
            onNotificationsClick = onNotificationsClick,
        )

        AiSearchBar(onSendClick = onAiSendClick)

        SectionTitle(
            text = stringResource(R.string.home_section_categories),
            link = stringResource(R.string.home_section_categories_link),
            onLinkClick = onSeeAllCategoriesClick,
        )

        CategorySection(
            state = state.categories,
            onCategoryClick = onCategoryClick,
            onRetryClick = onRetryClick,
        )

        SectionTitle(
            text = stringResource(R.string.home_section_requests),
            link = stringResource(R.string.home_section_requests_link),
        )
        ActiveRequestsSection(
            requests = activeRequests,
            onEmptyCtaClick = scrollToCategories,
        )

        // US-54 scenario 03-VSP: dedicated entry into the "Mis
        // Servicios" surface. The "Ver todas" link lands on a
        // full list of every service proposal regardless of
        // status. When the dashboard has proposals (Pending or
        // Accepted) we render a horizontally-scrollable row of
        // compact cards using the shared `ProposalCard` component;
        // when neither sub-state has items we fall back to the
        // same empty-state copy the dedicated MisServicios screen
        // shows, so the dashboard never leaves the user wondering
        // "what is this empty box?".
        SectionTitle(
            text = stringResource(R.string.home_section_mis_servicios),
            link = stringResource(R.string.home_section_mis_servicios_link),
            linkTestTag = com.loresuelvo.consumer.ui.screens.home.components.HOME_MIS_SERVICIOS_LINK_TAG,
            onLinkClick = onSeeAllMisServiciosClick,
        )
        MisServiciosRow(
            pending = state.pendingServiceProposals,
            upcoming = state.upcomingServiceProposals,
            onProposalClicked = onProposalClicked,
        )

        Text(
            text = stringResource(R.string.home_section_diagnoses),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
        )
        RecentDiagnosesEmpty(onCtaClick = onAiSendClick)

        Spacer(Modifier.height(8.dp))

        TextButton(
            onClick = onLogoutClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.home_logout),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun CategorySection(
    state: CategoriesState,
    onCategoryClick: (categoryId: Int, categoryName: String) -> Unit,
    onRetryClick: () -> Unit,
) {
    when (state) {
        CategoriesState.Loading -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }

        is CategoriesState.Ready -> CategoryGrid(
            categories = state.items,
            onCategoryClick = onCategoryClick,
            modifier = Modifier
                .height(280.dp)
                .fillMaxWidth(),
        )

        CategoriesState.Error -> Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.welcome_categories_error),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            Button(onClick = onRetryClick) {
                Text(text = stringResource(R.string.professionals_retry))
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun HomeScreenReadyPreview() {
    LoresuelvoTheme {
        HomeScreen(
            state = HomeUiState.Ready(
                categories = CategoriesState.Ready(
                    listOf(
                        com.loresuelvo.consumer.domain.category.Category(1, "Plomería"),
                        com.loresuelvo.consumer.domain.category.Category(2, "Gasista"),
                        com.loresuelvo.consumer.domain.category.Category(3, "Electricista"),
                        com.loresuelvo.consumer.domain.category.Category(4, "Climatización"),
                        com.loresuelvo.consumer.domain.category.Category(5, "Pintura"),
                        com.loresuelvo.consumer.domain.category.Category(6, "Albañilería"),
                    ),
                ),
                pendingServiceProposals = ServiceProposalsState.Ready(emptyList()),
                upcomingServiceProposals = ServiceProposalsState.Ready(emptyList()),
            ),
            displayName = "Matias",
            activeRequests = listOf(
                ActiveRequest(
                    title = "Fuga en lavamanos",
                    time = "Hoy 14:30",
                    status = "En camino",
                    proName = "Carlos M.",
                    proInitial = "C",
                    rating = 4.9,
                    reviewCount = 120,
                ),
            ),
            onCategoryClick = { _, _ -> },
            onSeeAllCategoriesClick = {},
            onNotificationsClick = {},
            onAiSendClick = {},
            onRetryClick = {},
            onLogoutClick = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun HomeScreenLoadingPreview() {
    LoresuelvoTheme {
        HomeScreen(
            state = HomeUiState.Loading(),
            displayName = "Matias",
            onCategoryClick = { _, _ -> },
            onSeeAllCategoriesClick = {},
            onNotificationsClick = {},
            onAiSendClick = {},
            onRetryClick = {},
            onLogoutClick = {},
        )
    }
}

/**
 * Card shown beneath the "Mis Servicios" section on Home when the
 * dashboard has neither pending nor upcoming proposals to show.
 * Renders the same copy the dedicated MisServicios screen uses,
 * so the dashboard never leaves the user wondering "what is this
 * empty box?". Hidden during Loading and Error so a transient
 * empty list (still loading, or just failed) doesn't paint an
 * empty card over a soon-to-be-populated section.
 */
@Composable
private fun MisServiciosEmptyCard(
    pending: ServiceProposalsState,
    upcoming: ServiceProposalsState,
) {
    val hasPendingItems = pending is ServiceProposalsState.Ready && pending.items.isNotEmpty()
    val hasUpcomingItems = upcoming is ServiceProposalsState.Ready && upcoming.items.isNotEmpty()
    if (!hasPendingItems && !hasUpcomingItems) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag(HOME_MIS_SERVICIOS_EMPTY_CARD_TAG),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.mis_servicios_empty_title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.mis_servicios_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = SubtitleGray,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Horizontal row of `ProposalCard`s under the Home "Mis Servicios"
 * section (US-54 scenario 03-VSP with proposals present). The
 * row concatenates the pending proposals (which need consumer
 * action) and the upcoming proposals (work scheduled) into one
 * preview; the "Ver todas" link on the section header remains
 * the path to the full MisServicios list.
 *
 * Hidden entirely when both sub-states are non-Ready (Loading /
 * Error) so we don't paint cards against a soon-to-be-populated
 * section.
 */
@Composable
private fun MisServiciosRow(
    pending: ServiceProposalsState,
    upcoming: ServiceProposalsState,
    onProposalClicked: (proposalId: String) -> Unit,
) {
    val pendingItems = (pending as? ServiceProposalsState.Ready)?.items.orEmpty()
    val upcomingItems = (upcoming as? ServiceProposalsState.Ready)?.items.orEmpty()
    val items = pendingItems + upcomingItems
    if (items.isEmpty()) {
        MisServiciosEmptyCard(pending = pending, upcoming = upcoming)
        return
    }
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(HOME_MIS_SERVICIOS_ROW_TAG),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
    ) {
        items(
            items = items,
            key = { it.id },
        ) { proposal ->
            com.loresuelvo.consumer.ui.components.proposalcard.ProposalCard(
                proposal = proposal,
                onViewClicked = { onProposalClicked(proposal.id) },
                modifier = Modifier.width(280.dp),
            )
        }
    }
}

/**
 * Compose testTags for the MisServicios block on Home.
 */
const val HOME_MIS_SERVICIOS_ROW_TAG: String = "home-mis-servicios-row"
const val HOME_MIS_SERVICIOS_EMPTY_CARD_TAG: String = "home-mis-servicios-empty-card"
