package com.loresuelvo.consumer.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

/**
 * Pure graph layer. The host ([com.loresuelvo.consumer.ui.navigation.LoResuelvoNav])
 * owns the `Scaffold` slot (bottom navigation bar) and the
 * smart-router logic; this composable only declares the routes and
 * the screen-typed Composable for each one. The two layers are
 * split so the host can be unit-tested in isolation — the graph
 * is a pure consumer of the screen composables.
 *
 * `contentPadding` carries the Scaffold insets (top status bar +
 * bottom nav bar when visible). The graph wraps the [NavHost] in
 * a [Box] with that padding so the bottom bar never overlaps the
 * scrollable content of any screen.
 */
@Composable
fun LoResuelvoNavHost(
    navController: NavHostController,
    startDestination: String,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    welcome: @Composable () -> Unit,
    completeProfile: @Composable () -> Unit,
    home: @Composable () -> Unit,
    professionals: @Composable (categoryId: Int, categoryName: String) -> Unit,
    chat: @Composable () -> Unit,
    conversation: @Composable (conversationId: String) -> Unit,
    messages: @Composable () -> Unit,
    assistant: @Composable () -> Unit,
) {
    Box(modifier = Modifier.padding(contentPadding)) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
        ) {
            composable(Route.Welcome.path) { welcome() }
            composable(Route.CompleteProfile.path) { completeProfile() }
            composable(Route.Home.path) { home() }
            composable(
                route = Route.Professionals(
                    categoryId = -1,
                    categoryName = "_ignored_",
                ).path,
                arguments = listOf(
                    navArgument("categoryId") { type = NavType.IntType },
                    navArgument("categoryName") { type = NavType.StringType },
                ),
            ) { entry ->
                val categoryId = entry.arguments?.getInt("categoryId") ?: -1
                val categoryName = entry.arguments?.getString("categoryName").orEmpty()
                professionals(categoryId, categoryName)
            }
            composable(Route.Chat.path) { chat() }
            composable(
                route = Route.Conversation(conversationId = "_ignored_").path,
                arguments = listOf(
                    navArgument("conversationId") { type = NavType.StringType },
                ),
            ) { entry ->
                val conversationId = entry.arguments?.getString("conversationId").orEmpty()
                conversation(conversationId)
            }
            // Bottom-bar destinations (US-18).
            composable(Route.Messages.path) { messages() }
            composable(Route.Assistant.path) { assistant() }
        }
    }
}
