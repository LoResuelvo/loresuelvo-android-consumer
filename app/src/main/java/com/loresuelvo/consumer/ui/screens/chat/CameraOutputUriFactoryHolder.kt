package com.loresuelvo.consumer.ui.screens.chat

import androidx.lifecycle.ViewModel
import com.loresuelvo.consumer.data.media.MediaOutputUriFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Hilt wrapper around [MediaOutputUriFactory] so the [ChatRoute]
 * Composable can obtain the singleton via `hiltViewModel()` without
 * wiring an `EntryPoint` accessor. The factory itself is stateless
 * (it only reads `Context.cacheDir` / `Context.packageName` to build
 * the cache file + `FileProvider` URI), so wrapping it in a
 * [ViewModel] does not leak any state — the [ViewModel] is just a
 * vehicle for Hilt's constructor injection graph.
 *
 * The wrapper is local to the AI chat surface so the chat-with-
 * provider surface (which uses the same factory through its own
 * `LoResuelvoNav` route) keeps its wiring independent.
 */
@HiltViewModel
class CameraOutputUriFactoryHolder @Inject constructor(
    val factory: MediaOutputUriFactory,
) : ViewModel()