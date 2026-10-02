package com.loresuelvo.consumer.ui.screens.misservicios

import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus

/** User interactions exposed by the service-proposals screen. */
data class MisServiciosScreenActions(
    val filters: Filters = Filters(),
    val proposals: Proposals = Proposals(),
    val detail: Detail = Detail(),
) {
    data class Filters(
        val onSelected: (ServiceProposalStatus?) -> Unit = {},
    )

    data class Proposals(
        val onRetry: () -> Unit = {},
        val onSelected: (String) -> Unit = {},
    )

    data class Detail(
        val onRetry: () -> Unit = {},
        val onViewConversation: (String) -> Unit = {},
        val onPayNow: (String) -> Unit = {},
        val onDismiss: () -> Unit = {},
    )
}
