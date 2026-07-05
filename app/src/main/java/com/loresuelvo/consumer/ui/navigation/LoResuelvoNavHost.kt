package com.loresuelvo.consumer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun LoResuelvoNavHost(
    navController: NavHostController,
    startDestination: String,
    welcome: @Composable () -> Unit,
    completeProfile: @Composable () -> Unit,
    home: @Composable () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(Route.Welcome.path) { welcome() }
        composable(Route.CompleteProfile.path) { completeProfile() }
        composable(Route.Home.path) { home() }
    }
}