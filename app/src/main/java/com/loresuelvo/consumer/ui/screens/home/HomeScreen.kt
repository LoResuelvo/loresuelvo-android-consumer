package com.loresuelvo.consumer.ui.screens.home

import androidx.compose.foundation.background
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.loresuelvo.consumer.ui.screens.home.components.AiSearchBar
import com.loresuelvo.consumer.ui.screens.home.components.CategoryGrid
import com.loresuelvo.consumer.ui.screens.home.components.EducationalEmptyCard
import com.loresuelvo.consumer.ui.screens.home.components.HomeHeader
import com.loresuelvo.consumer.ui.screens.home.components.SectionTitle
import com.loresuelvo.consumer.ui.components.proposalcard.ProposalCard
import com.loresuelvo.consumer.ui.theme.LoresuelvoTheme

/**
 * Home screen for the authenticated consumer. The layout follows
 * the "value-first" principle: greeting + AI-driven entry point on
 * top, the category grid (the primary conversion action) in the
 * middle, and below-fold secondary information (Mis Servicios,
 * Mis Turnos, Diagnósticos recientes).
 *
 * Mis Turnos body: the dashboard only renders the shared
 * empty-state for now. The preview row of `Turno` cards lands
 * with scenario 02-VT once the `GET /work-orders` endpoint is
 * surfaced via [HomeViewModel].
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
    onCategoryClick: (categoryId: Int, categoryName: String) -> Unit,
    onSeeAllCategoriesClick: () -> Unit,
    onSeeAllMisServiciosClick: () -> Unit = {},
    onSeeAllTurnosClick: () -> Unit = {},
    onTurnoCardClick: (turnoId: String) -> Unit = {},
    onProposalClicked: (proposalId: String) -> Unit = {},
    onNotificationsClick: () -> Unit,
    onAiSendClick: () -> Unit,
    onRetryClick: () -> Unit,
    onLogoutClick: () -> Unit,
    // US-54 bug fix: the "Ver Solicitud" CTA on the home row
    // must open the same modal bottom sheet the MisServicios
    // list does. The defaults keep existing previews compiling;
    // the host (`HomeRoute`) wires every callback below.
    detailState: ProposalDetailUiState = ProposalDetailUiState.Loading,
    onDetailRetry: () -> Unit = {},
    onViewConversation: (conversationId: String) -> Unit = {},
    onPayNow: (proposalId: String) -> Unit = {},
    onDetailDismiss: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

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

        // US-27 follow-up: "Pagos pendientes" section surfaces the
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
                        onTurnoCardClick = onTurnoCardClick,
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
            onLinkClick = onSeeAllCategoriesClick,
        )

        CategorySection(
            state = state.categories,
            onCategoryClick = onCategoryClick,
            onRetryClick = onRetryClick,
        )

        // visualize-turns.feature scenario 01-VT: dedicated entry
        // into the "Mis Turnos" surface. The SectionTitle's "Ver
        // todas" link routes to the full list screen
        // (`Route.Turnos`); the body below it shows the closest-
        // to-now `MAX_TURNOS_ON_HOME` scheduled appointments as
        // [TurnoCard]s when the round trip succeeded (scenarios
        // 02-VT / 03-VT distinguish non-empty vs empty). When the
        // list is empty / loading / errored the shared
        // [EducationalEmptyCard] keeps the section visually
        // consistent so the user never sees a blank box.
        SectionTitle(
            text = stringResource(R.string.home_section_mis_turnos),
            link = stringResource(R.string.home_section_mis_turnos_link),
            linkTestTag = HOME_TURNOS_LINK_TAG,
            onLinkClick = onSeeAllTurnosClick,
        )
        when (val t = state.turnos) {
            is TurnosState.Ready -> {
                if (t.items.isEmpty()) {
                    EducationalEmptyCard(
                        title = stringResource(R.string.home_turnos_empty_title),
                        body = stringResource(R.string.home_turnos_empty_body),
                        ctaText = stringResource(R.string.home_turnos_empty_cta),
                        onCtaClick = onSeeAllCategoriesClick,
                    )
                } else {
                    TurnosRow(
                        turnos = t.items,
                        onTurnoCardClick = onTurnoCardClick,
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
                onCtaClick = onSeeAllCategoriesClick,
            )
        }

        // US-54 scenario 03-VSP: dedicated entry into the "Mis
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
            onLinkClick = onSeeAllMisServiciosClick,
        )
        MisServiciosRow(
            pending = state.pendingServiceProposals,
            upcoming = state.upcomingServiceProposals,
            onCtaClick = onSeeAllCategoriesClick,
            onProposalClicked = onProposalClicked,
        )

        Text(
            text = stringResource(R.string.home_section_diagnoses),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
        )
        EducationalEmptyCard(
            title = stringResource(R.string.home_diagnoses_empty_title),
            body = stringResource(R.string.home_diagnoses_empty_body),
            ctaText = stringResource(R.string.home_diagnoses_empty_cta),
            onCtaClick = onAiSendClick,
        )

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

    // US-54 bug fix: the proposal-detail bottom sheet is rendered
    // alongside the scrollable column. The sheet becomes visible
    // the moment the detail VM leaves `Loading` — see
    // [DetailSheet] for the visibility gate.
    ProposalDetailBottomSheet(
        detailState = detailState,
        onRetry = onDetailRetry,
        onViewConversation = onViewConversation,
        onPayNow = onPayNow,
        onDismiss = onDetailDismiss,
    )
}

/**
 * Modal bottom sheet that surfaces a single proposal's detail on
 * the Home dashboard. Mirrors the matching helper in
 * `MisServiciosScreen.DetailSheet` so the "Ver Solicitud" CTA on
 * the Home row opens the same component the MisServicios list
 * does (US-54 bug fix: the two surfaces used to render different
 * detail routes).
 *
 * Visibility is gated on `Ready` or `Error`. Before this fix the
 * gate was `!isLoading`, which let a post-dismiss `Error(404)`
 * (the result of the previous `load("")` reset hack) re-open the
 * sheet behind the consumer's back. Driving the host to
 * [com.loresuelvo.consumer.ui.screens.proposals.ProposalDetailViewModel.reset]
 * on dismiss keeps the state at
 * [com.loresuelvo.consumer.ui.screens.proposals.ProposalDetailUiState.Idle],
 * which the gate treats as "stay hidden".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProposalDetailBottomSheet(
    detailState: ProposalDetailUiState,
    onRetry: () -> Unit,
    onViewConversation: (conversationId: String) -> Unit,
    onPayNow: (proposalId: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(detailState) {
        visible = detailState is ProposalDetailUiState.Ready ||
            detailState is ProposalDetailUiState.Error
    }
    if (!visible) return
    ModalBottomSheet(
        onDismissRequest = {
            visible = false
            onDismiss()
        },
        sheetState = sheetState,
    ) {
        ProposalDetailScreen(
            state = detailState,
            onRetry = onRetry,
            onViewConversation = onViewConversation,
            onPayNow = onPayNow,
            onDismiss = {
                visible = false
                onDismiss()
            },
        )
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
                awaitingPaymentTurnos = TurnosState.Ready(emptyList()),
                turnos = TurnosState.Ready(emptyList()),
            ),
            displayName = "Matias",
            onCategoryClick = { _, _ -> },
            onSeeAllCategoriesClick = {},
            onNotificationsClick = {},
            onAiSendClick = {},
            onRetryClick = {},
            onLogoutClick = {},
            onPayNow = {},
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
 * Horizontal row of `ProposalCard`s under the Home "Mis Servicios"
 * section (US-54 scenario 03-VSP with proposals present). The
 * row concatenates the pending proposals (which need consumer
 * action) and the upcoming proposals (work scheduled) into one
 * preview; the "Ver todas" link on the section header remains
 * the path to the full MisServicios list.
 *
 * When both sub-states are `Ready(emptyList())` we render the
 * shared [EducationalEmptyCard] so the dashboard never leaves the
 * user wondering "what is this empty box?". The empty-state is
 * hidden during Loading and Error so a transient empty list
 * (still loading, or just failed) doesn't paint an empty card
 * over a soon-to-be-populated section.
 *
 * US-54 bug fix: replaced the previous `LazyRow` with a `Row` +
 * `horizontalScroll`. Mixing `LazyRow` inside the screen-level
 * `Column.verticalScroll` crashed the layout pass with an
 * "infinity maximum height constraints" warning and silently
 * dropped clicks on the cards — tapping "Ver Solicitud" did
 * nothing until the navigation graph was rebuilt. `Row` with
 * `horizontalScroll` is the canonical Compose pattern for a
 * small set of horizontally-laid-out cards (3–5 entries) and
 * also keeps the click pipeline responsive.
 */
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

const val HOME_MIS_SERVICIOS_EMPTY_CARD_TAG: String = "home-mis-servicios-empty-card"
const val HOME_TURNOS_LINK_TAG: String = "home-turnos-link"
