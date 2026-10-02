package com.loresuelvo.consumer.ui.screens.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.loresuelvo.consumer.ui.screens.proposals.ProposalDetailUiState

@Composable
fun HomeScreen(
    state: HomeUiState,
    displayName: String?,
    onCategoryClick: (Int, String) -> Unit,
    onSeeAllCategoriesClick: () -> Unit,
    onSeeAllMisServiciosClick: () -> Unit = {},
    onSeeAllTurnosClick: () -> Unit = {},
    onTurnoCardClick: (String) -> Unit = {},
    onProposalClicked: (String) -> Unit = {},
    onNotificationsClick: () -> Unit,
    onAiSendClick: () -> Unit,
    onRetryClick: () -> Unit,
    onLogoutClick: () -> Unit,
    detailState: ProposalDetailUiState = ProposalDetailUiState.Loading,
    onDetailRetry: () -> Unit = {},
    onViewConversation: (String) -> Unit = {},
    onPayNow: (String) -> Unit = {},
    onDetailDismiss: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    HomeScreen(
        state = state,
        config = HomeScreenConfig(
            displayName = displayName,
            detailState = detailState,
        ),
        actions = HomeScreenActions(
            categories = HomeScreenActions.Categories(
                onCategoryClick = onCategoryClick,
                onSeeAll = onSeeAllCategoriesClick,
                onRetry = onRetryClick,
            ),
            turnos = HomeScreenActions.Turnos(
                onSeeAll = onSeeAllTurnosClick,
                onCardClick = onTurnoCardClick,
            ),
            proposals = HomeScreenActions.Proposals(
                onSeeAll = onSeeAllMisServiciosClick,
                onSelected = onProposalClicked,
                detail = HomeScreenActions.Proposals.Detail(
                    onRetry = onDetailRetry,
                    onViewConversation = onViewConversation,
                    onPayNow = onPayNow,
                    onDismiss = onDetailDismiss,
                ),
            ),
            diagnostics = HomeScreenActions.Diagnostics(onSend = onAiSendClick),
            account = HomeScreenActions.Account(onLogout = onLogoutClick),
            navigation = HomeScreenActions.Navigation(onNotifications = onNotificationsClick),
        ),
        modifier = modifier,
    )
}
