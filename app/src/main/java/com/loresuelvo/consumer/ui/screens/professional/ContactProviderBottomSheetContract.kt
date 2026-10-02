package com.loresuelvo.consumer.ui.screens.professional

import com.loresuelvo.consumer.domain.conversation.MediaUpload
import com.loresuelvo.consumer.domain.provider.Provider

/** Immutable rendering model for the contact-provider sheet. */
data class ContactProviderFormState(
    val provider: Provider,
    val title: String = "",
    val description: String = "",
    val canSubmit: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: ContactProviderError? = null,
    val attachedImages: List<MediaUpload.Image> = emptyList(),
    val attachmentError: String? = null,
)

/** Callback contract grouped by form and attachment responsibility. */
data class ContactProviderBottomSheetActions(
    val onTitleChange: (String) -> Unit = {},
    val onDescriptionChange: (String) -> Unit = {},
    val onAttachImages: () -> Unit = {},
    val onRemoveImage: (Int) -> Unit = {},
    val onSubmit: () -> Unit = {},
    val onCancel: () -> Unit = {},
)
