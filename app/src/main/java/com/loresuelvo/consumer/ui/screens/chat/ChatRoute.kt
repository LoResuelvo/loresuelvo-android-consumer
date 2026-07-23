package com.loresuelvo.consumer.ui.screens.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController

/**
 * Compose bridge for the AI diagnostic chat screen. Resolves the
 * [ChatViewModel] through Hilt and wires the navigation callbacks.
 *
 * Today this route only handles the back press (the smart router is
 * responsible for re-deriving the start destination based on the
 * session, so plain `popBackStack` is enough to return to Home).
 */
@Composable
fun ChatRoute(
    navController: NavHostController,
) {
    val viewModel: ChatViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsState()
    ChatScreen(
        onBackClick = { navController.popBackStack() },
    )
    // `state` is consumed here so subsequent commits can layer
    // additional Composables that observe it without touching
    // [ChatRoute] again.
    @Suppress("UNUSED_EXPRESSION") state
}
