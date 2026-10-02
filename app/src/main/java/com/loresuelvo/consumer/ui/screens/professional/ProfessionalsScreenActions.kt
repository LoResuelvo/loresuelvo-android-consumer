package com.loresuelvo.consumer.ui.screens.professional

import com.loresuelvo.consumer.domain.provider.Provider

/** Callback contract for the professionals list and contact form. */
data class ProfessionalsScreenActions(
    val onRetry: () -> Unit = {},
    val onContact: (Provider) -> Unit = {},
    val onViewProfile: (Provider) -> Unit = {},
    val contact: ContactFormActions = ContactFormActions(),
) {
    data class ContactFormActions(
        val onTitleChange: (String) -> Unit = {},
        val onDescriptionChange: (String) -> Unit = {},
        val onAttachImages: () -> Unit = {},
        val onRemoveImage: (Int) -> Unit = {},
        val onSubmit: () -> Unit = {},
        val onCancel: () -> Unit = {},
    )
}
