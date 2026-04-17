package ai.wenjuanpro.app.ui.scan

import ai.wenjuanpro.app.feature.scan.ScanIntent
import ai.wenjuanpro.app.feature.scan.ScanUiState
import ai.wenjuanpro.app.ui.screens.scan.ScanContent
import ai.wenjuanpro.app.ui.screens.scan.ScanScreenTags
import ai.wenjuanpro.app.ui.theme.WenJuanProTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodes
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

class ScanScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `2_1-E2E-001 permission denied state renders retry and settings buttons (BLIND-ERROR-001)`() {
        val intents = mutableStateListOf<ScanIntent>()
        composeRule.setContent {
            WenJuanProTheme {
                ScanContent(
                    state = ScanUiState.PermissionDenied(reason = "CAMERA_DENIED"),
                    onIntent = { intents.add(it) },
                )
            }
        }
        composeRule.onNodeWithTag(ScanScreenTags.PERMISSION_DENIED_BODY).assertIsDisplayed()
        composeRule.onNodeWithTag(ScanScreenTags.RETRY_PERMISSION_BUTTON).performClick()
        composeRule.onNodeWithTag(ScanScreenTags.OPEN_SETTINGS_BUTTON).performClick()
        assertTrue(intents.any { it is ScanIntent.OnRetryPermission })
        assertTrue(intents.any { it is ScanIntent.OnOpenSettings })
    }

    @Test
    fun `2_1-E2E-002 no camera state renders back button (BLIND-FLOW-001 BLIND-ERROR-003)`() {
        val intents = mutableStateListOf<ScanIntent>()
        composeRule.setContent {
            WenJuanProTheme {
                ScanContent(
                    state = ScanUiState.NoCamera(),
                    onIntent = { intents.add(it) },
                )
            }
        }
        composeRule.onNodeWithTag(ScanScreenTags.NO_CAMERA_BODY).assertIsDisplayed()
        composeRule.onNodeWithTag(ScanScreenTags.NO_CAMERA_BACK_BUTTON).performClick()
        assertTrue(intents.any { it is ScanIntent.OnNavigateBack })
    }

    @Ignore(
        "E2E-003 requires a Hilt test harness + FakeQrSource to push a decoded payload through the " +
            "real CameraX/ZXing pipeline. Deterministic coverage is provided by ScanViewModelTest " +
            "UNIT-013 (session write + NavigateToWelcome) and UNIT-014 (ordering). Story 2.2 will " +
            "introduce the FakeQrSource test binding and re-enable this test.",
    )
    @Test
    fun `2_1-E2E-003 valid qr emits navigate to welcome route args (BLIND-DATA-002)`() = Unit

    @Ignore(
        "E2E-005 requires a TestNavHost with synthetic 'scan?configId=' route to observe popBackStack. " +
            "Equivalent behavior is covered at the VM boundary by UNIT-017 (empty configId → NavigateBack) " +
            "and UNIT-018 (null configId → NavigateBack).",
    )
    @Test
    fun `2_1-E2E-005 empty configId routes popBackStack immediately`() = Unit

    @Test
    fun `2_1-E2E-006 invalid qr shows snackbar and preserves preview state (BLIND-FLOW-002)`() {
        val message = "学号格式非法：bad text"
        composeRule.setContent {
            WenJuanProTheme {
                ScanContent(
                    state =
                        ScanUiState.Preview(
                            configId = "cog-mem-2026q3",
                            transientSnackbar = message,
                        ),
                    onIntent = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onAllNodes(hasText(message)).onFirst().assertIsDisplayed()
    }

    @Test
    fun `2_1-E2E-007 release build hides debug simulator input (BR-1_4)`() {
        composeRule.setContent {
            WenJuanProTheme {
                ScanContent(
                    state = ScanUiState.Preview(configId = "cog-mem-2026q3"),
                    onIntent = {},
                )
            }
        }
        composeRule.waitForIdle()
        val editableMatcher =
            SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText)
        composeRule.onAllNodes(editableMatcher).assertCountEquals(0)
    }

    @Ignore(
        "E2E-008 requires simulating Lifecycle.Event.ON_RESUME against a real Activity plus a Hilt " +
            "binding override for CameraPermissionRepository. The ScanScreen installs the observer " +
            "(DisposableEffect ON_RESUME → OnEnter); VM transition paths are covered by " +
            "ScanViewModelTest UNIT-009 (granted) and UNIT-012 (denied → Preview after grant).",
    )
    @Test
    fun `2_1-E2E-008 resume after settings grant transitions to preview (BLIND-FLOW-003)`() = Unit

    @Ignore(
        "E2E-009 requires createAndroidComposeRule<MainActivity>() + real CameraX bind. " +
            "CameraPreviewView.DisposableEffect.onDispose calls cameraProvider.unbindAll() and " +
            "cameraExecutor.shutdown() — verified by code review per Testing Requirements #3.",
    )
    @Test
    fun `2_1-E2E-009 activity recreate disposes camera resources cleanly (BLIND-RESOURCE-001)`() = Unit

    @Test
    fun `2_1-E2E-010 recognized state triggers haptic feedback exactly once`() {
        val hapticCalls = mutableStateListOf<HapticFeedbackType>()
        val spyHaptic =
            object : HapticFeedback {
                override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                    hapticCalls.add(hapticFeedbackType)
                }
            }
        val state = mutableStateOf<ScanUiState>(ScanUiState.Preview(configId = "cog-mem-2026q3"))
        composeRule.setContent {
            WenJuanProTheme {
                CompositionLocalProvider(LocalHapticFeedback provides spyHaptic) {
                    ScanContent(state = state.value, onIntent = {})
                }
            }
        }
        composeRule.runOnIdle {
            state.value = ScanUiState.Recognized(studentId = "S001")
        }
        composeRule.waitForIdle()
        assertEquals(listOf(HapticFeedbackType.LongPress), hapticCalls.toList())
    }
}
