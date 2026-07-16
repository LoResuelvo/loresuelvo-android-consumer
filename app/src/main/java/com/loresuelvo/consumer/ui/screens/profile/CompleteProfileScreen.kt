package com.loresuelvo.consumer.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.ui.screens.profile.components.ProfileForm
import com.loresuelvo.consumer.ui.screens.profile.components.ProfileHeader
import com.loresuelvo.consumer.ui.screens.profile.components.errorToMessage

/**
 * Composable for the `CompleteProfile` screen. Stateless: every
 * value the screen renders is passed in. The host
 * ([com.loresuelvo.consumer.MainActivity]) collects the
 * [CompleteProfileViewModel] state and forwards user input back.
 *
 * Layout is composed by [ProfileHeader] (logo + hero copy) and
 * [ProfileForm] (text fields, submit button, privacy note); this
 * file only orchestrates them. The error-to-message mapping stays
 * in the form package so it lives next to the strings it consumes.
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        ProfileHeader()

        Spacer(modifier = Modifier.height(28.dp))

        ProfileForm(
            firstName = firstName,
            lastName = lastName,
            loading = loading,
            errorMessage = errorMessage,
            onFirstNameChange = onFirstNameChange,
            onLastNameChange = onLastNameChange,
            onContinueClick = onContinueClick,
        )
    }
}