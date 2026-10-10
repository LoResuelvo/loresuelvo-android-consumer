package com.loresuelvo.consumer.ui.screens.professional

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.ui.components.images.JobRequestImageAttachmentSelector
import com.loresuelvo.consumer.ui.components.provider.ProviderVerificationBadge
import com.loresuelvo.consumer.ui.theme.SubtitleGray

@Composable
fun ContactProviderBottomSheet(
    state: ContactProviderFormState,
    actions: ContactProviderBottomSheetActions = ContactProviderBottomSheetActions(),
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()

            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProviderAvatar(
                name = state.provider.name,
                profilePhotoUrl = state.provider.profilePhotoUrl,
                size = 48.dp,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "${state.provider.name} ${state.provider.surname}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (state.provider.identityVerified) {
                        ProviderVerificationBadge()
                    }
                }
                Text(
                    text = state.provider.categoryName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SubtitleGray,
                )
            }
        }

        Spacer(Modifier.size(4.dp))

        Text(
            text = stringResource(R.string.contact_provider_modal_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.contact_provider_modal_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = SubtitleGray,
        )

        Spacer(Modifier.size(4.dp))

        OutlinedTextField(
            value = state.title,
            onValueChange = actions.onTitleChange,
            label = { Text(stringResource(R.string.contact_provider_field_title)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(CONTACT_PROVIDER_TITLE_FIELD_TAG),
            singleLine = true,
            enabled = !state.isSubmitting,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                keyboardType = KeyboardType.Text,
            ),
        )

        OutlinedTextField(
            value = state.description,
            onValueChange = actions.onDescriptionChange,
            label = { Text(stringResource(R.string.contact_provider_field_description)) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .testTag(CONTACT_PROVIDER_DESCRIPTION_FIELD_TAG),
            minLines = 4,
            maxLines = 6,
            enabled = !state.isSubmitting,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                keyboardType = KeyboardType.Text,
            ),
        )

        JobRequestImageAttachmentSelector(
            images = state.attachedImages,
            onAttachClick = actions.onAttachImages,
            onRemove = actions.onRemoveImage,
            error = state.attachmentError,
        )

        if (state.error != null) {
            val errorMessage = when (val error = requireNotNull(state.error)) {
                ContactProviderError.Network ->
                    stringResource(R.string.contact_provider_error_network)
                ContactProviderError.Unauthorized ->
                    stringResource(R.string.contact_provider_error_unauthorized)
                is ContactProviderError.Server -> error.message
            }
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(CONTACT_PROVIDER_ERROR_TAG),
            ) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }

        Spacer(Modifier.size(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(
                space = 12.dp,
                alignment = Alignment.End,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = actions.onCancel,
                enabled = !state.isSubmitting,
                modifier = Modifier.testTag(CONTACT_PROVIDER_CANCEL_BUTTON_TAG),
            ) {
                Text(stringResource(R.string.contact_provider_button_cancel))
            }
            Button(
                onClick = actions.onSubmit,
                enabled = state.canSubmit,
                modifier = Modifier.testTag(CONTACT_PROVIDER_SUBMIT_BUTTON_TAG),
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(stringResource(R.string.contact_provider_button_submit))
                }
            }
        }
    }
}

/**
 * Compose testTag for the title field. Public so the Compose-test
 * in `ContactProviderBottomSheetTest` can locate the field without
 * depending on the placeholder text.
 */
const val CONTACT_PROVIDER_TITLE_FIELD_TAG: String = "contact-provider-title-field"

/**
 * Compose testTag for the description field.
 */
const val CONTACT_PROVIDER_DESCRIPTION_FIELD_TAG: String = "contact-provider-description-field"

/**
 * Compose testTag for the inline error surface. Asserting this
 * node exists means the typed [ContactProviderError] was rendered
 * to the user (vs silently swallowed).
 */
const val CONTACT_PROVIDER_ERROR_TAG: String = "contact-provider-error"

/**
 * Compose testTag for the Cancel button.
 */
const val CONTACT_PROVIDER_CANCEL_BUTTON_TAG: String = "contact-provider-cancel"

/**
 * Compose testTag for the Submit button.
 */
const val CONTACT_PROVIDER_SUBMIT_BUTTON_TAG: String = "contact-provider-submit"
