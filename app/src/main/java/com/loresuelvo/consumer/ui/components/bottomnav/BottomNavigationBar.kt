package com.loresuelvo.consumer.ui.components.bottomnav

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/**
 * Instagram-style bottom navigation bar for the three primary
 * destinations in [BottomDestination]. The composable is
 * **stateless** — the host (`LoResuelvoNav`) owns the current
 * route, the navigation controller, and the [Scaffold] slot.
 *
 * Visual brief (matches the US-18 UX notes):
 *  - Material 3 `NavigationBar` (no FAB, no elevated top edge).
 *  - Surface container colour, 2 dp tonal elevation — the bar
 *    looks like a quiet surface, not a chip.
 *  - Same icon for selected / unselected; the M3 default
 *    "active indicator" pill handles the selection visual.
 *  - [NavigationBarDefaults.windowInsets] is used by default, so
 *    the system gesture bar is honoured on Edge-to-Edge layouts
 *    (the activity enables `setDecorFitsSystemWindows(false)`).
 *
 * Visibility is delegated to [BottomDestination.shouldShow]; the
 * bar returns early when the current route isn't a primary
 * destination (Welcome, Complete Profile, Chat, Conversation, …).
 */
@Composable
fun LoResuelvoBottomBar(
    currentRoute: String?,
    onNavigate: (BottomDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!BottomDestination.shouldShow(currentRoute)) return

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = modifier,
    ) {
        BottomDestination.all.forEach { destination ->
            val isSelected = currentRoute == destination.route
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(destination) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = stringResource(destination.labelRes),
                    )
                },
                label = { Text(stringResource(destination.labelRes)) },
            )
        }
    }
}
