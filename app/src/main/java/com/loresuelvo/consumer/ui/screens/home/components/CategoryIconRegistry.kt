package com.loresuelvo.consumer.ui.screens.home.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Carpenter
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.FormatPaint
import androidx.compose.material.icons.outlined.Handyman
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Plumbing
import androidx.compose.ui.graphics.vector.ImageVector
import com.loresuelvo.consumer.domain.category.Category

/**
 * Maps a backend [Category] to a Material [ImageVector] for the
 * Home grid. This is the **single source of truth** for category
 * icons: any new category the backend exposes has to be added
 * here (single line), and unknown categories fall back to a
 * generic icon so the grid never renders an empty box.
 *
 * Lives as a top-level function (not a global `object`) so it
 * follows the project's "no `object` global mutable" rule and stays
 * trivially testable. Material Icons come from
 * `androidx.compose.material:material-icons-extended`, the same
 * library the AiSearchBar uses for `SmartToy`, so the visual
 * language stays consistent across the app.
 *
 * The mapping is by `name.lowercase()` (with a few common synonyms
 * like `plomero` / `gasista` / `electricista`) — if the backend ever
 * renames a category in a non-trivial way, the only place to update
 * is this `when` expression.
 */
internal fun categoryIconRes(category: Category): ImageVector =
    when (category.name.lowercase()) {
        "plomería", "plomero" -> Icons.Outlined.Plumbing
        "electricidad", "electricista" -> Icons.Outlined.Bolt
        "gas", "gasista" -> Icons.Outlined.LocalFireDepartment
        "albañilería", "albañil" -> Icons.Outlined.Construction
        "carpintería", "carpintero" -> Icons.Outlined.Carpenter
        "pintura", "pintor" -> Icons.Outlined.FormatPaint
        "climatización" -> Icons.Outlined.AcUnit
        "handyman" -> Icons.Outlined.Handyman
        else -> Icons.Outlined.Category
    }