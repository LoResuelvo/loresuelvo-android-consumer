package com.loresuelvo.consumer.ui.screens.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.ui.screens.professional.ProviderAvatar
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.conversation.ConversationCounterpart
import com.loresuelvo.consumer.domain.conversation.ConversationStatus

/**
 * Top bar for the consumer ↔ provider conversation detail
 * screen. WhatsApp-style: back button on the left, counterpart
 * (provider) name + category on the centre, and a "Pendiente"
 * badge on the right when the conversation is still awaiting
 * the provider's acceptance.
 *
 * Stateless — the parent owns the navigation callback.
 */
@Composable
fun ConversationTopBar(
    counterpart: ConversationCounterpart,
    status: ConversationStatus,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .testTag(CONVERSATION_TOP_BAR_TAG)
            // 08-UXUI: `ConversationTopBar` is a custom composable
            // (not a Material 3 `TopAppBar`), so it does not inset
            // the status bar automatically. The outer `Scaffold`
            // already consumes the status bar inset for the
            // `topBar` slot, but because this composable does not
            // apply it, the back arrow and avatar ended up under
            // the status bar. Applying the inset here keeps the
            // back arrow and avatar below the system bar.
            .windowInsetsPadding(WindowInsets.statusBars),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.testTag(CONVERSATION_TOP_BAR_BACK_TAG),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(
                        R.string.conversation_top_bar_back_content_description,
                    ),
                )
            }
            Spacer(Modifier.width(4.dp))

            ProviderAvatar(
                name = counterpart.name,
                profilePhotoUrl = counterpart.profilePhotoUrl,
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${counterpart.name} ${counterpart.surname}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = counterpart.categoryName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (status is ConversationStatus.Pending) {
                Spacer(Modifier.width(8.dp))
                PendingPill()
            }
        }
    }
}

@Composable
private fun PendingPill() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag(CONVERSATION_TOP_BAR_PENDING_TAG),
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
 * Compose testTags for the conversation top bar. Pinning the
 * locators (rather than literal Spanish labels) keeps the
 * Compose tests locale-independent.
 */
const val CONVERSATION_TOP_BAR_TAG: String = "conversation-top-bar"
const val CONVERSATION_TOP_BAR_BACK_TAG: String = "conversation-top-bar-back"
const val CONVERSATION_TOP_BAR_PENDING_TAG: String = "conversation-top-bar-pending"