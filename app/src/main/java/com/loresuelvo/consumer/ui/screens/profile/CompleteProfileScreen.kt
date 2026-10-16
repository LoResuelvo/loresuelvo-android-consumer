package com.loresuelvo.consumer.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.tappableElement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.ui.components.buttons.PrimaryButton
import com.loresuelvo.consumer.ui.screens.profile.components.ProfileForm
import com.loresuelvo.consumer.ui.screens.profile.components.ProfileHeader
import com.loresuelvo.consumer.ui.screens.profile.components.ProfilePhotoPicker
import com.loresuelvo.consumer.ui.screens.profile.components.ProfilePhotoCropper
import com.loresuelvo.consumer.ui.screens.profile.components.errorToMessage

/**
 * Composable for the `CompleteProfile` screen. Stateless: the host passes
 * one immutable state object and receives user intent through one action
 * dispatcher. The host ([com.loresuelvo.consumer.MainActivity]) collects
 * the [CompleteProfileViewModel] state and forwards actions back.
 *
 * The form scrolls independently from the fixed action area. The
 * keyboard inset is represented by a dedicated spacer so the action
 * area stays immediately above the keyboard without reserving it twice.
 */
@Composable
fun CompleteProfileScreen(
    state: CompleteProfileUiState,
    onAction: (CompleteProfileAction) -> Unit,
) {
    val errorMessage = state.error?.let { errorToMessage(it) }
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val imeBottomInset = WindowInsets.ime.getBottom(density)
    val tappableBottomInset = WindowInsets.tappableElement.getBottom(density)
    val keyboardSpacerHeight = with(density) {
        (imeBottomInset - tappableBottomInset).coerceAtLeast(0).toDp() - 48.dp
    }.coerceAtLeast(0.dp)

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            scrollState.animateScrollTo(0)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    errorMessage?.let { message ->
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    ProfileHeader()

                    Spacer(modifier = Modifier.height(16.dp))

                    ProfilePhotoPicker(
                        state = state,
                        onAction = onAction,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ProfileForm(
                        state = state,
                        onAction = onAction,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PrimaryButton(
                    text = stringResource(R.string.complete_profile_button_continue),
                    onClick = { onAction(CompleteProfileAction.ContinueClicked) },
                    enabled = !state.loading,
                )
                Text(
                    text = stringResource(R.string.complete_profile_footer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(keyboardSpacerHeight))
        }
    }

    state.pendingProfilePhoto?.let { photo ->
        ProfilePhotoCropper(
            photo = photo,
            onConfirm = { croppedPhoto ->
                onAction(CompleteProfileAction.ProfilePhotoCropConfirmed(croppedPhoto))
            },
            onCancel = { onAction(CompleteProfileAction.CancelProfilePhotoCropClicked) },
        )
    }
}
