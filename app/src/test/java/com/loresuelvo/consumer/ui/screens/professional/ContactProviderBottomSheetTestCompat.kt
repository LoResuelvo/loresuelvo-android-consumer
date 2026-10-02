package com.loresuelvo.consumer.ui.screens.professional

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.loresuelvo.consumer.domain.conversation.MediaUpload
import com.loresuelvo.consumer.domain.provider.Provider

/** Test-only adapter for the pre-refactor fixture signature. */
@Composable
fun ContactProviderBottomSheet(
    provider: Provider,
    title: String,
    description: String,
    canSubmit: Boolean,
    isSubmitting: Boolean,
    error: ContactProviderError?,
    attachedImages: List<MediaUpload.Image>,
    attachmentError: String?,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onAttachImagesClick: () -> Unit,
    onRemoveImage: (Int) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ContactProviderBottomSheet(
        state = ContactProviderFormState(
            provider = provider,
            title = title,
            description = description,
            canSubmit = canSubmit,
            isSubmitting = isSubmitting,
            error = error,
            attachedImages = attachedImages,
            attachmentError = attachmentError,
        ),
        actions = ContactProviderBottomSheetActions(
            onTitleChange = onTitleChange,
            onDescriptionChange = onDescriptionChange,
            onAttachImages = onAttachImagesClick,
            onRemoveImage = onRemoveImage,
            onSubmit = onSubmit,
            onCancel = onCancel,
        ),
        modifier = modifier,
    )
}
