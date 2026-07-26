package com.loresuelvo.consumer.ui.screens.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Compose testTag for [AssistantAvatar]. Exposed so the Compose
 * tests can locate the avatar regardless of where it's rendered
 * (header of the empty state, per-message avatar in commit 11C, etc.).
 */
const val ASSISTANT_AVATAR_TAG: String = "chat-assistant-avatar"

/**
 * Circular brand mark for the assistant. Reused in two places:
 *  1. The empty state header (commit 11G, default `size` 24dp).
 *  2. Per-message avatar on assistant bubbles (commit 11C, 32dp).
 *
 * The icon's size scales with the surface (`size * 0.625f`) so a
 * smaller avatar doesn't render a 20dp icon inside a 24dp circle.
 */
@Composable
fun AssistantAvatar(
    size: Dp = 24.dp,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier
            .size(size)
            .testTag(ASSISTANT_AVATAR_TAG),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Outlined.SmartToy,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(size * 0.625f),
            )
        }
    }
}
