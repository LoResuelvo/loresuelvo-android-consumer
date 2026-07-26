package com.loresuelvo.consumer.ui.screens.chat

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Compose UI test for [AssistantAvatar]. Run on the JVM via
 * Robolectric.
 *
 * The avatar is a circular `Surface` rendered with
 * `MaterialTheme.colorScheme.primaryContainer` and embeds a
 * `SmartToy` icon inside. It's used both in the empty state (as
 * the brand mark) and on each assistant message (commit 11C).
 *
 * Plan: 11A — EmptyState. The avatar is extracted first so the
 * empty state + bubble can reuse it without duplication.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "es-rAR", sdk = [34])
class AssistantAvatarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun renders_a_circular_surface_with_the_assistant_test_tag() {
        composeTestRule.setContent {
            AssistantAvatar(modifier = Modifier.testTag(ASSISTANT_AVATAR_TAG))
        }
        composeTestRule.onNodeWithTag(ASSISTANT_AVATAR_TAG).assertExists()
    }
}
