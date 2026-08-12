package com.loresuelvo.consumer.ui.screens.chat

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.loresuelvo.consumer.ui.navigation.Route
import com.loresuelvo.consumer.ui.screens.professional.ContactProviderBottomSheet
import com.loresuelvo.consumer.ui.screens.professional.ContactProviderEvent
import com.loresuelvo.consumer.ui.screens.professional.ContactProviderUiState
import com.loresuelvo.consumer.ui.screens.professional.ContactProviderViewModel

/**
 * Compose bridge for the AI diagnostic chat screen. Resolves the
 * [ChatViewModel] through Hilt and wires the navigation callbacks.
 *
 * Today this route handles:
 *  - the back press (the smart router is responsible for re-deriving
 *    the start destination based on the session, so plain
 *    `popBackStack` is enough to return to Home);
 *  - the `onContactClick(Provider)` propagation from the diagnosis
 *    summary carousel into the contact-provider flow.
 *
 * The contact flow itself is reused from the Professionals screen
 * via [ContactProviderViewModel]: the route hosts a second Hilt
 * VM scoped to the chat back-stack entry (one VM per entry, so
 * navigating Chat → Contactar does NOT bleed into Professionals
 * and vice-versa). When the contact form submits successfully the
 * VM emits
 * [ContactProviderEvent.NavigateToConversation] which the route
 * forwards to the [NavHostController]. The `POST /job-requests`
 * plumbing and the bottom-sheet UI live in
 * `ui/screens/professional/` and are reused verbatim — no
 * surface duplication.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoute(
    navController: NavHostController,
) {
    val viewModel: ChatViewModel = hiltViewModel()
    val contactViewModel: ContactProviderViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsState()
    val contactState by contactViewModel.uiState.collectAsState()

    // Forward the navigation event emitted by the contact form
    // when `POST /job-requests` succeeds. Mirrors the pattern
    // already used by `ProfessionalsRoute` in
    // `ui/navigation/LoResuelvoNav.kt`.
    LaunchedEffect(contactViewModel) {
        contactViewModel.events.collect { event ->
            when (event) {
                is ContactProviderEvent.NavigateToConversation ->
                    navController.navigate(
                        Route.Conversation.buildPath(event.conversationId),
                    )
            }
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
        onPromptChange = viewModel::onPromptChange,
        onSendClick = viewModel::onSendClick,
        onRetryClick = viewModel::onRetryClick,
        onErrorDismiss = viewModel::onErrorDismiss,
        onContactClick = contactViewModel::onOpenContact,
        onBackClick = { navController.popBackStack() },
    )

    // Contact-provider bottom sheet — same host pattern as
    // ProfessionalsScreen.kt. Renders only when the contact VM
    // exposes an Open state (the modal is dismissed by transitioning
    // back to Closed). The `onContactClick` wiring above is the
    // entry point that flips the VM state from Closed → Open.
    val openState = contactState as? ContactProviderUiState.Open
    if (openState != null) {
        val sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )
        ModalBottomSheet(
            onDismissRequest = contactViewModel::onCancel,
            sheetState = sheetState,
        ) {
            ContactProviderBottomSheet(
                provider = openState.provider,
                title = openState.title,
                description = openState.description,
                canSubmit = openState.canSubmit,
                isSubmitting = openState.isSubmitting,
                error = openState.error,
                onTitleChange = contactViewModel::onTitleChange,
                onDescriptionChange = contactViewModel::onDescriptionChange,
                onSubmit = contactViewModel::onSubmit,
                onCancel = contactViewModel::onCancel,
            )
        }
    }
}
