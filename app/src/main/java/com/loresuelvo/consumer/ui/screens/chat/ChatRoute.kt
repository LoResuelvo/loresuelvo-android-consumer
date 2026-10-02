package com.loresuelvo.consumer.ui.screens.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    var sheetVisible by remember { mutableStateOf(false) }

    val cameraOutputUriFactory = hiltViewModel<CameraOutputUriFactoryHolder>().factory

    LaunchedEffect(conversationId) {
        if (!conversationId.isNullOrBlank()) {
            viewModel.loadExisting(conversationId)
        }
    }

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

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            viewModel.onAttachImageFromGallery(uri)
        }
        sheetVisible = false
    }

    var cameraOutputUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = cameraOutputUri
        if (success && uri != null) {
            viewModel.onAttachImageFromCamera(uri)
        }
        cameraOutputUri = null
        sheetVisible = false
    }

    ChatScreen(
        state = state,
        actions = ChatScreenActions(
            composer = ChatScreenActions.Composer(
                onPromptChange = viewModel::onPromptChange,
                onSend = viewModel::onSendClick,
            ),
            diagnosis = ChatScreenActions.Diagnosis(
                onContact = { provider ->
                    aiContactViewModel.onContactProviderClick(
                        provider,
                        state.conversationId,
                    )
                },
                onViewProfile = { provider ->
                    navController.navigate(Route.ProviderProfile.buildPath(provider.id))
                },
            ),
            navigation = ChatScreenActions.Navigation(
                onBack = { navController.popBackStack() },
            ),
            media = ChatScreenActions.Media(
                onAttach = { sheetVisible = true },
                onGallery = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly,
                        ),
                    )
                },
                onCamera = {
                    val uri = cameraOutputUriFactory.createCameraOutputUri()
                    cameraOutputUri = uri
                    cameraLauncher.launch(uri)
                },
                onConfirmSend = { index ->

                    @Suppress("UNUSED_PARAMETER") index
                },
                onDiscard = viewModel::onRemoveAttachment,
                showAttachSheet = sheetVisible,
                onAttachSheetDismiss = { sheetVisible = false },

                audioEnabled = state.audioEnabled,
            ),
            errors = ChatScreenActions.Errors(
                onRetry = viewModel::onRetryClick,
                onDismiss = viewModel::onErrorDismiss,
            ),
        ),
    )
}
