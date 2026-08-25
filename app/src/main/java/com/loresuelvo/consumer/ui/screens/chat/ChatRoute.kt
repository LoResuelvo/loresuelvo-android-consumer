package com.loresuelvo.consumer.ui.screens.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.loresuelvo.consumer.ui.navigation.Route

/**
 * Compose bridge for the AI diagnostic chat screen. Resolves the
 * [ChatViewModel] through Hilt and wires the navigation callbacks.
 *
 * Two Hilt VMs are hosted by the route:
 *
 *  - [ChatViewModel] owns the chat surface (text input, send
 *    round-trip, recommended providers, etc.).
 *  - [AiDiagnosisContactViewModel] owns the AI pre-filled
 *    "Contactar" flow: tapping a recommended provider
 *    triggers `POST /chatbot/conversations/{id}/job-requests`,
 *    the backend's AI fills `title` and `description`, and on
 *    success the route lands on
 *    `Route.Conversation(conversationId)`.
 *
 * The previous "manual contact modal" flow (reusing
 * `ContactProviderViewModel` + `ContactProviderBottomSheet`)
 * is **no longer invoked from this route**. The AI flow and
 * the Professionals flow share the same wire goal — the
 * consumer ends up on `Route.Conversation` — but the AI flow
 * takes a different surface (no modal) because the backend
 * pre-fills the form. The `ContactProviderViewModel` and
 * `ContactProviderBottomSheet` are still used by the
 * Professionals flow under `ui/screens/professional/`.
 */
@Composable
fun ChatRoute(
    navController: NavHostController,
    conversationId: String? = null,
) {
    val viewModel: ChatViewModel = hiltViewModel()
    val aiContactViewModel: AiDiagnosisContactViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsState()

    // Resume a saved AI session when the route was opened with a
    // `conversationId` arg (Assistant list → tap a row). The VM
    // no-ops if the same conversation is already loaded, so this
    // is safe to fire on every recomposition.
    LaunchedEffect(conversationId) {
        if (!conversationId.isNullOrBlank()) {
            viewModel.loadExisting(conversationId)
        }
    }

    // Forward the navigation event emitted by the AI contact flow
    // when the round-trip succeeds. The backend's `job-requests`
    // response carries the `conversation_id` the chat pops to.
    LaunchedEffect(aiContactViewModel) {
        aiContactViewModel.events.collect { event ->
            when (event) {
                is AiDiagnosisContactEvent.NavigateToConversation ->
                    navController.navigate(
                        Route.Conversation.buildPath(event.conversationId),
                    )
            }
        }
    }

    // Gallery picker for the AI diagnostic chat. 01-AIP wires
    // the gallery path only; camera (02-AIP) adds its own
    // launcher in its respective commit. The launcher is
    // remembered at the route level so the result callback
    // survives recompositions.
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            viewModel.onAttachImageFromGallery(uri)
        }
    }

    ChatScreen(
        promptInput = state.promptInput,
        canSend = state.canSend,
        sending = state.sending,
        messages = state.messages,
        assessment = state.assessment,
        recommendedProviders = state.recommendedProviders,
        transientError = state.transientError,
        preliminaryWarningVisible = state.preliminaryWarningVisible,
        pendingAttachments = state.pendingAttachments,
        onPromptChange = viewModel::onPromptChange,
        onSendClick = viewModel::onSendClick,
        onRetryClick = viewModel::onRetryClick,
        onErrorDismiss = viewModel::onErrorDismiss,
        onContactClick = { provider ->
            aiContactViewModel.onContactProviderClick(
                provider,
                state.conversationId,
            )
        },
        onBackClick = { navController.popBackStack() },
        onAttachImageFromGallery = {
            galleryLauncher.launch(
                PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly,
                ),
            )
        },
    )
}
