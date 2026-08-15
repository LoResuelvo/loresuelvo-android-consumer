package com.loresuelvo.consumer.testdi

import android.net.Uri
import com.loresuelvo.consumer.data.media.MediaReader
import com.loresuelvo.consumer.domain.conversation.MediaUpload
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Test-only [MediaReader] that hands back a deterministic
 * in-memory JPEG for any URI the caller hands it. The acceptance
 * tests don't exercise the actual gallery flow (the
 * `PickVisualMedia` activity is a system surface that an
 * instrumented test cannot drive deterministically); they wire
 * the VM with this fake so the VM's
 * [com.loresuelvo.consumer.ui.screens.chat.ConversationViewModel.onAttachImageFromGallery]
 * path resolves its dependency graph without crashing on the
 * real [com.loresuelvo.consumer.data.media.AndroidMediaReader]'s
 * `ContentResolver` call.
 *
 * Mirrors the discipline of [FakeConversationRepository] /
 * [com.loresuelvo.consumer.acceptance.diagnosis.FakeDiagnosisRepository]:
 * self-contained, `@Singleton`, `@Inject`-constructable so the
 * `@TestInstallIn` modules can bind it with a one-line
 * `@Binds` declaration.
 */
@Singleton
class FakeMediaReader @Inject constructor() : MediaReader {
    override suspend fun read(uri: Uri): MediaUpload.Image =
        MediaUpload.Image(
            bytes = byteArrayOf(),
            mimeType = "image/jpeg",
            originalName = uri.lastPathSegment ?: "test.jpg",
        )
}