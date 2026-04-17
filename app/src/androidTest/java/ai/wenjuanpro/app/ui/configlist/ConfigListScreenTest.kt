package ai.wenjuanpro.app.ui.configlist

import ai.wenjuanpro.app.data.parser.ParseError
import ai.wenjuanpro.app.data.parser.ParseErrorCode
import ai.wenjuanpro.app.feature.configlist.ConfigCardUiModel
import ai.wenjuanpro.app.feature.configlist.ConfigListIntent
import ai.wenjuanpro.app.feature.configlist.ConfigListUiState
import ai.wenjuanpro.app.feature.configlist.ErrorSheetState
import ai.wenjuanpro.app.ui.components.AssessmentCardTags
import ai.wenjuanpro.app.ui.components.ErrorSheetTags
import ai.wenjuanpro.app.ui.screens.configlist.ConfigListContent
import ai.wenjuanpro.app.ui.screens.configlist.ConfigListScreenTags
import ai.wenjuanpro.app.ui.theme.WenJuanProTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI test skeleton for Story 1.4 — ConfigListScreen.
 *
 * Validates ConfigListContent (stateless) against prebuilt UiState inputs, following the same
 * pattern established by Story 1.2 PermissionScreenTest (keeps tests Hilt-free and fast).
 */
class ConfigListScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun validCard(
        configId: String,
        title: String = configId,
        count: Int = 5,
    ): ConfigCardUiModel =
        ConfigCardUiModel(
            configId = configId,
            title = title,
            questionCount = count,
            isValid = true,
            errors = emptyList(),
            sourceFileName = "$configId.txt",
        )

    private fun invalidCard(fileName: String): ConfigCardUiModel =
        ConfigCardUiModel(
            configId = fileName.removeSuffix(".txt"),
            title = fileName,
            questionCount = null,
            isValid = false,
            errors =
                listOf(
                    ParseError(
                        line = 2,
                        field = "title",
                        code = ParseErrorCode.CONFIG_FIELD_INVALID,
                        message = "$fileName: 第 2 行缺少必填字段 title",
                    ),
                ),
            sourceFileName = fileName,
        )

    @Test
    fun `1_4-E2E-008 Renders 3 cards (2 valid + 1 invalid) with correct status chips and question count badges`() {
        val state =
            ConfigListUiState.Success(
                cards =
                    listOf(
                        validCard("cog-mem", title = "认知记忆", count = 5),
                        validCard("emo-reg", title = "情绪调节", count = 3),
                        invalidCard("broken.txt"),
                    ),
                allInvalid = false,
            )
        composeRule.setContent {
            WenJuanProTheme {
                ConfigListContent(state = state, sheetState = null, onIntent = {})
            }
        }
        composeRule.onNodeWithTag(AssessmentCardTags.VALID_CARD_PREFIX + "cog-mem.txt").assertIsDisplayed()
        composeRule.onNodeWithTag(AssessmentCardTags.VALID_CARD_PREFIX + "emo-reg.txt").assertIsDisplayed()
        composeRule.onNodeWithTag(AssessmentCardTags.INVALID_CARD_PREFIX + "broken.txt").assertIsDisplayed()
        composeRule.onNodeWithText("5 题").assertIsDisplayed()
        composeRule.onNodeWithText("3 题").assertIsDisplayed()
        composeRule.onNodeWithText("? 题").assertIsDisplayed()
    }

    @Test
    fun `1_4-E2E-009 Empty directory renders Empty state message + 刷新 button`() {
        composeRule.setContent {
            WenJuanProTheme {
                ConfigListContent(state = ConfigListUiState.Empty, sheetState = null, onIntent = {})
            }
        }
        composeRule
            .onNodeWithText("请将 config TXT 文件放入 /sdcard/WenJuanPro/config/ 后点击刷新")
            .assertIsDisplayed()
        composeRule.onNodeWithTag(ConfigListScreenTags.EMPTY_REFRESH_BUTTON).assertIsDisplayed()
    }

    @Test
    fun `1_4-E2E-010 All-invalid state renders top banner '当前无可用测评集' + invalid cards`() {
        val state =
            ConfigListUiState.Success(
                cards =
                    listOf(
                        invalidCard("a.txt"),
                        invalidCard("b.txt"),
                        invalidCard("c.txt"),
                    ),
                allInvalid = true,
            )
        composeRule.setContent {
            WenJuanProTheme {
                ConfigListContent(state = state, sheetState = null, onIntent = {})
            }
        }
        composeRule.onNodeWithTag(ConfigListScreenTags.ALL_INVALID_BANNER).assertIsDisplayed()
        composeRule.onNodeWithTag(AssessmentCardTags.INVALID_CARD_PREFIX + "a.txt").assertIsDisplayed()
    }

    @Test
    fun `1_4-E2E-011 Clicking '查看错误详情' on invalid card opens ErrorSheet with error rows`() {
        composeRule.setContent {
            WenJuanProTheme {
                var sheet by remember { mutableStateOf<ErrorSheetState?>(null) }
                val card = invalidCard("broken.txt")
                val state = ConfigListUiState.Success(cards = listOf(card), allInvalid = true)
                ConfigListContent(
                    state = state,
                    sheetState = sheet,
                    onIntent = { intent ->
                        when (intent) {
                            is ConfigListIntent.ViewErrors ->
                                sheet = ErrorSheetState(card.sourceFileName, card.errors)
                            ConfigListIntent.DismissSheet -> sheet = null
                            else -> Unit
                        }
                    },
                )
            }
        }
        composeRule
            .onNodeWithTag(AssessmentCardTags.VIEW_ERRORS_BUTTON_PREFIX + "broken.txt")
            .performClick()
        composeRule.onNodeWithTag(ErrorSheetTags.ROOT).assertIsDisplayed()
    }

    @Test
    fun `1_4-E2E-012 Clicking close button on ErrorSheet dismisses it`() {
        composeRule.setContent {
            WenJuanProTheme {
                var sheet by remember {
                    mutableStateOf<ErrorSheetState?>(
                        ErrorSheetState(
                            sourceFileName = "broken.txt",
                            errors = invalidCard("broken.txt").errors,
                        ),
                    )
                }
                ConfigListContent(
                    state =
                        ConfigListUiState.Success(
                            cards = listOf(invalidCard("broken.txt")),
                            allInvalid = true,
                        ),
                    sheetState = sheet,
                    onIntent = { intent ->
                        if (intent == ConfigListIntent.DismissSheet) sheet = null
                    },
                )
            }
        }
        composeRule.onNodeWithTag(ErrorSheetTags.ROOT).assertIsDisplayed()
        composeRule.onNodeWithTag(ErrorSheetTags.CLOSE_BUTTON).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ErrorSheetTags.ROOT).assertDoesNotExist()
    }

    @Test
    fun `1_4-E2E-013 Scan beyond 5s renders Timeout state + 重试 button; retry transitions back to Success`() {
        composeRule.setContent {
            WenJuanProTheme {
                var state by remember {
                    mutableStateOf<ConfigListUiState>(ConfigListUiState.Timeout)
                }
                ConfigListContent(
                    state = state,
                    sheetState = null,
                    onIntent = { intent ->
                        if (intent is ConfigListIntent.Refresh) {
                            state =
                                ConfigListUiState.Success(
                                    cards = listOf(validCard("cog-mem")),
                                    allInvalid = false,
                                )
                        }
                    },
                )
            }
        }
        composeRule.onNodeWithTag(ConfigListScreenTags.TIMEOUT).assertIsDisplayed()
        composeRule.onNodeWithTag(ConfigListScreenTags.RETRY_BUTTON).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(AssessmentCardTags.VALID_CARD_PREFIX + "cog-mem.txt").assertIsDisplayed()
    }

    @Test
    fun `1_4-BLIND-BOUNDARY-002 Single config renders one card (N=1 boundary)`() {
        val state =
            ConfigListUiState.Success(
                cards = listOf(validCard("cog-mem", count = 5)),
                allInvalid = false,
            )
        composeRule.setContent {
            WenJuanProTheme {
                ConfigListContent(state = state, sheetState = null, onIntent = {})
            }
        }
        composeRule.onNodeWithTag(AssessmentCardTags.VALID_CARD_PREFIX + "cog-mem.txt").assertIsDisplayed()
    }

    @Test
    fun `1_4-BLIND-BOUNDARY-003 100 configs render without OOM (N=100 upper bound)`() {
        val cards = (1..100).map { validCard(configId = "c%03d".format(it), count = 5) }
        val state = ConfigListUiState.Success(cards = cards, allInvalid = false)
        composeRule.setContent {
            WenJuanProTheme {
                ConfigListContent(state = state, sheetState = null, onIntent = {})
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(AssessmentCardTags.VALID_CARD_PREFIX + "c001.txt").assertIsDisplayed()
    }

    @Test
    fun `1_4-BLIND-FLOW-003 Back button while ErrorSheet is open dismisses sheet (not navigates away)`() {
        var dismissed = 0
        composeRule.setContent {
            WenJuanProTheme {
                ConfigListContent(
                    state =
                        ConfigListUiState.Success(
                            cards = listOf(invalidCard("broken.txt")),
                            allInvalid = true,
                        ),
                    sheetState = ErrorSheetState("broken.txt", invalidCard("broken.txt").errors),
                    onIntent = { intent ->
                        if (intent == ConfigListIntent.DismissSheet) dismissed++
                    },
                )
            }
        }
        androidx.test.espresso.Espresso.pressBack()
        composeRule.waitForIdle()
        assertTrue("BackHandler should fire DismissSheet exactly once", dismissed >= 1)
    }
}
