package com.loresuelvo.consumer.ui.screens.home.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * "Solicitudes en curso" section. Renders the first
 * [ActiveRequest] of [requests] if any, or the empty state
 * otherwise. Stateless; the active-requests list is the
 * HomeViewModel's responsibility once `/requests` exists.
 */
@Composable
fun ActiveRequestsSection(
    requests: List<ActiveRequest>,
    modifier: Modifier = Modifier,
) {
    if (requests.isEmpty()) {
        ActiveRequestsEmpty(modifier = modifier)
    } else {
        ActiveRequestCard(request = requests.first())
    }
}
