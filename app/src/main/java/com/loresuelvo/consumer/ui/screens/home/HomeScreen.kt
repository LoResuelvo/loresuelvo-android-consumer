package com.loresuelvo.consumer.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.loresuelvo.consumer.ui.screens.proposals.ProposalDetailScreen
import com.loresuelvo.consumer.ui.screens.proposals.ProposalDetailUiState
import com.loresuelvo.consumer.ui.screens.proposals.ProposalDetailViewModel
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.assistant.AiConversationSummary
import com.loresuelvo.consumer.domain.assistant.AiConversationListOutcome
import com.loresuelvo.consumer.ui.screens.chat.AssistantAvatar
import com.loresuelvo.consumer.ui.screens.home.components.AiSearchBar
import com.loresuelvo.consumer.ui.screens.home.components.CategoryGrid
import com.loresuelvo.consumer.ui.screens.home.components.EducationalEmptyCard
import com.loresuelvo.consumer.ui.screens.home.components.HomeHeader
import com.loresuelvo.consumer.ui.screens.home.components.SectionTitle
import com.loresuelvo.consumer.ui.components.proposalcard.ProposalCard
import com.loresuelvo.consumer.ui.theme.LoresuelvoTheme

@Composable
fun HomeScreen(
    state: HomeUiState,
    config: HomeScreenConfig = HomeScreenConfig(),
    actions: HomeScreenActions = HomeScreenActions(),
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag(HOME_SCREEN_TAG)
            .background(MaterialTheme.colorScheme.background)
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
            displayName = config.displayName,
            onNotificationsClick = actions.navigation.onNotifications,
        )

        AiSearchBar(onSendClick = actions.diagnostics.onSend)

        // consumer's [TurnoStatus.AwaitingPayment] turnos directly
        // above the category grid so the balance-clearance CTA is
        // the first actionable thing the user sees after the AI
        // search. The section is hidden entirely when the list is
        // empty (no placeholder / "nothing pending" copy) — the
        // "you have something to pay" signal should be loud, not
        // diluted with a permanent empty state.
        when (val ap = state.awaitingPaymentTurnos) {
            is TurnosState.Ready -> {
                if (ap.items.isNotEmpty()) {
                    SectionTitle(
                        text = stringResource(R.string.home_section_pending_payments),
                    )
                    AwaitingPaymentRow(
                        turnos = ap.items,
                        onTurnoCardClick = actions.turnos.onCardClick,
                    )
                }
                // Loading / Error / empty: no fallback block — the
                // section is simply absent. The dedicated Mis
                // Turnos screen renders the typed retry / empty
                // copy for the full list.
            }
            TurnosState.Loading,
            TurnosState.Error,
            -> Unit
        }

        SectionTitle(
            text = stringResource(R.string.home_section_categories),
            link = stringResource(R.string.home_section_categories_link),
            onLinkClick = actions.categories.onSeeAll,
        )

        CategorySection(
            state = state.categories,
            onCategoryClick = actions.categories.onCategoryClick,
            onRetryClick = actions.categories.onRetry,
        )

        // into the "Mis Turnos" surface. The SectionTitle's "Ver
        // todas" link routes to the full list screen
        // (`Route.Turnos`); the body below it shows the closest-
        // to-now `MAX_TURNOS_ON_HOME` scheduled appointments as
        // [TurnoCard]s when the round trip succeeded. If the
        // list is empty, loading, or errored, the shared
        // [EducationalEmptyCard] keeps the section visually
        // consistent so the user never sees a blank box.
        SectionTitle(
            text = stringResource(R.string.home_section_mis_turnos),
            link = stringResource(R.string.home_section_mis_turnos_link),
            linkTestTag = HOME_TURNOS_LINK_TAG,
            onLinkClick = actions.turnos.onSeeAll,
        )
        when (val t = state.turnos) {
            is TurnosState.Ready -> {
                if (t.items.isEmpty()) {
                    EducationalEmptyCard(
                        title = stringResource(R.string.home_turnos_empty_title),
                        body = stringResource(R.string.home_turnos_empty_body),
                        ctaText = stringResource(R.string.home_turnos_empty_cta),
                        onCtaClick = actions.categories.onSeeAll,
                    )
                } else {
                    TurnosRow(
                        turnos = t.items,
                        onTurnoCardClick = actions.turnos.onCardClick,
                    )
                }
            }
            // Loading and Error silently fall back to the empty
            // card so the dashboard doesn't break on a failed
            // `GET /work-orders`; the dedicated Mis Turnos
            // screen renders the typed retry branch instead.
            TurnosState.Loading,
            TurnosState.Error,
            -> EducationalEmptyCard(
                title = stringResource(R.string.home_turnos_empty_title),
                body = stringResource(R.string.home_turnos_empty_body),
                ctaText = stringResource(R.string.home_turnos_empty_cta),
                onCtaClick = actions.categories.onSeeAll,
            )
        }

        // Servicios" surface. The "Ver todas" link lands on a
        // full list of every service proposal regardless of
        // status. When the dashboard has proposals (Pending or
        // Accepted) we render a horizontally-scrollable row of
        // compact cards using the shared `ProposalCard` component;
        // when neither sub-state has items we fall back to the
        // shared `EducationalEmptyCard` so the dashboard never
        // leaves the user wondering "what is this empty box?".
        SectionTitle(
            text = stringResource(R.string.home_section_mis_servicios),
            link = stringResource(R.string.home_section_mis_servicios_link),
            linkTestTag = com.loresuelvo.consumer.ui.screens.home.components.HOME_MIS_SERVICIOS_LINK_TAG,
            onLinkClick = actions.proposals.onSeeAll,
        )
        MisServiciosRow(
            pending = state.pendingServiceProposals,
            upcoming = state.upcomingServiceProposals,
            onCtaClick = actions.categories.onSeeAll,
            onProposalClicked = actions.proposals.onSelected,
        )

        SectionTitle(
            text = stringResource(R.string.home_section_diagnoses),
            link = stringResource(R.string.home_section_diagnoses_link),
            linkTestTag = HOME_DIAGNOSTICS_LINK_TAG,
            onLinkClick = actions.diagnostics.onSeeAll,
        )
        RecentDiagnosticsSection(
            state = state.recentAiConversations,
            onConversationClick = actions.diagnostics.onConversationClick,
            onRetry = actions.diagnostics.onRetry,
            onStartDiagnosis = actions.diagnostics.onSend,
        )

        Spacer(Modifier.height(8.dp))

        TextButton(
            onClick = actions.account.onLogout,
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

    // alongside the scrollable column. The sheet becomes visible
    // the moment the detail VM leaves `Loading` — see
    // [DetailSheet] for the visibility gate.
    ProposalDetailBottomSheet(
        detailState = config.detailState,
        actions = actions.proposals.detail,
    )
}

@Composable
private fun ProposalDetailBottomSheet(
    detailState: ProposalDetailUiState,
    actions: HomeScreenActions.Proposals.Detail,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(detailState) {
        visible = detailState is ProposalDetailUiState.Ready ||
            detailState is ProposalDetailUiState.Error
    }
    if (!visible) return
    ProposalDetailScreen(
        state = detailState,
        onRetry = actions.onRetry,
        onViewConversation = actions.onViewConversation,
        onPayNow = actions.onPayNow,
        onDismiss = {
            visible = false
            actions.onDismiss()
        },
    )
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

@Composable
private fun RecentDiagnosticsSection(
    state: AiConversationsState,
    onConversationClick: (String) -> Unit,
    onRetry: () -> Unit,
    onStartDiagnosis: () -> Unit,
) {
    when (state) {
        AiConversationsState.Loading -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.testTag(HOME_DIAGNOSTICS_LOADING_TAG),
                color = MaterialTheme.colorScheme.secondary,
            )
        }

        is AiConversationsState.Ready -> {
            if (state.items.isEmpty()) {
                EducationalEmptyCard(
                    title = stringResource(R.string.home_diagnoses_empty_title),
                    body = stringResource(R.string.home_diagnoses_empty_body),
                    ctaText = stringResource(R.string.home_diagnoses_empty_cta),
                    onCtaClick = onStartDiagnosis,
                    modifier = Modifier.testTag(HOME_DIAGNOSTICS_EMPTY_TAG),
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(HOME_DIAGNOSTICS_LIST_TAG),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.items.forEach { conversation ->
                        RecentDiagnosisRow(
                            conversation = conversation,
                            onClick = { onConversationClick(conversation.id) },
                        )
                    }
                }
            }
        }

        is AiConversationsState.Error -> {
            val message = when (state.failure) {
                is AiConversationListOutcome.Failure.Network ->
                    R.string.assistant_screen_error_network
                is AiConversationListOutcome.Failure.Server ->
                    R.string.assistant_screen_error_server
                is AiConversationListOutcome.Failure.Unauthorized ->
                    R.string.assistant_screen_error_unauthorized
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(HOME_DIAGNOSTICS_ERROR_TAG),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(message),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Button(onClick = onRetry) {
                    Text(text = stringResource(R.string.assistant_screen_error_retry))
                }
            }
        }
    }
}

@Composable
private fun RecentDiagnosisRow(
    conversation: AiConversationSummary,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("$HOME_DIAGNOSTICS_ROW_TAG-${conversation.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AssistantAvatar(size = 44.dp)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = conversation.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                conversation.lastMessagePreview
                    ?.takeIf { it.isNotBlank() }
                    ?.let { preview ->
                        Text(
                            text = preview,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
            }
            if (conversation.lastMessageAtEpochMillis > 0L) {
                Text(
                    text = formatDiagnosisDate(conversation.lastMessageAtEpochMillis),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

private fun formatDiagnosisDate(epochMillis: Long): String =
    android.text.format.DateUtils.getRelativeTimeSpanString(
        epochMillis,
        System.currentTimeMillis(),
        android.text.format.DateUtils.MINUTE_IN_MILLIS,
    ).toString()

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
                awaitingPaymentTurnos = TurnosState.Ready(emptyList()),
                turnos = TurnosState.Ready(emptyList()),
            ),
            config = HomeScreenConfig(displayName = "Matias"),
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun HomeScreenLoadingPreview() {
    LoresuelvoTheme {
        HomeScreen(
            state = HomeUiState.Loading(),
            config = HomeScreenConfig(displayName = "Matias"),
        )
    }
}

@Composable
private fun MisServiciosRow(
    pending: ServiceProposalsState,
    upcoming: ServiceProposalsState,
    onCtaClick: () -> Unit,
    onProposalClicked: (proposalId: String) -> Unit,
) {
    val pendingItems = (pending as? ServiceProposalsState.Ready)?.items.orEmpty()
    val upcomingItems = (upcoming as? ServiceProposalsState.Ready)?.items.orEmpty()
    val items = pendingItems + upcomingItems

    val bothReady =
        pending is ServiceProposalsState.Ready &&
        upcoming is ServiceProposalsState.Ready

    if (items.isEmpty() && bothReady) {
        EducationalEmptyCard(
            title = stringResource(R.string.mis_servicios_empty_title),
            body = stringResource(R.string.mis_servicios_empty_body),
            ctaText = stringResource(R.string.mis_servicios_empty_cta),
            onCtaClick = onCtaClick,
            modifier = Modifier.testTag(HOME_MIS_SERVICIOS_EMPTY_CARD_TAG),
        )
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .testTag(HOME_MIS_SERVICIOS_ROW_TAG),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items.forEach { proposal ->
            ProposalCard(
                proposal = proposal,
                onViewClicked = { onProposalClicked(proposal.id) },
                modifier = Modifier.width(370.dp),
            )
        }
    }
}

/**
 * Compose testTags for the MisServicios block on Home.
 */
const val HOME_MIS_SERVICIOS_ROW_TAG: String = "home-mis-servicios-row"
/**
 * Horizontal row of up to [HomeViewModel.MAX_TURNOS_ON_HOME]
 * scheduled-appointment [TurnoCard]s for the Home "Mis Turnos"
 * section. The VM takes the closest-to-now N before this
 * composable runs, so the row always renders at most N items.
 *
 * `Row + horizontalScroll` mirrors [MisServiciosRow]'s pattern:
 * small N, scroll only if the user's locale makes the cards
 * wider than the viewport.
 */
@Composable
private fun TurnosRow(
    turnos: List<com.loresuelvo.consumer.domain.turno.Turno>,
    onTurnoCardClick: (turnoId: String) -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .testTag(HOME_TURNOS_ROW_TAG),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        turnos.forEach { turno ->
            com.loresuelvo.consumer.ui.components.turnocard.TurnoCard(
                turno = turno,
                onDetailsClick = { onTurnoCardClick(turno.id) },
                modifier = Modifier.width(370.dp),
            )
        }
    }
}

/**
 * Horizontal row of every [TurnoStatus.AwaitingPayment] [Turno]
 * for the Home "Pagos pendientes" section. The section itself
 * is hidden by the caller when the list is empty; this row
 * assumes at least one item is present.
 *
 * The row reuses [TurnoCard] (same data shape, same testTag
 * layout for the inner CTA). The width is the same as
 * [TurnosRow] so the visual density matches the "Mis Turnos"
 * preview below.
 */
@Composable
private fun AwaitingPaymentRow(
    turnos: List<com.loresuelvo.consumer.domain.turno.Turno>,
    onTurnoCardClick: (turnoId: String) -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .testTag(HOME_PENDING_PAYMENTS_ROW_TAG),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        turnos.forEach { turno ->
            com.loresuelvo.consumer.ui.components.turnocard.TurnoCard(
                turno = turno,
                onDetailsClick = { onTurnoCardClick(turno.id) },
                modifier = Modifier.width(370.dp),
            )
        }
    }
}

const val HOME_TURNOS_ROW_TAG: String = "home-turnos-row"
const val HOME_PENDING_PAYMENTS_ROW_TAG: String = "home-pending-payments-row"
const val HOME_SCREEN_TAG: String = "home-screen"
const val HOME_DIAGNOSTICS_LINK_TAG: String = "home-diagnostics-link"
const val HOME_DIAGNOSTICS_LOADING_TAG: String = "home-diagnostics-loading"
const val HOME_DIAGNOSTICS_EMPTY_TAG: String = "home-diagnostics-empty"
const val HOME_DIAGNOSTICS_LIST_TAG: String = "home-diagnostics-list"
const val HOME_DIAGNOSTICS_ERROR_TAG: String = "home-diagnostics-error"
const val HOME_DIAGNOSTICS_ROW_TAG: String = "home-diagnostics-row"

const val HOME_MIS_SERVICIOS_EMPTY_CARD_TAG: String = "home-mis-servicios-empty-card"
const val HOME_TURNOS_LINK_TAG: String = "home-turnos-link"
