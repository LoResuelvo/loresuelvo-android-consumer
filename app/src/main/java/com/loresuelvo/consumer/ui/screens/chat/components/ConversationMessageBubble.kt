package com.loresuelvo.consumer.ui.screens.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.domain.conversation.ConversationMessage
import com.loresuelvo.consumer.domain.conversation.ConversationSender

/**
 * WhatsApp-style bubble for a single message in a consumer ↔
 * provider conversation. The consumer's own messages render on
 * the right with the brand-primary background; the provider's
 * messages render on the left with a neutral surface. The
 * asymmetry is the canonical "this is me / this is them"
 * affordance and matches the Gherkin's user expectation.
 *
 * The component is intentionally narrow (max 80% of the row
 * width) so long messages wrap rather than span the entire
 * screen. The `RoundedCornerShape(20.dp)` is uniform; the
 * corner nearest the edge of the row could be made sharper
 * for a tighter WhatsApp look — that's a follow-up polish, out
 * of scope for 05-IC.
 */
@Composable
fun ConversationMessageBubble(
    message: ConversationMessage,
    modifier: Modifier = Modifier,
) {
    val isConsumer = message.sender is ConversationSender.Consumer
    val bubbleColor = if (isConsumer) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }
    val textColor = if (isConsumer) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = if (isConsumer) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(bubbleColor)
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .testTag(CONVERSATION_MESSAGE_BUBBLE_TAG),
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                overflow = TextOverflow.Visible,
            )
        }
    }
    Spacer(Modifier.width(0.dp))
}

/**
 * Compose testTag for the conversation message bubble. Applied
 * to every bubble regardless of sender so the test can count
 * bubbles via `onAllNodesWithTag`.
 */
const val CONVERSATION_MESSAGE_BUBBLE_TAG: String = "conversation-message-bubble"