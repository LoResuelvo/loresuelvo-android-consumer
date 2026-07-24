package com.loresuelvo.consumer.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R

/**
 * Bottom-of-screen prompt composer for the chat screen.
 *
 * Auto-grow behaviour (scenarios 07-DIA / 08-DIA):
 *  - The [BasicTextField] has `maxLines = 6` so it grows up to
 *    six visible lines as the user types; once the user types a
 *    seventh line the field stays at the cap and the overflowing
 *    content scrolls vertically inside the field.
 *  - The vertical scroll uses the standard
 *    [rememberScrollState] so cursor position is preserved across
 *    recompositions.
 *  - This matches the Gherkin: "permite visualizar hasta 6
 *    líneas de contenido sin scroll" + "el campo de texto
 *    mantiene una altura máxima de 6 líneas" + "puedo desplazarme
 *    mediante scroll dentro del campo".
 *
 * Send-button states (ticket 2 of the chat-UX backlog):
 *  - When `canSend = true` (non-empty prompt AND not mid-flight),
 *    the [Icons.AutoMirrored.Filled.Send] icon renders in the
 *    primary colour and the button is enabled.
 *  - When `canSend = false` (empty prompt OR round-trip in flight),
 *    the same icon stays in the tree but drops to `primary` with
 *    `alpha = 0.38f` so the disabled state is unmistakable
 *    without resorting to a second spinner (the in-flight
 *    indicator already lives in the chat's typing bubble).
 *
 * Stateless: every input/output goes through the callbacks; the
 * parent owns state via [ChatViewModel].
 */
@Composable
fun ChatInputBar(
    promptInput: String,
    canSend: Boolean,
    sending: Boolean,
    onPromptChange: (String) -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BasicTextField(
            value = promptInput,
            onValueChange = onPromptChange,
            modifier = Modifier
                .weight(1f)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(24.dp),
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
                // 08-DIA: vertical scroll inside the capped field so
                // lines 7+ stay reachable without growing the
                // surface past the Gherkin cap.
                .verticalScroll(rememberScrollState())
                .testTag(CHAT_INPUT_FIELD_TAG),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            // 07-DIA: field grows up to 6 lines.
            // 08-DIA: lines beyond 6 stay reachable via vertical scroll.
            maxLines = CHAT_INPUT_MAX_LINES,
            singleLine = false,
            decorationBox = { inner ->
                if (promptInput.isEmpty()) {
                    Text(
                        text = stringResource(R.string.chat_input_placeholder),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Start,
                    )
                }
                inner()
            },
        )

        // Circular send button (WhatsApp-style). The background
        // hue is `primary` so the action is conspicuous; alpha drops
        // to 0.38 when not actionable so the user reads at a glance
        // that the round-trip is in flight or the prompt is empty.
        Surface(
            onClick = onSendClick,
            enabled = canSend,
            modifier = Modifier
                .size(48.dp)
                .testTag(SEND_BUTTON_TAG),
            shape = CircleShape,
            color = if (canSend) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
            },
            contentColor = if (canSend) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.38f)
            },
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(
                        R.string.chat_send_content_description,
                    ),
                    modifier = Modifier.testTag(SEND_ICON_TAG),
                )
            }
        }
    }
}

/**
 * Compose testTag for the prompt [BasicTextField]. Exposed
 * publicly so the Compose-test in `src/test/.../ChatInputBarTest.kt`
 * can measure the field's height across single- and multi-line
 * contents without depending on text-content assertions.
 */
const val CHAT_INPUT_FIELD_TAG: String = "chat_input-field"

/**
 * Compose testTag for the trailing Send button.
 */
const val SEND_BUTTON_TAG: String = "chat-send-button"

/**
 * Compose testTag for the Send icon. The icon is rendered in both
 * idle and disabled states — only the alpha changes — so the
 * testTag is always present when the bar is mounted.
 */
const val SEND_ICON_TAG: String = "chat-send-icon"

/**
 * Compose testTag for the [androidx.compose.material3.HorizontalDivider]
 * that separates the chat surface from the composer (WhatsApp-style
 * border between the list and the input). Lives next to the input
 * so the divider travels with the composer across IME insets.
 */
const val CHAT_INPUT_DIVIDER_TAG: String = "chat-input-divider"

/**
 * Maximum visible lines for the prompt field (scenarios 07 / 08-DIA).
 * Kept as a top-level constant so the test can reference the same
 * value the implementation uses.
 */
const val CHAT_INPUT_MAX_LINES: Int = 6
