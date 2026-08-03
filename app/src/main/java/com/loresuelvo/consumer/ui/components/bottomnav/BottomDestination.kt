package com.loresuelvo.consumer.ui.components.bottomnav

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.ui.graphics.vector.ImageVector
import com.loresuelvo.consumer.R

/**
 * Single source of truth for the bottom-navigation tabs. Each
 * destination owns:
 *  - the [route] the bar navigates to (a plain `String` so the
 *    bar stays decoupled from the `Route` sealed class in
 *    `ui.navigation`),
 *  - the Material icon the bar renders inside the `NavigationBarItem`,
 *  - the localised label the bar shows under the icon.
 *
 * The composable (`LoResuelvoBottomBar`) iterates over [all] and
 * renders one `NavigationBarItem` per entry; adding a fourth tab
 * later (Profile, Activity, Notifications, …) is a single new
 * `val` in [Companion] + a line in [Companion.all]. No composable
 * changes required.
 *
 * The selection state is driven by the current `NavBackStackEntry`
 * route at the host — the bar holds no flags of its own (the
 * "Instagram-style" UX brief: visibility is derived from the
 * current route, never from a manual boolean).
 *
 * Implementation note: the three tabs are modelled as plain `val`s
 * in a companion object (not `data object`s) because KAPT's Kotlin
 * 1.9 fallback (the project uses KSP for most things, but KAPT for
 * Hilt) appears to leave the `INSTANCE` static field of `data
 * object`s as null at the time the `listOf(Mensajes, …)` is built
 * in Robolectric-driven tests. Plain companion `val`s initialise
 * in the companion's `<clinit>` and are guaranteed to be
 * non-null by the time the list is constructed. Production
 * runtime (no KAPT) is unaffected.
 */
data class BottomDestination(
    val route: String,
    val icon: ImageVector,
    @StringRes val labelRes: Int,
) {

    companion object {
        /** Tab order matches the brief: Mensajes (left) · Inicio (centre) · Asistente IA (right). */
        val Mensajes: BottomDestination =
            BottomDestination(
                route = "messages",
                icon = Icons.Outlined.Message,
                labelRes = R.string.bottom_nav_mensajes,
            )

        val Inicio: BottomDestination =
            BottomDestination(
                route = "home",
                icon = Icons.Outlined.Home,
                labelRes = R.string.bottom_nav_inicio,
            )

        val AsistenteIa: BottomDestination =
            BottomDestination(
                route = "assistant",
                icon = Icons.Outlined.SmartToy,
                labelRes = R.string.bottom_nav_asistente,
            )

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
         *
         * Implemented with a `when` rather than `all.any { … }` to
         * sidestep a class-loading order quirk in the Robolectric /
         * KAPT 1.9 fallback test runtime that left the captured
         * `it` as null in the bytecode. The behaviour is identical
         * to a membership test against `all.map { it.route }`.
         */
        fun shouldShow(currentRoute: String?): Boolean = when (currentRoute) {
            Mensajes.route, Inicio.route, AsistenteIa.route -> true
            else -> false
        }
    }
}
