package ai.wenjuanpro.app.ui.question

import ai.wenjuanpro.app.domain.model.OptionContent
import ai.wenjuanpro.app.domain.model.StemContent
import ai.wenjuanpro.app.feature.question.QuestionIntent
import ai.wenjuanpro.app.feature.question.QuestionUiState
import ai.wenjuanpro.app.ui.components.OptionCardTags
import ai.wenjuanpro.app.ui.screens.question.MultiChoiceAllInOneContent
import ai.wenjuanpro.app.ui.screens.question.MultiChoiceAllInOneTags
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MultiChoiceAllInOneContentTest {
    @get:Rule val rule = createComposeRule()

    private val options =
        listOf(
            OptionContent.Text("A"),
            OptionContent.Text("B"),
            OptionContent.Text("C"),
            OptionContent.Text("D"),
        )

    private fun state(
        selectedIndices: Set<Int> = emptySet(),
    ) = QuestionUiState.MultiChoiceAllInOne(
        qid = "Q1",
        questionIndex = 1,
        totalQuestions = 3,
        stem = StemContent.Text("Pick all that apply"),
        options = options,
        selectedIndices = selectedIndices,
        submitEnabled = selectedIndices.isNotEmpty(),
    )

    @Test
    fun rendersAllOptions() {
        rule.setContent {
            MultiChoiceAllInOneContent(state = state(), onIntent = {})
        }
        rule.onNodeWithTag(MultiChoiceAllInOneTags.ROOT).assertIsDisplayed()
        (1..4).forEach { i ->
            rule.onNodeWithTag(OptionCardTags.PREFIX + i).assertIsDisplayed()
        }
    }

    @Test
    fun toggleOptionCallsIntent() {
        val intents = mutableListOf<QuestionIntent>()
        rule.setContent {
            MultiChoiceAllInOneContent(state = state(), onIntent = { intents.add(it) })
        }
        rule.onNodeWithTag(OptionCardTags.PREFIX + 1).performClick()
        assertTrue(intents.any { it is QuestionIntent.ToggleOption && it.index == 1 })
    }

    @Test
    fun submitButtonDisabledWhenNoSelection() {
        rule.setContent {
            MultiChoiceAllInOneContent(state = state(emptySet()), onIntent = {})
        }
        rule.onNodeWithTag(MultiChoiceAllInOneTags.SUBMIT_BUTTON).assertIsNotEnabled()
    }

    @Test
    fun submitButtonEnabledWhenSelected() {
        rule.setContent {
            MultiChoiceAllInOneContent(state = state(setOf(1, 3)), onIntent = {})
        }
        rule.onNodeWithTag(MultiChoiceAllInOneTags.SUBMIT_BUTTON).assertIsEnabled()
    }

    @Test
    fun checkCircleShownForSelectedMultiOption() {
        rule.setContent {
            MultiChoiceAllInOneContent(state = state(setOf(1, 3)), onIntent = {})
        }
        rule.onNodeWithTag(OptionCardTags.CHECK_CIRCLE_PREFIX + 1).assertIsDisplayed()
        rule.onNodeWithTag(OptionCardTags.CHECK_CIRCLE_PREFIX + 3).assertIsDisplayed()
    }
}
