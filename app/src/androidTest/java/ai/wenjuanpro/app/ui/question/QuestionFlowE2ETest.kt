package ai.wenjuanpro.app.ui.question

import ai.wenjuanpro.app.domain.model.OptionContent
import ai.wenjuanpro.app.domain.model.StemContent
import ai.wenjuanpro.app.feature.question.QuestionEffect
import ai.wenjuanpro.app.feature.question.QuestionIntent
import ai.wenjuanpro.app.feature.question.QuestionUiState
import ai.wenjuanpro.app.ui.components.OptionCardTags
import ai.wenjuanpro.app.ui.screens.question.QuestionContent
import ai.wenjuanpro.app.ui.screens.question.SingleChoiceAllInOneTags
import ai.wenjuanpro.app.ui.theme.WenJuanProTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Story 2.3 E2E navigation test — driven by a stub state holder + intent recorder so we don't need
 * Hilt instrumentation. Covers the AC1 happy path: render → click option → click submit →
 * onNavigateComplete fires (single-question config) → NavController lands on `complete` route.
 */
class QuestionFlowE2ETest {
    @get:Rule
    val composeRule = createComposeRule()

    private val baseState =
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
            selectedIndex = null,
            submitEnabled = false,
            countdownProgress = 1f,
            isWarning = false,
        )

    @Test
    fun click_option_then_submit_triggers_NavigateComplete_and_routes_to_complete() {
        val intents = mutableListOf<QuestionIntent>()
        val effects = Channel<QuestionEffect>(Channel.UNLIMITED)
        lateinit var navController: TestNavHostController
        composeRule.setContent {
            WenJuanProTheme {
                navController =
                    TestNavHostController(ApplicationProvider.getApplicationContext()).apply {
                        navigatorProvider.addNavigator(ComposeNavigator())
                    }
                var state by remember { mutableStateOf<QuestionUiState>(baseState) }
                LaunchedEffect(Unit) {
                    effects.receiveAsFlow().collect { effect ->
                        when (effect) {
                            QuestionEffect.NavigateComplete -> navController.navigate("complete")
                            else -> Unit
                        }
                    }
                }
                NavHost(navController = navController, startDestination = "question") {
                    composable("question") {
                        QuestionContent(
                            state = state,
                            onIntent = { intent ->
                                intents += intent
                                when (intent) {
                                    is QuestionIntent.SelectOption ->
                                        state = baseState.copy(
                                            selectedIndex = intent.index,
                                            submitEnabled = true,
                                        )
                                    QuestionIntent.Submit ->
                                        effects.trySend(QuestionEffect.NavigateComplete)
                                    else -> Unit
                                }
                            },
                        )
                    }
                    composable("complete") { Text("complete-placeholder") }
                }
            }
        }
        composeRule.onNodeWithTag(OptionCardTags.PREFIX + 2).performClick()
        composeRule.onNodeWithTag(SingleChoiceAllInOneTags.SUBMIT_BUTTON).performClick()
        composeRule.waitForIdle()
        assertTrue(intents.contains(QuestionIntent.SelectOption(2)))
        assertTrue(intents.contains(QuestionIntent.Submit))
        assertEquals("complete", navController.currentDestination?.route)
    }
}
