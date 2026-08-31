package com.loresuelvo.consumer.domain.jobrequest

import com.loresuelvo.consumer.domain.conversation.MediaUpload

/**
 * Limits the consumer app enforces on images attached to a job
 * request. Mirrors the backend's `UploadPolicy` table for the
 * `job_request_image` purpose; kept here (domain) so both the
 * UI selector and the future submit-side validation read the
 * same source of truth.
 *
 *  - [MAX_JOB_REQUEST_IMAGES]: hard cap on how many images one
 *    job request can carry. Matches the webapp's
 *    `ImageAttachmentSelector` default (52.3-AIJR).
 *  - [MAX_IMAGE_BYTES]: per-file size cap. Mirrors the chat's
 *    `CONVERSATION_MESSAGE_IMAGE` cap (5 MiB) since the upload
 *    pipeline reuses the same presign → upload → confirm flow.
 *  - [ALLOWED_IMAGE_MIME_TYPES]: the picker filters out anything
 *    outside this set before staging the file.
 */
const val MAX_JOB_REQUEST_IMAGES: Int = 3
const val MAX_IMAGE_BYTES: Long = 5L * 1024L * 1024L
val ALLOWED_IMAGE_MIME_TYPES: Set<String> = setOf(
    "image/jpeg",
    "image/png",
    "image/webp",
)

/**
 * Outcome of validating a batch of images the consumer is
 * trying to attach to a job request. The UI selector renders
 * the rejection copy for [LimitReached] and drops the offending
 * batch; other variants are reserved for the future submit-side
 * validation that mirrors this contract.
 */
sealed interface ImageAttachmentRejection {
    data object LimitReached : ImageAttachmentRejection
    data object Empty : ImageAttachmentRejection
}

/**
 * Result of attaching one or more images to a job request. The
 * UI selector drives its `error` slot off [rejection] and lets
 * the user retry with a smaller batch.
 */
sealed interface ImageAttachmentOutcome {
    data class Accepted(
        val images: List<MediaUpload.Image>,
    ) : ImageAttachmentOutcome

    data class Rejected(
        val rejection: ImageAttachmentRejection,
    ) : ImageAttachmentOutcome
}
