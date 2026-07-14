package com.loresuelvo.consumer.ui.navigation

import android.net.Uri

sealed class Route(val path: String) {
    data object Welcome : Route("welcome")
    data object CompleteProfile : Route("complete_profile")
    data object Home : Route("home")

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
