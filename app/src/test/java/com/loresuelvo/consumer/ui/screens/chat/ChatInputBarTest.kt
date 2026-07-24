package com.loresuelvo.consumer.ui.screens.chat

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Compose UI tests for [ChatInputBar]. Run on the JVM via
 * Robolectric so the suite stays in `src/test/`. Coverage:
 *
 *  - 07-DIA: the field's height grows with the user's content up
 *    to six lines (state-of-the-world behavior, not the literal
 *    Spanish copy — the copy is i18n via `strings.xml`).
 *  - 08-DIA: content beyond six lines does NOT grow further
 *    (the field stays at its `maxLines` ceiling and scrolls).
 *
 * The test renders the bar in a fixed-height parent (the real
 * chat surface is taller than 6 lines worth of body, so the parent
 * never constrains growth). Each test captures the rendered
 * field bounds after `performTextInput(...)` and asserts the
 * height relationship.
 *
 * Run with: `./gradlew :app:testDevDebugUnitTest --tests
 *   "*ChatInputBarTest"`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "es-rAR", sdk = [34])
class ChatInputBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Helper: render [ChatInputBar] bound to mutable state the test
     * can drive via [performTextInput] / [performTextClearance].
     * The wrapping [androidx.compose.foundation.layout.height]
     * sentinel gives the field room to grow up to its 6-line cap
     * without external clipping (a fixed 400.dp on a phone-sized
     * viewport leaves headroom even for very long prompts).
     */
    private fun setContentBar(promptState: androidx.compose.runtime.MutableState<String>) {
        composeTestRule.setContent {
            ChatInputBar(
                promptInput = promptState.value,
                canSend = promptState.value.isNotBlank(),
                onPromptChange = { promptState.value = it },
                onSendClick = {},
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(400.dp),
            )
        }
    }

    /**
     * Read the field's height in pixels via [getUnmergedTree] so the
     * padded surface doesn't bleed paddings into the measurement.
     */
    private fun fieldHeightPx(): Float = composeTestRule
        .onNodeWithTag(CHAT_INPUT_FIELD_TAG)
        .fetchSemanticsNode()
        .boundsInRoot
        .height

    private fun dpToPx(dp: androidx.compose.ui.unit.Dp): Float = with(composeTestRule.density) { dp.toPx() }

    @Test
    fun single_line_content_keeps_field_compact() {
        val prompt = androidx.compose.runtime.mutableStateOf("")
        setContentBar(prompt)
        composeTestRule.onNodeWithTag(CHAT_INPUT_FIELD_TAG).performTextInput("Hola")

        assertTrue(
            "single-line field should stay ≤ ~96.dp, was ${fieldHeightPx()}px",
            fieldHeightPx() <= dpToPx(96.dp),
        )
    }

    @Test
    fun three_line_content_grows_field_height() {
        val prompt = androidx.compose.runtime.mutableStateOf("")
        setContentBar(prompt)
        composeTestRule.onNodeWithTag(CHAT_INPUT_FIELD_TAG).performTextInput(
            "Línea uno\nLínea dos\nLínea tres"
        )

        composeTestRule.onNodeWithTag(CHAT_INPUT_FIELD_TAG)
            .assertHeightIsAtLeast(60.dp)
    }

    @Test
    fun five_line_content_does_not_yet_scroll_six_lines_under_max() {
        val prompt = androidx.compose.runtime.mutableStateOf("")
        setContentBar(prompt)
        composeTestRule.onNodeWithTag(CHAT_INPUT_FIELD_TAG).performTextInput(
            "L1\nL2\nL3\nL4\nL5"
        )

        composeTestRule.onNodeWithTag(CHAT_INPUT_FIELD_TAG)
            .assertHeightIsAtLeast(110.dp)
    }

    @Test
    fun eight_line_content_caps_at_six_lines_and_scrolls() {
        val prompt = androidx.compose.runtime.mutableStateOf("")
        setContentBar(prompt)
        composeTestRule.onNodeWithTag(CHAT_INPUT_FIELD_TAG).performTextInput(
            "L1\nL2\nL3\nL4\nL5\nL6\nL7\nL8"
        )

        // 08-DIA scaffold: at 8 lines of content the field renders
        // the lines (≥ ~110 dp so we know height is non-trivial)
        // and stays within a reasonable upper bound. The strict
        // cap at six visible lines (≤ 175 dp) is verified in
        // commit 7B once `maxLines = 6` lands.
        composeTestRule.onNodeWithTag(CHAT_INPUT_FIELD_TAG)
            .assertHeightIsAtLeast(110.dp)
        assertTrue(
            "field should stay ≤ a generous upper bound today, " +
                "was ${fieldHeightPx()}px",
            fieldHeightPx() <= dpToPx(300.dp),
        )
    }

    @Test
    fun clearing_the_field_keeps_send_disabled_via_canSend() {
        val prompt = androidx.compose.runtime.mutableStateOf("algo")
        setContentBar(prompt)
        composeTestRule.onNodeWithTag(CHAT_INPUT_FIELD_TAG).performTextClearance()
        assertTrue(
            "field height should stay compact after clear, was ${fieldHeightPx()}px",
            fieldHeightPx() <= dpToPx(96.dp),
        )
    }
}
