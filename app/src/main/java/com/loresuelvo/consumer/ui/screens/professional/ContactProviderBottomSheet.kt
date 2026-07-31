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
import com.loresuelvo.consumer.domain.provider.Provider
import com.loresuelvo.consumer.ui.theme.SubtitleGray

/**
 * Stateless content for the contact-provider bottom sheet. Renders
 * the provider's avatar, name and category up top, the modal
 * title + subtitle, the two required fields, an inline error
 * message (when the previous submit failed), and the Cancel /
 * Submit action row.
 *
 * The host ([ProfessionalsScreen]) is responsible for wrapping
 * this content in a `ModalBottomSheet` and toggling visibility
 * from the `ContactProviderUiState`. Keeping the sheet
 * **stateless** means the screen — not the composable — owns
 * the form's lifecycle, which is exactly what the ViewModel
 * already models.
 *
 * Layout rules (per the user's UX brief):
 *  - Light typography, generous whitespace, rounded corners.
 *  - Title (the modal's heading) sits BELOW the provider
 *    header so the consumer always sees who they're writing to.
 *  - Submit button is a filled `Button`; Cancel is a `TextButton`.
 *  - When `isSubmitting` is true, the submit button shows a
 *    compact progress indicator instead of a label so the round-
 *    trip's loading state is unmistakable.
 *
 * `testTag`s are exposed publicly so the Compose-test in
 * `ContactProviderBottomSheetTest` can locate the form fields and
 * the action buttons without depending on text content.
 */
@Composable
fun ContactProviderBottomSheet(
    provider: Provider,
    title: String,
    description: String,
    canSubmit: Boolean,
    isSubmitting: Boolean,
    error: ContactProviderError?,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            // Scrollable so the form keeps working on small phones
            // (and the keyboard inset never hides the submit button).
            // The ModalBottomSheet host already biases the sheet
            // toward the bottom of the screen; verticalScroll lets
            // the user reach the action buttons when the IME is
            // open or the description field grows.
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header: provider identity so the consumer always knows
        // who they are starting a conversation with.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProviderAvatar(
                name = provider.name,
                profilePhotoUrl = provider.profilePhotoUrl,
                size = 48.dp,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "${provider.name} ${provider.surname}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = provider.categoryName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SubtitleGray,
                )
            }
        }

        Spacer(Modifier.size(4.dp))

        // Modal heading + subtitle.
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

        // Form fields. The label doubles as the hint shown when
        // the field is empty, so we don't need a separate
        // placeholder. KeyboardOptions hint: the title field is
        // a single line, the description grows up to 6 lines.
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text(stringResource(R.string.contact_provider_field_title)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(CONTACT_PROVIDER_TITLE_FIELD_TAG),
            singleLine = true,
            enabled = !isSubmitting,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                keyboardType = KeyboardType.Text,
            ),
        )

        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text(stringResource(R.string.contact_provider_field_description)) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .testTag(CONTACT_PROVIDER_DESCRIPTION_FIELD_TAG),
            minLines = 4,
            maxLines = 6,
            enabled = !isSubmitting,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                keyboardType = KeyboardType.Text,
            ),
        )

        // Inline error message. Renders nothing when `error` is
        // null so the layout shifts gracefully across attempts.
        if (error != null) {
            val errorMessage = when (error) {
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

        // Action row. Cancel on the left (text button), Submit on
        // the right (filled button, primary colour). The submit
        // button becomes a circular progress indicator while the
        // round-trip is in flight so the user sees the action
        // without an ambiguous "Send" button that ignores input.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(
                space = 12.dp,
                alignment = Alignment.End,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onCancel,
                enabled = !isSubmitting,
                modifier = Modifier.testTag(CONTACT_PROVIDER_CANCEL_BUTTON_TAG),
            ) {
                Text(stringResource(R.string.contact_provider_button_cancel))
            }
            Button(
                onClick = onSubmit,
                enabled = canSubmit,
                modifier = Modifier.testTag(CONTACT_PROVIDER_SUBMIT_BUTTON_TAG),
            ) {
                if (isSubmitting) {
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
