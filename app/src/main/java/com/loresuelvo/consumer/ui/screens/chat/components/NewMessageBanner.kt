package com.loresuelvo.consumer.ui.screens.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R

/**
 * "↓ nuevo mensaje" banner pinned to the bottom of the chat
 * surface when a provider message arrives while the user is
 * scrolled up reading older messages (scenario 10-IC). Tapping
 * the banner scrolls the list back to the bottom (where the new
 * bubble has landed) and clears the unread flag on the VM.
 *
 * The banner is purely a presentation surface — it reads
 * `hasUnreadIncoming` from the parent and emits a single
 * `onTap` callback. The scroll-to-bottom action lives in the
 * parent (`ReadyState` owns the `LazyListState`) because the
 * banner itself doesn't have access to it.
 */
@Composable
fun NewMessageBanner(
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onTap)
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag(NEW_MESSAGE_BANNER_TAG),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .padding(end = 6.dp)
                    .testTag(NEW_MESSAGE_BANNER_ICON_TAG),
            )
            Text(
                text = stringResource(R.string.conversation_new_message_banner),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/**
 * Compose testTags for the new-message banner. Pinning the
 * locators (rather than literal Spanish labels) keeps the
 * Compose tests locale-independent.
 */
const val NEW_MESSAGE_BANNER_TAG: String = "conversation-new-message-banner"
const val NEW_MESSAGE_BANNER_ICON_TAG: String = "conversation-new-message-banner-icon"