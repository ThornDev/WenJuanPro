package ai.wenjuanpro.app.ui.question

import ai.wenjuanpro.app.domain.model.OptionContent
import ai.wenjuanpro.app.domain.model.StemContent
import ai.wenjuanpro.app.feature.question.QuestionIntent
import ai.wenjuanpro.app.feature.question.QuestionUiState
import ai.wenjuanpro.app.ui.components.OptionCardTags
import ai.wenjuanpro.app.ui.screens.question.SingleChoiceAllInOneContent
import ai.wenjuanpro.app.ui.screens.question.SingleChoiceAllInOneTags
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class SingleChoiceAllInOneContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun state(selectedIndex: Int? = null) =
        QuestionUiState.SingleChoiceAllInOne(
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
            selectedIndex = selectedIndex,
            submitEnabled = selectedIndex != null,
            countdownProgress = 1f,
            isWarning = false,
        )

    @Test
    fun renders_4_options_and_disables_submit_until_selected() {
        val intents = mutableListOf<QuestionIntent>()
        composeRule.setContent {
            SingleChoiceAllInOneContent(state = state(), onIntent = { intents += it })
        }
        composeRule.onNodeWithTag(SingleChoiceAllInOneTags.ROOT).assertExists()
        composeRule.onNodeWithTag(OptionCardTags.PREFIX + 1).assertExists()
        composeRule.onNodeWithTag(OptionCardTags.PREFIX + 2).assertExists()
        composeRule.onNodeWithTag(OptionCardTags.PREFIX + 3).assertExists()
        composeRule.onNodeWithTag(OptionCardTags.PREFIX + 4).assertExists()
        composeRule.onNodeWithTag(SingleChoiceAllInOneTags.SUBMIT_BUTTON).assertIsNotEnabled()
    }

    @Test
    fun click_option_2_emits_SelectOption_intent() {
        val intents = mutableListOf<QuestionIntent>()
        composeRule.setContent {
            SingleChoiceAllInOneContent(state = state(), onIntent = { intents += it })
        }
        composeRule.onNodeWithTag(OptionCardTags.PREFIX + 2).performClick()
        composeRule.runOnIdle {
            assert(intents.contains(QuestionIntent.SelectOption(2))) {
                "expected SelectOption(2) emitted, got $intents"
            }
        }
    }

    @Test
    fun submit_button_enabled_after_selection() {
        composeRule.setContent {
            SingleChoiceAllInOneContent(state = state(selectedIndex = 2), onIntent = {})
        }
        composeRule.onNodeWithTag(SingleChoiceAllInOneTags.SUBMIT_BUTTON).assertIsEnabled()
    }

    @Test
    fun click_submit_emits_Submit_intent() {
        val intents = mutableListOf<QuestionIntent>()
        composeRule.setContent {
            SingleChoiceAllInOneContent(
                state = state(selectedIndex = 2),
                onIntent = { intents += it },
            )
        }
        composeRule.onNodeWithTag(SingleChoiceAllInOneTags.SUBMIT_BUTTON).performClick()
        composeRule.runOnIdle {
            assert(intents.contains(QuestionIntent.Submit)) {
                "expected Submit emitted, got $intents"
            }
        }
    }
}
