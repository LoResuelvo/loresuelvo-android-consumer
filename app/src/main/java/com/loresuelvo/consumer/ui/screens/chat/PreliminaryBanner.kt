package com.loresuelvo.consumer.ui.screens.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R

/**
 * Persistent banner above the chat list that reminds the user
 * that the AI's responses are preliminary guidance, not a
 * definitive technical diagnosis (scenario 05-DIA). Mirrors the
 * webapp's `InfoBanner` placement (above the messages area,
 * below the top bar).
 *
 * Stateless: the caller ([com.loresuelvo.consumer.ui.screens.chat.ChatScreen])
 * decides when to render via the
 * [com.loresuelvo.consumer.ui.screens.chat.ChatUiState.preliminaryWarningVisible]
 * flag.
 *
 * `secondaryContainer` is used so the banner reads as a calm
 * informational panel rather than an error or warning state; the
 * copy itself does the heavy lifting.
 */
@Composable
fun PreliminaryBanner(
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            text = stringResource(R.string.chat_preliminary_warning),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}
