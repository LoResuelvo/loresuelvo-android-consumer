package com.loresuelvo.consumer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

@Composable
fun LoResuelvoNavHost(
    navController: NavHostController,
    startDestination: String,
    welcome: @Composable () -> Unit,
    completeProfile: @Composable () -> Unit,
    home: @Composable () -> Unit,
    professionals: @Composable (categoryId: Int, categoryName: String) -> Unit,
    chat: @Composable () -> Unit,
) {
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
    }
}
