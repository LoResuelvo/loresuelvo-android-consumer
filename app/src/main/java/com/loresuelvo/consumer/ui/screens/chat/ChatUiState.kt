package com.loresuelvo.consumer.ui.screens.chat

import com.loresuelvo.consumer.domain.diagnosis.ChatMessage
import com.loresuelvo.consumer.domain.diagnosis.DiagnosisAssessment
import com.loresuelvo.consumer.domain.provider.Provider

/**
 * UDF state for the AI diagnostic chat screen.
 *
 *  - [preliminaryWarningVisible]: always `true` while the chat is
 *    open, controls whether the orientation-preliminary banner
 *    renders above the messages. The text comes from the
 *    `chat_preliminary_warning` resource (so it tracks the
 *    active locale). Carried as a flag rather than a literal
 *    string to keep the Spanish copy out of `app/src/main/java/`
 *    per the AGENTS.md i18n rule.
 *  - [assessment]: structured AI outcome (`outcome` + matched
 *    `problem_category`). `null` while the AI is still asking
 *    follow-up questions. The UI gates the summary card on
 *    `assessment?.isProfessionalRequired == true` per the
 *    09-DIA / 10-DIA scenarios.
 *  - [recommendedProviders]: providers the AI suggests for the
 *    matched category. Populated alongside the
 *    `professional_required` outcome.
 *  - [pendingAttachments]: images the consumer attached from the
 *    gallery / camera but has not yet sent. Each entry is a
 *    [PendingMedia] of kind [PendingMediaKind.IMAGE] carrying the
 *    read bytes + mime + original name. The UI renders the
 *    preview grid from this list. Introduced by 01-AIP; cleared
 *    on send-success or explicit discard (05-AIP).
 */
data class ChatUiState(
    val placeholderBody: String = "",
    val promptInput: String = "",
    val sending: Boolean = false,
    val conversationId: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val assessment: DiagnosisAssessment? = null,
    val recommendedProviders: List<Provider>? = null,
    val transientError: ChatError? = null,
    val lastAttemptedPrompt: String? = null,
    val preliminaryWarningVisible: Boolean = true,
    val pendingAttachments: List<PendingMedia> = emptyList(),
    /**
     * Attachments the upload pipeline confirmed before the
     * send endpoint rejected (10-AIP). Distinct from
     * [pendingAttachments] which holds bytes that still need a
     * fresh upload on retry (08-AIP). The UI renders this
     * snapshot below the optimistic user bubble so the consumer
     * keeps context of what they tried to send across a failed
     * round-trip.
     */
    val sentAttachments: List<PendingMedia> = emptyList(),
    /**
     * File IDs the orchestrator returned for the attachments
     * in [sentAttachments], aligned by index. The VM uses this
     * snapshot on retry (11-AIP) so the chat message endpoint
     * is called again with the cached IDs instead of re-running
     * the presign → upload → confirm pipeline.
     */
    val sentAttachmentFileIds: List<String> = emptyList(),
    /**
     * Whether the AI diagnostic chat surface currently exposes
     * the audio recording affordance. Defaults to `false` while
     * the underlying AI audio functionality is not available
     * (scenario 01-UXUI) so the Mic / Stop buttons are hidden
     * from the user. The chat-with-provider surface uses a
     * separate [com.loresuelvo.consumer.ui.screens.chat.ConversationUiState]
     * and keeps audio enabled regardless of this flag.
     */
    val audioEnabled: Boolean = false,
) {
    val canSend: Boolean get() =
        (promptInput.trim().isNotEmpty() || pendingAttachments.isNotEmpty()) && !sending
}
