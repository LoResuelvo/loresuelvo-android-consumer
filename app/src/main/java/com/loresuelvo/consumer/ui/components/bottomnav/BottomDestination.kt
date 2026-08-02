package com.loresuelvo.consumer.ui.components.bottomnav

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.ui.graphics.vector.ImageVector
import com.loresuelvo.consumer.R

/**
 * Sealed hierarchy that models the three primary bottom-navigation
 * tabs of the app. Each destination owns:
 *  - the [route] the bar navigates to (a plain `String` so the
 *    bar stays decoupled from the `Route` sealed class in
 *    `ui.navigation`),
 *  - the Material icon the bar renders inside the `NavigationBarItem`,
 *  - the localised label the bar shows under the icon.
 *
 * The composable (`LoResuelvoBottomBar`) iterates over [all] and
 * renders one `NavigationBarItem` per entry; adding a fourth tab
 * later (Profile, Activity, Notifications, …) is a single
 * `data object` + a line in [all]. No composable changes required.
 *
 * The selection state is driven by the current `NavBackStackEntry`
 * route at the host — the bar holds no flags of its own (the
 * "Instagram-style" UX brief: visibility is derived from the
 * current route, never from a manual boolean).
 */
sealed class BottomDestination(
    val route: String,
    val icon: ImageVector,
    @StringRes val labelRes: Int,
) {

    data object Mensajes : BottomDestination(
        route = "messages",
        icon = Icons.Outlined.Message,
        labelRes = R.string.bottom_nav_mensajes,
    )

    data object Inicio : BottomDestination(
        route = "home",
        icon = Icons.Outlined.Home,
        labelRes = R.string.bottom_nav_inicio,
    )

    data object AsistenteIa : BottomDestination(
        route = "assistant",
        icon = Icons.Outlined.SmartToy,
        labelRes = R.string.bottom_nav_asistente,
    )

    companion object {
        /**
         * Single source of truth for the bottom-bar order. The
         * `NavigationBar` renders the items in the order declared
         * here, so re-ordering the tabs is a one-line edit.
         */
        val all: List<BottomDestination> = listOf(Mensajes, Inicio, AsistenteIa)

        /**
         * The bar is visible on any route that maps to a
         * `BottomDestination.route` (Home, the messages list, the
         * AI assistant landing). Detail / auth routes are excluded
         * by this membership test — no per-screen `if` required.
         */
        fun shouldShow(currentRoute: String?): Boolean =
            currentRoute != null && all.any { it.route == currentRoute }
    }
}
