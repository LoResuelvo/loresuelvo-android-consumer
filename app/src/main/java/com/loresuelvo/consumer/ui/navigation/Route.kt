package com.loresuelvo.consumer.ui.navigation

import android.net.Uri

sealed class Route(val path: String) {
    data object Welcome : Route("welcome")
    data object CompleteProfile : Route("complete_profile")
    data object Home : Route("home")

    /**
     * AI diagnostic chat screen. Reached from the `AiSearchBar` on
     * Home (the "Chat con IA" entry point). No arguments yet — the
     * first commit only wires navigation; subsequent commits will
     * carry the conversation id and persisted history.
     */
    data object Chat : Route("chat")

    /**
     * Provider list for a single category. The category name is
     * display-only (rendered in the header) — the underlying query
     * is always by `categoryId`. It is URL-encoded in the path so
     * accents (`Plomería`) survive navigation round-trips.
     */
    data class Professionals(
        val categoryId: Int,
        val categoryName: String,
    ) : Route("professionals/{categoryId}/{categoryName}") {
        companion object {
            fun buildPath(categoryId: Int, categoryName: String): String =
                "professionals/$categoryId/" +
                    Uri.encode(categoryName)
        }
    }
}
