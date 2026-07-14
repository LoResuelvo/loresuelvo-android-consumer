package com.loresuelvo.consumer.ui.screens.home.components

import com.loresuelvo.consumer.domain.category.Category

/**
 * Maps a backend [Category] to a Material vector drawable for the
 * Home grid. Lives as a top-level function (not a global `object`)
 * so it follows the project's "no `object` global mutable" rule.
 *
 * Categories outside this map fall back to a generic compass icon
 * so the grid never renders an empty box.
 */
internal fun categoryIconRes(category: Category): Int =
    when (category.name.lowercase()) {
        "plomería", "plomero" -> android.R.drawable.ic_menu_compass
        "gas", "gasista" -> android.R.drawable.ic_menu_send
        "electricidad", "electricista" -> android.R.drawable.ic_menu_view
        "climatización" -> android.R.drawable.ic_menu_compass
        "pintura", "pintor" -> android.R.drawable.ic_menu_edit
        "albañilería" -> android.R.drawable.ic_menu_send
        "carpintería", "carpintero" -> android.R.drawable.ic_menu_crop
        "streamer" -> android.R.drawable.ic_menu_camera
        else -> android.R.drawable.ic_menu_compass
    }
