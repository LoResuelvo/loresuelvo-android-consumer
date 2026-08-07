package com.loresuelvo.consumer.ui.screens.messages.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.conversation.Conversation
import com.loresuelvo.consumer.domain.conversation.ConversationStatus
import com.loresuelvo.consumer.ui.screens.professional.ProviderAvatar
import com.loresuelvo.consumer.ui.theme.SubtitleGray

/**
 * WhatsApp-style row for the consumer's conversations list. Each
 * row pairs:
 *
 *  - a circular avatar (re-used from the providers list — same
 *    initial-letter fallback + Coil photo load);
 *  - the counterpart's full name + the most recent message preview
 *    (or a placeholder when [Conversation.lastMessage] is `null`,
 *    e.g. a brand-new conversation);
 *  - a "Pendiente" badge when the conversation status is
 *    [ConversationStatus.Pending];
 *  - a relative timestamp (today → `HH:mm`; older →
 *    `getRelativeTimeSpanString`) on the trailing edge.
 *
 * Tapping the whole row fires [onClick]. Wired to the host's
 * `navController.navigate(Route.Conversation.buildPath(id))` so
 * the user lands on the chat detail screen (commit 15b wired
 * the navigation; commit 17 — this one — wires the actual
 * `clickable` modifier that was missing).
 */
@Composable
fun ConversationRow(
    conversation: Conversation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val counterpartName = "${conversation.counterpart.name} ${conversation.counterpart.surname}"
    val lastMessageText = conversation.lastMessage?.content
        ?: stringResource(R.string.messages_screen_no_preview)
    val isPending = conversation.status is ConversationStatus.Pending

    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(CONVERSATION_ROW_TAG),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProviderAvatar(
            name = conversation.counterpart.name,
            profilePhotoUrl = conversation.counterpart.profilePhotoUrl,
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = counterpartName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (isPending) {
                    Spacer(Modifier.width(8.dp))
                    PendingBadge()
                }
            }
            Spacer(Modifier.size(2.dp))
            Text(
                text = lastMessageText,
                style = MaterialTheme.typography.bodyMedium,
                color = SubtitleGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = formatConversationTimestamp(conversation.updatedOnEpochMillis),
            style = MaterialTheme.typography.labelSmall,
            color = SubtitleGray,
        )
    }
}

/**
 * Small pill rendered next to the counterpart name when the
 * conversation is awaiting the provider's acceptance.
 */
@Composable
private fun PendingBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .testTag(CONVERSATION_ROW_PENDING_TAG),
    ) {
        Text(
            text = stringResource(R.string.messages_screen_pending_badge),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * Compose testTags for the conversation row. Pinning the locators
 * (rather than literal Spanish labels) keeps the Compose tests
 * locale-independent.
 */
const val CONVERSATION_ROW_TAG: String = "conversation-row"
const val CONVERSATION_ROW_PENDING_TAG: String = "conversation-row-pending"

/**
 * Locale-aware timestamp formatter using `android.text.format.DateUtils`,
 * which produces WhatsApp-style output ("5 min ago", "Yesterday",
 * "Monday", "5/30/26"). `epochMillis == 0L` (parser fallback) renders
 * as an empty string so the row never crashes on a backend regression.
 */
private fun formatConversationTimestamp(epochMillis: Long): String {
    if (epochMillis <= 0L) return ""
    return android.text.format.DateUtils.getRelativeTimeSpanString(
        epochMillis,
        System.currentTimeMillis(),
        android.text.format.DateUtils.MINUTE_IN_MILLIS,
    ).toString()
}