package ai.wenjuanpro.app.ui.question

import ai.wenjuanpro.app.domain.model.OptionContent
import ai.wenjuanpro.app.domain.model.StemContent
import ai.wenjuanpro.app.feature.question.QuestionIntent
import ai.wenjuanpro.app.feature.question.QuestionUiState
import ai.wenjuanpro.app.ui.screens.question.StagedMultiChoiceScaffold
import ai.wenjuanpro.app.ui.screens.question.StagedMultiChoiceTags
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StagedMultiChoiceScaffoldTest {
    @get:Rule val rule = createComposeRule()

    private val options =
        listOf(
            OptionContent.Text("A"),
            OptionContent.Text("B"),
            OptionContent.Text("C"),
            OptionContent.Text("D"),
        )

    private fun state(stage: QuestionUiState.Stage) =
        QuestionUiState.MultiChoiceStaged(
            qid = "Q1",
            questionIndex = 1,
            totalQuestions = 3,
            stem = StemContent.Text("Pick all that apply"),
            options = options,
            stage = stage,
            selectedIndices = emptySet(),
            submitEnabled = false,
        )

    @Test
    fun stemPhaseShowsStemHidesOptions() {
        rule.setContent {
            StagedMultiChoiceScaffold(state = state(QuestionUiState.Stage.STEM), onIntent = {})
        }
        rule.onNodeWithTag(StagedMultiChoiceTags.STEM_BLOCK).assertIsDisplayed()
    }

    @Test
    fun stemPhaseTapIgnored() {
        val intents = mutableListOf<QuestionIntent>()
        rule.setContent {
            StagedMultiChoiceScaffold(state = state(QuestionUiState.Stage.STEM), onIntent = { intents.add(it) })
        }
        rule.onNodeWithTag(StagedMultiChoiceTags.STEM_BLOCK).performClick()
        assertTrue("stem tap should be ignored", intents.isEmpty())
    }

    @Test
    fun optionsPhaseShowsOptionsHidesStem() {
        rule.setContent {
            StagedMultiChoiceScaffold(state = state(QuestionUiState.Stage.OPTIONS), onIntent = {})
        }
        rule.onNodeWithTag(StagedMultiChoiceTags.OPTIONS_BLOCK).assertIsDisplayed()
    }
}
