package ai.wenjuanpro.app.ui.question

import ai.wenjuanpro.app.ui.components.CountdownBar
import ai.wenjuanpro.app.ui.components.CountdownBarTags
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test

class CountdownBarTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun normal_state_renders_with_normal_semantic_color() {
        composeRule.setContent {
            CountdownBar(progress = 0.5f, isWarning = false)
        }
        composeRule
            .onNodeWithTag(CountdownBarTags.ROOT)
            .assertContentDescriptionContains(CountdownBarTags.SEMANTIC_NORMAL)
    }

    @Test
    fun warning_state_renders_with_warning_semantic_color() {
        composeRule.setContent {
            CountdownBar(progress = 0.1f, isWarning = true)
        }
        composeRule
            .onNodeWithTag(CountdownBarTags.ROOT)
            .assertContentDescriptionContains(CountdownBarTags.SEMANTIC_WARNING)
    }

    @Test
    fun paused_state_renders_with_paused_semantic_color_and_overlay() {
        composeRule.setContent {
            CountdownBar(
                progress = 1f,
                isWarning = false,
                isPaused = true,
                overlayText = "记忆中...",
            )
        }
        composeRule
            .onNodeWithTag(CountdownBarTags.ROOT)
            .assertContentDescriptionContains(CountdownBarTags.SEMANTIC_PAUSED)
        composeRule.onNodeWithTag(CountdownBarTags.OVERLAY_TEXT).assertExists()
    }
}
