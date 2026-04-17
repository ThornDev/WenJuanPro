package ai.wenjuanpro.app.ui.permission

import ai.wenjuanpro.app.feature.permission.PermissionUiState
import ai.wenjuanpro.app.ui.screens.permission.PermissionContent
import ai.wenjuanpro.app.ui.screens.permission.PermissionScreenTags
import ai.wenjuanpro.app.ui.theme.WenJuanProTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for Story 1.2. Covers 1.2-INT-007, 1.2-INT-008, 1.2-BLIND-FLOW-003.
 * Requires emulator / physical device; run via `./gradlew connectedAndroidTest`.
 */
class PermissionScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `1_2-INT-007 NotGranted with intent available renders main button and hides fallback`() {
        composeRule.setContent {
            WenJuanProTheme {
                PermissionContent(
                    state = PermissionUiState.NotGranted(intentAvailable = true),
                    onIntent = {},
                )
            }
        }

        composeRule.onNodeWithTag(PermissionScreenTags.PRIMARY_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithTag(PermissionScreenTags.RECHECK_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithTag(PermissionScreenTags.FALLBACK_COPY_BUTTON)
            .assertDoesNotExist()
    }

    @Test
    fun `1_2-INT-008 NotGranted with intent unavailable renders fallback and hides main button`() {
        composeRule.setContent {
            WenJuanProTheme {
                PermissionContent(
                    state = PermissionUiState.NotGranted(intentAvailable = false),
                    onIntent = {},
                )
            }
        }

        composeRule.onNodeWithTag(PermissionScreenTags.FALLBACK_TEXT).assertIsDisplayed()
        composeRule.onNodeWithTag(PermissionScreenTags.FALLBACK_COPY_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithTag(PermissionScreenTags.PRIMARY_BUTTON).assertDoesNotExist()
    }
}
