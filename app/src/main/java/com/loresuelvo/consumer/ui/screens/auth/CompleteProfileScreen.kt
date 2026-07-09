package com.loresuelvo.consumer.ui.screens.auth

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.ui.auth.CompleteProfileError
import com.loresuelvo.consumer.ui.auth.CompleteProfileEvent
import com.loresuelvo.consumer.ui.components.branding.AppLogo
import com.loresuelvo.consumer.ui.components.buttons.PrimaryButton
import com.loresuelvo.consumer.ui.components.cards.AuthCard
import com.loresuelvo.consumer.ui.components.inputs.PrimaryTextField
import com.loresuelvo.consumer.ui.theme.AppBackgroundBottom
import com.loresuelvo.consumer.ui.theme.AppBackgroundMiddle
import com.loresuelvo.consumer.ui.theme.AppBackgroundTop
import com.loresuelvo.consumer.ui.theme.SubtitleGray
import com.loresuelvo.consumer.ui.theme.TextWhite

/**
 * Composable for the `CompleteProfile` screen. Stateless: every
 * value the screen renders is passed in. The host ([com.loresuelvo.consumer.MainActivity])
 * collects the [com.loresuelvo.consumer.ui.auth.CompleteProfileViewModel]
 * state and forwards user input back.
 *
 * Strings are externalized to `strings.xml` (es + en) per the
 * `i18n` rule in AGENTS.md. The error-to-message mapping is the
 * only place where typed [CompleteProfileError] values become
 * localized strings.
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AppBackgroundTop,
                        AppBackgroundMiddle,
                        AppBackgroundBottom
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppLogo()

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.complete_profile_title),
            style = MaterialTheme.typography.displaySmall,
            color = TextWhite,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.complete_profile_subtitle),
            style = MaterialTheme.typography.titleMedium,
            color = TextWhite.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        AuthCard(modifier = Modifier.fillMaxWidth()) {
            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            PrimaryTextField(
                value = firstName,
                label = stringResource(R.string.complete_profile_field_first_name),
                placeholder = stringResource(R.string.complete_profile_field_first_name_placeholder),
                onValueChange = onFirstNameChange,
                testTag = "first-name"
            )

            Spacer(modifier = Modifier.height(16.dp))

            PrimaryTextField(
                value = lastName,
                label = stringResource(R.string.complete_profile_field_last_name),
                placeholder = stringResource(R.string.complete_profile_field_last_name_placeholder),
                onValueChange = onLastNameChange,
                testTag = "last-name"
            )

            Spacer(modifier = Modifier.height(24.dp))

            PrimaryButton(
                text = stringResource(R.string.complete_profile_button_continue),
                onClick = onContinueClick,
                enabled = !loading,
            )

            if (loading) {
                Spacer(modifier = Modifier.height(12.dp))
                CircularProgressIndicator(
                    color = TextWhite,
                    modifier = Modifier
                        .height(32.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.complete_profile_footer),
                style = MaterialTheme.typography.bodySmall,
                color = SubtitleGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Maps a typed [CompleteProfileError] to a localized message. The
 * composable layer never sees the typed error directly; it only
 * sees a String, which keeps the screen test simple and the
 * translation boundary clean.
 */
@Composable
private fun errorToMessage(error: CompleteProfileError): String = when (error) {
    is CompleteProfileError.MissingFirstName ->
        stringResource(R.string.complete_profile_error_missing_first_name)
    is CompleteProfileError.MissingLastName ->
        stringResource(R.string.complete_profile_error_missing_last_name)
    is CompleteProfileError.Network ->
        stringResource(R.string.complete_profile_error_network)
    is CompleteProfileError.Server ->
        stringResource(R.string.complete_profile_error_server, error.code, error.message)
    is CompleteProfileError.Unauthorized ->
        stringResource(R.string.complete_profile_error_unauthorized, error.message)
}
