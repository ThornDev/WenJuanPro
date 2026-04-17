package ai.wenjuanpro.app.ui.configlist

import ai.wenjuanpro.app.feature.configlist.ConfigCardUiModel
import ai.wenjuanpro.app.feature.configlist.ConfigListEffect
import ai.wenjuanpro.app.feature.configlist.ConfigListIntent
import ai.wenjuanpro.app.feature.configlist.ConfigListUiState
import ai.wenjuanpro.app.ui.components.AssessmentCardTags
import ai.wenjuanpro.app.ui.screens.configlist.ConfigListContent
import ai.wenjuanpro.app.ui.theme.WenJuanProTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavType
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI navigation test for Story 1.4 — ConfigList → Scan placeholder route.
 *
 * Uses a minimal NavHost mirroring WenJuanProNavHost to avoid Hilt in instrumented tests.
 */
class ConfigListNavigationTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun validCard(): ConfigCardUiModel =
        ConfigCardUiModel(
            configId = "cog-mem",
            title = "认知记忆",
            questionCount = 5,
            isValid = true,
            errors = emptyList(),
            sourceFileName = "cog-mem.txt",
        )

    @Test
    fun `1_4-E2E-014 Clicking a valid config card navigates to scan route with correct configId arg`() {
        val effectChannel = Channel<ConfigListEffect>(Channel.UNLIMITED)
        lateinit var navController: TestNavHostController
        composeRule.setContent {
            WenJuanProTheme {
                navController =
                    TestNavHostController(ApplicationProvider.getApplicationContext())
                        .apply { navigatorProvider.addNavigator(ComposeNavigator()) }
                val state by remember {
                    mutableStateOf(
                        ConfigListUiState.Success(
                            cards = listOf(validCard()),
                            allInvalid = false,
                        ),
                    )
                }
                LaunchedEffect(Unit) {
                    effectChannel.receiveAsFlow().collect { effect ->
                        if (effect is ConfigListEffect.NavigateToScan) {
                            navController.navigate("scan?configId=${effect.configId}")
                        }
                    }
                }
                NavHost(
                    navController = navController,
                    startDestination = "configlist",
                ) {
                    composable("configlist") {
                        ConfigListContent(
                            state = state,
                            sheetState = null,
                            onIntent = { intent ->
                                if (intent is ConfigListIntent.CardClicked) {
                                    effectChannel.trySend(
                                        ConfigListEffect.NavigateToScan(intent.configId),
                                    )
                                }
                            },
                        )
                    }
                    composable(
                        "scan?configId={configId}",
                        arguments =
                            listOf(
                                navArgument("configId") {
                                    type = NavType.StringType
                                    nullable = false
                                },
                            ),
                    ) { Text("scan placeholder") }
                }
            }
        }
        composeRule.onNodeWithTag(AssessmentCardTags.VALID_CARD_PREFIX + "cog-mem.txt").performClick()
        composeRule.waitForIdle()

        val route = navController.currentDestination?.route
        assertTrue("route should start with scan?configId=", route?.startsWith("scan?configId=") == true)
        val configId = navController.currentBackStackEntry?.arguments?.getString("configId")
        assertEquals("cog-mem", configId)
    }
}
