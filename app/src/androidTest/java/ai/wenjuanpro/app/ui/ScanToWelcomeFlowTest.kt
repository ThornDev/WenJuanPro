package ai.wenjuanpro.app.ui

import org.junit.Ignore
import org.junit.Test

/**
 * E2E Compose UI test for Story 2.1 — ScanToWelcomeFlow.
 *
 * Scenario IDs map to docs/qa/assessments/2.1-test-design-20260417.md.
 */
class ScanToWelcomeFlowTest {
    @Ignore(
        "E2E-004 requires a Hilt test harness (HiltAndroidTest + HiltAndroidRule), a FakeQrSource " +
            "test binding, and a seeded /sdcard/WenJuanPro/configs/ fixture — none of which exist in " +
            "this repo yet (build + instrumentation verification is deferred to Android Studio per " +
            "project memory 'build_deferred'). Deterministic cross-VM coverage is provided by " +
            "ScanViewModelTest UNIT-013 (session write + NavigateToWelcome) + UNIT-014 (ordering) and " +
            "the integration of ConfigListViewModel → SessionStateHolder is covered by Story 1.4's " +
            "ConfigListViewModelTest. A follow-up story (tracked in Story 2.1 Open Issues) should " +
            "introduce the FakeQrSource binding and re-enable this test.",
    )
    @Test
    fun `2_1-E2E-004 full flow config click to scan to welcome with session written`() = Unit
}
