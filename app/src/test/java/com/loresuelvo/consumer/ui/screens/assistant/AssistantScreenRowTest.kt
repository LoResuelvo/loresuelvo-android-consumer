package com.loresuelvo.consumer.ui.screens.assistant

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import com.loresuelvo.consumer.domain.assistant.AiConversationSummary
import com.loresuelvo.consumer.ui.theme.LoresuelvoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Compose test that pins the "12-DIA Asistente IA" row layout.
 * Each session is rendered as a chat-style row:
 *
 *  ```
 *  Título del problema                  Fecha
 *  Último mensaje del chat
 *  ```
 *
 * Three contracts pinned:
 *  1. exactly one row per conversation (per-id testTag);
 *  2. each row carries the title (left) + the formatted date
 *     (right) — the chat-style "when was this last active";
 *  3. the optional last-message preview (bottom, full width)
 *     renders when the conversation has one, and is absent
 *     when the conversation has none (defensive: backend may
 *     omit `last_message` for fresh sessions).
 *
 * Lives on the JVM via Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "es-rAR", sdk = [34])
class AssistantScreenRowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun conversations(): List<AiConversationSummary> = listOf(
        AiConversationSummary(
            id = "1",
            title = "Pérdida de agua en la cocina",
            lastMessageAtEpochMillis = 1_716_080_400_000L,
            lastMessagePreview = "Revisá si el agua sale desde la rosca del sifón.",
        ),
        AiConversationSummary(
            id = "2",
            title = "Pérdida de agua en el baño",
            lastMessageAtEpochMillis = 1_716_080_700_000L,
            lastMessagePreview = null,
        ),
    )

    @Test
    fun every_session_renders_one_row_with_title_date_and_preview() {
        composeTestRule.setContent {
            LoresuelvoTheme {
                Box {
                    AssistantScreen(
                        state = AssistantUiState.Ready(conversations = conversations()),
                        onRetryClick = {},
                        onConversationClick = {},
                    )
                }
            }
        }
        composeTestRule.onNodeWithTag(ASSISTANT_SCREEN_TAG).assertExists()
        composeTestRule.onNodeWithTag(ASSISTANT_LIST_TAG).assertExists()

        // One row per conversation (per-id testTag).
        composeTestRule
            .onAllNodesWithTag("$ASSISTANT_ROW_TAG-1", useUnmergedTree = true)
            .assertCountEquals(1)
        composeTestRule
            .onAllNodesWithTag("$ASSISTANT_ROW_TAG-2", useUnmergedTree = true)
            .assertCountEquals(1)

        // Title + date for both rows (the date only renders when
        // lastMessageAtEpochMillis > 0 — both fixtures satisfy that).
        composeTestRule
            .onNodeWithTag("$ASSISTANT_ROW_TITLE_TAG-1", useUnmergedTree = true)
            .assertExists()
        composeTestRule
            .onNodeWithTag("$ASSISTANT_ROW_DATE_TAG-1", useUnmergedTree = true)
            .assertExists()
        composeTestRule
            .onNodeWithTag("$ASSISTANT_ROW_TITLE_TAG-2", useUnmergedTree = true)
            .assertExists()
        composeTestRule
            .onNodeWithTag("$ASSISTANT_ROW_DATE_TAG-2", useUnmergedTree = true)
            .assertExists()

        // Preview is present for row #1 (has a last_message) and
        // absent for row #2 (no last_message). The conditional
        // rendering is the contract being pinned.
        composeTestRule
            .onNodeWithTag("$ASSISTANT_ROW_PREVIEW_TAG-1", useUnmergedTree = true)
            .assertExists()
        composeTestRule
            .onAllNodesWithTag("$ASSISTANT_ROW_PREVIEW_TAG-2", useUnmergedTree = true)
            .assertCountEquals(0)
    }
}
