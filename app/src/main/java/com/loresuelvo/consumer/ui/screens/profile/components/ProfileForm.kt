package com.loresuelvo.consumer.ui.screens.profile.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.ui.components.buttons.PrimaryButton
import com.loresuelvo.consumer.ui.components.inputs.PrimaryTextField
import com.loresuelvo.consumer.ui.screens.profile.CompleteProfileError
import com.loresuelvo.consumer.ui.theme.SubtitleGray
import com.loresuelvo.consumer.ui.theme.TextWhite

/**
 * Form block for the CompleteProfile screen: optional error
 * banner, two text fields (first / last name), the continue button,
 * an inline progress indicator while submitting, and the privacy
 * note at the bottom.
 *
 * Stateless: every value the user types and every callback the
 * parent invokes is passed in. Error-to-message mapping lives in
 * the parent screen so this component stays presentation-only.
 */
@Composable
fun ProfileForm(
    firstName: String,
    lastName: String,
    loading: Boolean,
    errorMessage: String?,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onContinueClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(28.dp)) {
            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            PrimaryTextField(
                value = firstName,
                label = stringResource(R.string.complete_profile_field_first_name),
                placeholder = stringResource(R.string.complete_profile_field_first_name_placeholder),
                onValueChange = onFirstNameChange,
                testTag = "first-name",
            )

            Spacer(modifier = Modifier.height(16.dp))

            PrimaryTextField(
                value = lastName,
                label = stringResource(R.string.complete_profile_field_last_name),
                placeholder = stringResource(R.string.complete_profile_field_last_name_placeholder),
                onValueChange = onLastNameChange,
                testTag = "last-name",
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
                        .align(Alignment.CenterHorizontally),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.complete_profile_footer),
                style = MaterialTheme.typography.bodySmall,
                color = SubtitleGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Typed error → localized message bridge. Lives next to the form
 * because it's a UI concern (it consumes `stringResource`); the
 * screen-level orchestrator calls into it before rendering the
 * form, so the form itself stays presentation-only.
 */
@Composable
internal fun errorToMessage(error: CompleteProfileError): String = when (error) {
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