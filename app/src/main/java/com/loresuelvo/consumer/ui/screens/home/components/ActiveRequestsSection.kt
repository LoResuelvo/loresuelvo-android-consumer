package com.loresuelvo.consumer.ui.screens.home.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * "Solicitudes en curso" section. Renders the first
 * [ActiveRequestMock] of [requests] if any, or the empty state
 * otherwise. Stateless; the active-requests list is the
 * HomeViewModel's responsibility (TODO: replace the hard-coded
 * mock in HomeScreen with a real flow when the backend exposes
 * `/requests`).
 */
@Composable
fun ActiveRequestsSection(
    requests: List<ActiveRequestMock>,
    modifier: Modifier = Modifier,
) {
    if (requests.isEmpty()) {
        ActiveRequestsEmpty(modifier = modifier)
    } else {
        ActiveRequestCard(request = requests.first())
    }
}
