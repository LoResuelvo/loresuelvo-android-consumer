package com.loresuelvo.consumer.data.media

import android.net.Uri
import com.loresuelvo.consumer.domain.conversation.MediaUpload

/**
 * Port that converts a `content://` / `file://` URI handed by
 * an Android picker or in-app recorder into a domain
 * [MediaUpload]. Keeps the
 * [com.loresuelvo.consumer.ui.screens.chat.ConversationViewModel]
 * free of `android.content.ContentResolver` so the JVM unit tests
 * can swap a `FakeMediaReader` and assert the resulting
 * `pendingMedia` state without spinning up Robolectric.
 *
 * The reader never throws on a missing / unreadable URI:
 * unrecoverable I/O errors bubble up as the underlying
 * [java.io.IOException] so the calling layer (the VM in this
 * project) can translate it into a typed failure for the UI.
 *
 * The implementation lives in
 * [com.loresuelvo.consumer.data.media.AndroidMediaReader]; it
 * is the only place in the codebase that touches
 * `android.provider.OpenableColumns` /
 * `android.content.ContentResolver`.
 */
interface MediaReader {

    /**
     * Reads the bytes / mime / display name of the file the URI
     * points at and packages them as a polymorphic
     * [MediaUpload]. The polymorphic shape lets a future audio
     * scenario land without changing the VM call site; the
     * reader inspects the URI's mime type and dispatches.
     *
     * @throws java.io.IOException when the URI cannot be opened
     *  (revoked permission, missing provider, deleted file).
     */
    suspend fun read(uri: Uri): MediaUpload
}