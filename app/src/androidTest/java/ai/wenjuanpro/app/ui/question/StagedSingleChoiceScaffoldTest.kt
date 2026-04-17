package ai.wenjuanpro.app.ui.question

import ai.wenjuanpro.app.domain.model.OptionContent
import ai.wenjuanpro.app.domain.model.StemContent
import ai.wenjuanpro.app.feature.question.QuestionIntent
import ai.wenjuanpro.app.feature.question.QuestionUiState
import ai.wenjuanpro.app.ui.components.OptionCardTags
import ai.wenjuanpro.app.ui.screens.question.StagedSingleChoiceScaffold
import ai.wenjuanpro.app.ui.screens.question.StagedSingleChoiceTags
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class StagedSingleChoiceScaffoldTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun staged(stage: QuestionUiState.Stage): QuestionUiState.SingleChoiceStaged =
        QuestionUiState.SingleChoiceStaged(
            qid = "Q1",
            questionIndex = 1,
            totalQuestions = 1,
            stem = StemContent.Text("题干"),
            options =
                listOf(
                    OptionContent.Text("A"),
                    OptionContent.Text("B"),
                    OptionContent.Text("C"),
                    OptionContent.Text("D"),
                ),
            stage = stage,
            selectedIndex = null,
            submitEnabled = false,
            countdownProgress = 1f,
            isWarning = false,
        )

    @Test
    fun stem_phase_renders_no_options_or_submit() {
        composeRule.setContent {
            StagedSingleChoiceScaffold(state = staged(QuestionUiState.Stage.STEM), onIntent = {})
        }
        composeRule.onNodeWithTag(StagedSingleChoiceTags.STEM_BLOCK).assertExists()
        composeRule.onNodeWithTag(StagedSingleChoiceTags.SUBMIT_BUTTON).assertDoesNotExist()
        composeRule.onNodeWithTag(OptionCardTags.PREFIX + 1).assertDoesNotExist()
    }

    @Test
    fun stem_phase_click_does_not_emit_intent() {
        val intents = mutableListOf<QuestionIntent>()
        composeRule.setContent {
            StagedSingleChoiceScaffold(
                state = staged(QuestionUiState.Stage.STEM),
                onIntent = { intents += it },
            )
        }
        composeRule.onNodeWithTag(StagedSingleChoiceTags.STEM_BLOCK).performClick()
        composeRule.runOnIdle {
            assert(intents.isEmpty()) { "expected no intents in stem stage, got $intents" }
        }
    }

    @Test
    fun options_stage_shows_options_and_submit() {
        composeRule.setContent {
            StagedSingleChoiceScaffold(
                state = staged(QuestionUiState.Stage.OPTIONS),
                onIntent = {},
            )
        }
        composeRule.onNodeWithTag(StagedSingleChoiceTags.OPTIONS_BLOCK).assertExists()
        composeRule.onNodeWithTag(StagedSingleChoiceTags.SUBMIT_BUTTON).assertExists()
        composeRule.onNodeWithTag(OptionCardTags.PREFIX + 1).assertExists()
    }
}
