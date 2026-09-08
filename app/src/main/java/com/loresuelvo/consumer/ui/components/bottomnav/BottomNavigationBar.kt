package com.loresuelvo.consumer.ui.components.bottomnav

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun LoResuelvoBottomBar(
    currentRoute: String?,
    onNavigate: (BottomDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!BottomDestination.shouldShow(currentRoute)) return

    val barShape = RoundedCornerShape(30.dp)

    // "Floating" bottom-bar: the [Surface] replaces the previous
    // `Box` + `.shadow(elevation, …, clip = false)` + `.clip(barShape)`
    // + `.background(MaterialTheme.colorScheme.surface)` stack.
    // [Surface] coordinates `shape`, `shadowElevation`, and the
    // painted layer in one place so we can keep `color =
    // Color.Transparent` (true transparency, you can see the
    // screen content behind the bar) **and** the floating shadow
    // stays intact (Compose's `shadow` modifier relies on a
    // painted layer; an un-painted `Box` would drop the shadow
    // entirely — see the KDoc in `shadow()` for the caveat).
    //
    // `tonalElevation = 0.dp` keeps the bar free from the
    // Material 3 surface-tint overlay so dark-mode contrast is
    // predictable instead of inheriting a tonal tint that paints
    // over the transparency.
    Surface(
        shape = barShape,
        color = Color.Transparent,
        shadowElevation = 10.dp,
        tonalElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(LOWRESUELVO_BOTTOM_BAR_TAG),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomDestination.all.forEach { destination ->
                val isSelected = currentRoute == destination.route

                Box(
                    modifier = Modifier
                        .clickable(onClick = { onNavigate(destination) })
                        .padding(8.dp)
                        .testTag(BOTTOM_BAR_ITEM_TAG_PREFIX + destination.route),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = stringResource(destination.labelRes),
                        tint = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(if (isSelected) 30.dp else 27.dp),
                    )
                }
            }
        }
    }
}

/**
 * Compose testTags for the LoResuelvo floating bottom bar.
 *
 * The bar exposes two tag surfaces:
 *  - [LOWRESUELVO_BOTTOM_BAR_TAG] — the outer Surface so callers
 *    can assert "the bar is rendered / not rendered".
 *  - [BOTTOM_BAR_ITEM_TAG_PREFIX]` + route — one per tab; lets a
 *    test target the click target without depending on the
 *    localised label copy (which the bar no longer paints
 *    because it is icon-only).
 */
const val LOWRESUELVO_BOTTOM_BAR_TAG: String = "lo-resuelvo-bottom-bar"
const val BOTTOM_BAR_ITEM_TAG_PREFIX: String = "bottom-bar-item-"