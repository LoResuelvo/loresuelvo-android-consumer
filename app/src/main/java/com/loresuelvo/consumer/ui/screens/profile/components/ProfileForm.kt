package com.loresuelvo.consumer.ui.screens.profile.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.ui.components.inputs.PrimaryTextField
import com.loresuelvo.consumer.ui.screens.profile.CompleteProfileError

/**
 * Form block for the CompleteProfile screen: optional error
 * banner and two text fields (first / last name) wrapped in a slim
 * card. The continue button and the privacy note moved to the
 * screen-level `bottomBar` so they always sit at the bottom of
 * the viewport, mirroring the "Continuar" pattern of the Welcome
 * screen.
 *
 * Stateless: every value the user types and every callback the
 * parent invokes is passed in.
 */
@Composable
fun ProfileForm(
    firstName: String,
    lastName: String,
    street: String,
    streetNumber: String,
    floor: String,
    unit: String,
    errorMessage: String?,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onStreetChange: (String) -> Unit,
    onStreetNumberChange: (String) -> Unit,
    onFloorChange: (String) -> Unit,
    onUnitChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
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
                onValueChange = onFirstNameChange,
                testTag = "first-name",
            )

            Spacer(modifier = Modifier.height(12.dp))

            PrimaryTextField(
                value = lastName,
                label = stringResource(R.string.complete_profile_field_last_name),
                onValueChange = onLastNameChange,
                testTag = "last-name",
            )

            Spacer(modifier = Modifier.height(12.dp))

            PrimaryTextField(
                value = street,
                label = stringResource(R.string.complete_profile_field_street),
                onValueChange = onStreetChange,
                testTag = "street",
            )

            Spacer(modifier = Modifier.height(12.dp))

            PrimaryTextField(
                value = streetNumber,
                label = stringResource(R.string.complete_profile_field_street_number),
                onValueChange = onStreetNumberChange,
                testTag = "street-number",
            )

            Spacer(modifier = Modifier.height(12.dp))

            PrimaryTextField(
                value = floor,
                label = stringResource(R.string.complete_profile_field_floor),
                onValueChange = onFloorChange,
                testTag = "floor",
            )

            Spacer(modifier = Modifier.height(12.dp))

            PrimaryTextField(
                value = unit,
                label = stringResource(R.string.complete_profile_field_unit),
                onValueChange = onUnitChange,
                testTag = "unit",
            )
        }
    }
}

/**
 * Typed error → localized message bridge. Lives next to the form
 * because it's a UI concern (it consumes `stringResource`); the
 * screen-level orchestrator calls into it before rendering the
 * form so the form itself stays presentation-only.
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
    is CompleteProfileError.MissingStreet ->
        stringResource(R.string.complete_profile_error_missing_street)
    is CompleteProfileError.MissingStreetNumber ->
        stringResource(R.string.complete_profile_error_missing_street_number)
}