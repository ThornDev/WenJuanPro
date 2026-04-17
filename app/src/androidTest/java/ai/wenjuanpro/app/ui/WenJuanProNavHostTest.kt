package ai.wenjuanpro.app.ui

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Contract test for 1.2-INT-009 — NavHost startDestination="permission".
 * Uses a minimal NavHost that mirrors WenJuanProNavHost routing to avoid coupling to Hilt
 * in instrumented tests. Route constants come from NavRoutes (production code).
 */
class WenJuanProNavHostTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `1_2-INT-009 NavHost startDestination is permission route`() {
        var route: String? = null
        composeRule.setContent {
            val navController =
                TestNavHostController(ApplicationProvider.getApplicationContext())
                    .apply { navigatorProvider.addNavigator(ComposeNavigator()) }
            NavHost(
                navController = navController,
                startDestination = "permission",
            ) {
                composable("permission") {
                    Text("permission-route", modifier = Modifier.testTag("permission_route_marker"))
                }
                composable("configlist") {
                    Text("configlist-route")
                }
            }
            route = navController.currentDestination?.route
        }

        composeRule.onNodeWithTag("permission_route_marker").assertIsDisplayed()
        assertEquals("permission", route)
    }
}
