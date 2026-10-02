package com.loresuelvo.consumer.ui.screens.home

import com.loresuelvo.consumer.ui.screens.proposals.ProposalDetailUiState

/** Non-rendering configuration required by the home screen. */
data class HomeScreenConfig(
    val displayName: String? = null,
    val detailState: ProposalDetailUiState = ProposalDetailUiState.Loading,
)

/** User interactions grouped by the home screen sections. */
data class HomeScreenActions(
    val categories: Categories = Categories(),
    val turnos: Turnos = Turnos(),
    val proposals: Proposals = Proposals(),
    val diagnostics: Diagnostics = Diagnostics(),
    val account: Account = Account(),
    val navigation: Navigation = Navigation(),
) {
    data class Categories(
        val onCategoryClick: (Int, String) -> Unit = { _, _ -> },
        val onSeeAll: () -> Unit = {},
        val onRetry: () -> Unit = {},
    )

    data class Turnos(
        val onSeeAll: () -> Unit = {},
        val onCardClick: (String) -> Unit = {},
    )

    data class Proposals(
        val onSeeAll: () -> Unit = {},
        val onSelected: (String) -> Unit = {},
        val detail: Detail = Detail(),
    ) {
        data class Detail(
            val onRetry: () -> Unit = {},
            val onViewConversation: (String) -> Unit = {},
            val onPayNow: (String) -> Unit = {},
            val onDismiss: () -> Unit = {},
        )
    }

    data class Diagnostics(
        val onSend: () -> Unit = {},
    )

    data class Account(
        val onLogout: () -> Unit = {},
    )

    data class Navigation(
        val onNotifications: () -> Unit = {},
    )
}
