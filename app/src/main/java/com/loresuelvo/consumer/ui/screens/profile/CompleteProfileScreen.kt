package com.loresuelvo.consumer.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.ui.components.buttons.PrimaryButton
import com.loresuelvo.consumer.ui.screens.profile.components.ProfileForm
import com.loresuelvo.consumer.ui.screens.profile.components.ProfileHeader
import com.loresuelvo.consumer.ui.screens.profile.components.errorToMessage

/**
 * Composable for the `CompleteProfile` screen. Stateless: every
 * value the screen renders is passed in. The host
 * ([com.loresuelvo.consumer.MainActivity]) collects the
 * [CompleteProfileViewModel] state and forwards user input back.
 *
 * Layout uses [Scaffold] so the "Continuar" button and the
 * privacy note can pin to the bottom of the viewport via
 * `bottomBar` — they always stay reachable regardless of how the
 * form scrolls or where the soft keyboard lands. The rest of the
 * screen (step indicator + header + form) lives in the
 * scrollable `content`.
 */
@Composable
fun CompleteProfileScreen(
    firstName: String,
    lastName: String,
    loading: Boolean,
    error: CompleteProfileError?,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onContinueClick: () -> Unit,
    onEvent: (CompleteProfileEvent) -> Unit,
) {
    val errorMessage = error?.let { errorToMessage(it) }
    val scrollState = rememberScrollState()

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            scrollState.animateScrollTo(0)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .imePadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PrimaryButton(
                    text = stringResource(R.string.complete_profile_button_continue),
                    onClick = onContinueClick,
                    enabled = !loading,
                )
                Text(
                    text = stringResource(R.string.complete_profile_footer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ProfileHeader()

            Spacer(modifier = Modifier.height(16.dp))

            ProfileForm(
                firstName = firstName,
                lastName = lastName,
                errorMessage = errorMessage,
                onFirstNameChange = onFirstNameChange,
                onLastNameChange = onLastNameChange,
            )
        }
    }
}