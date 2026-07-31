package com.loresuelvo.consumer.ui.screens.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.ui.theme.SubtitleGray

/**
 * Placeholder surface for the 1:1 chat between the consumer and
 * a provider. Created by `POST /job-requests`; the conversation
 * id is the backend-issued id that the contact-form flow
 * navigates with (see scenario 02-SRP).
 *
 * The actual chat UI — message list, composer, real-time updates,
 * the messages list screen (scenarios 03-SRP / 04-SRP) — is
 * out of scope for the current US. This composable renders a
 * placeholder so the navigation event from the contact form has
 * a destination and the BDD assertion "I am redirected to the
 * messages screen with X" can be verified in the
 * `ProfessionalsAcceptanceTest` (Hilt instrumented) at the
 * integration level.
 */
@Composable
fun ConversationScreen(
    conversationId: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Conversation #$conversationId",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Chat surface pending — out of scope for this US.",
                style = MaterialTheme.typography.bodyMedium,
                color = SubtitleGray,
                textAlign = TextAlign.Center,
            )
        }
    }
}
