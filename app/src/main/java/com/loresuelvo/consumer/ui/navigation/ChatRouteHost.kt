package com.loresuelvo.consumer.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.loresuelvo.consumer.ui.screens.assistant.AssistantScreen
import com.loresuelvo.consumer.ui.screens.assistant.AssistantViewModel
import com.loresuelvo.consumer.ui.screens.messages.MessagesScreen
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/** Route hosts for messages, assistant and conversation media effects. */

@Composable
internal fun MessagesRoute(
    navController: NavHostController,
) {
    val viewModel: com.loresuelvo.consumer.ui.screens.messages.MessagesListViewModel =
        hiltViewModel()
    val state by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.load()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    MessagesScreen(
        state = state,
        onRetryClick = viewModel::load,
        onConversationClick = { conversationId ->
            navController.navigate(Route.Conversation.buildPath(conversationId))
        },
    )
}

@Composable
internal fun AssistantRoute(
    navController: NavHostController,
) {
    val viewModel: AssistantViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.retry()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AssistantScreen(
        state = state,
        onRetryClick = viewModel::retry,
        onConversationClick = { conversationId ->
            navController.navigate(Route.Chat.buildPath(conversationId = conversationId))
        },
    )
}

/**
 * Conversation host. Activity-result launchers live here with the
 * ConversationViewModel that consumes their results, so adding another
 * media source does not grow the application navigation composition root.
 */
@Composable
internal fun ConversationRoute(
    navController: NavHostController,
    conversationId: String,
) {
    val viewModel: com.loresuelvo.consumer.ui.screens.chat.ConversationViewModel =
        hiltViewModel()
    val proposalSummaryViewModel:
        com.loresuelvo.consumer.ui.screens.chat.ConversationProposalSummaryViewModel =
        hiltViewModel()
    val state by viewModel.uiState.collectAsState()
    val proposalSummaryState by proposalSummaryViewModel.uiState.collectAsState()

    LaunchedEffect(conversationId) {
        viewModel.load(conversationId)
    }
    LaunchedEffect(conversationId) {
        proposalSummaryViewModel.load(conversationId)
    }

    val sheetState = remember { mutableStateOf(false) }
    val showAttachSheet = sheetState.value
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            viewModel.onAttachImageFromGallery(uri)
        }
        sheetState.value = false
    }

    val context = LocalContext.current
    val cameraOutputUriState = remember { mutableStateOf<android.net.Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = cameraOutputUriState.value
        if (success && uri != null) {
            viewModel.onAttachImageFromGallery(uri)
        }
        cameraOutputUriState.value = null
        sheetState.value = false
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.onStartAudioRecording()
        }
    }

    com.loresuelvo.consumer.ui.screens.chat.ConversationScreen(
        state = state,
        proposalSummaryState = proposalSummaryState,
        actions = com.loresuelvo.consumer.ui.screens.chat.ConversationScreenActions(
            navigation = com.loresuelvo.consumer.ui.screens.chat.ConversationScreenActions.Navigation(
                onBack = { navController.popBackStack() },
                onViewWorkOrder = { workOrderId ->
                    navController.navigate(Route.WorkOrderDetail.buildPath(workOrderId))
                },
            ),
            composer = com.loresuelvo.consumer.ui.screens.chat.ConversationScreenActions.Composer(
                onPromptChange = viewModel::onPromptChange,
                onSend = viewModel::onSendClick,
                onAttach = { sheetState.value = true },
                onStartAudioRecording = {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO,
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasPermission) {
                        viewModel.onStartAudioRecording()
                    } else {
                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onStopAudioRecording = viewModel::onStopAudioRecording,
            ),
            media = com.loresuelvo.consumer.ui.screens.chat.ConversationScreenActions.Media(
                showAttachSheet = showAttachSheet,
                onGallery = {
                    galleryLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly,
                        ),
                    )
                },
                onCamera = {
                    val uri = createCameraOutputUri(context)
                    cameraOutputUriState.value = uri
                    cameraLauncher.launch(uri)
                },
                onConfirmSend = viewModel::onConfirmMediaSend,
                onDiscard = viewModel::onDiscardMediaPreview,
                onErrorDismiss = viewModel::onErrorDismiss,
                onAttachSheetDismiss = { sheetState.value = false },
            ),
            playback = com.loresuelvo.consumer.ui.screens.chat.ConversationScreenActions.Playback(
                onPlayAudio = viewModel::onPlayAudio,
                onPauseAudio = viewModel::onPauseAudio,
                onImageClick = viewModel::onImageClick,
                onFullscreenImageDismiss = viewModel::onFullscreenImageDismiss,
            ),
            errors = com.loresuelvo.consumer.ui.screens.chat.ConversationScreenActions.Errors(
                onRetry = { viewModel.load(conversationId) },
                onDismiss = viewModel::onErrorDismiss,
                onScrollPositionChanged = viewModel::onScrollPositionChanged,
                onUnreadBannerTapped = viewModel::onUnreadBannerTapped,
            ),
        ),
    )
}

private fun createCameraOutputUri(context: android.content.Context): android.net.Uri {
    val cameraDir = java.io.File(context.cacheDir, "camera").apply { mkdirs() }
    val file = java.io.File(cameraDir, "capture_${System.currentTimeMillis()}.jpg")
    val authority = "${context.packageName}.fileprovider"
    return androidx.core.content.FileProvider.getUriForFile(context, authority, file)
}
