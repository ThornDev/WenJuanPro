package ai.wenjuanpro.app.feature.permission

import ai.wenjuanpro.app.data.permission.PermissionRepository
import ai.wenjuanpro.app.domain.usecase.CheckPermissionUseCase
import android.content.Intent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.channels.Channel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for Story 1.2: MANAGE_EXTERNAL_STORAGE 授权引导
 *
 * Implements the test-design skeleton at docs/qa/assessments/1.2-test-design-20260417.md.
 * Do NOT delete any `@Test` — each maps to a designed scenario.
 */
class PermissionViewModelTest {
    private val repo: PermissionRepository = mockk(relaxed = true)
    private val intent: Intent = mockk(relaxed = true)

    private fun viewModel(): PermissionViewModel {
        val useCase = CheckPermissionUseCase(repo)
        return PermissionViewModel(useCase, repo)
    }

    // ============================================================
    // AC1: 未授权时引导用户跳转系统设置 — Core scenarios
    // ============================================================

    @Test
    fun `1_2-UNIT-001 CheckPermission when granted emits Granted state and NavigateToConfigList effect`() {
        every { repo.isExternalStorageManager() } returns true
        val vm = viewModel()

        vm.onIntent(PermissionIntent.CheckPermission)

        assertEquals(PermissionUiState.Granted, vm.uiState.value)
        assertEquals(listOf(PermissionEffect.NavigateToConfigList), drainEffects(vm))
    }

    @Test
    fun `1_2-UNIT-002 CheckPermission when not granted and intent available emits NotGranted intentAvailable=true`() {
        every { repo.isExternalStorageManager() } returns false
        every { repo.buildManageStorageIntent() } returns intent
        val vm = viewModel()

        vm.onIntent(PermissionIntent.CheckPermission)

        assertEquals(
            PermissionUiState.NotGranted(intentAvailable = true, hasAttempted = false),
            vm.uiState.value,
        )
    }

    @Test
    fun `1_2-UNIT-003 CheckPermission when not granted and intent unavailable emits NotGranted intentAvailable=false`() {
        every { repo.isExternalStorageManager() } returns false
        every { repo.buildManageStorageIntent() } returns null
        val vm = viewModel()

        vm.onIntent(PermissionIntent.CheckPermission)

        assertEquals(
            PermissionUiState.NotGranted(intentAvailable = false, hasAttempted = false),
            vm.uiState.value,
        )
    }

    @Test
    fun `1_2-UNIT-004 OpenSettings when intent available emits LaunchSettings effect and sets hasAttempted true`() {
        every { repo.isExternalStorageManager() } returns false
        every { repo.buildManageStorageIntent() } returns intent
        val vm = viewModel()
        vm.onIntent(PermissionIntent.CheckPermission)

        vm.onIntent(PermissionIntent.OpenSettings)

        val state = vm.uiState.value as PermissionUiState.NotGranted
        assertTrue(state.hasAttempted)
        val effects = drainEffects(vm)
        assertTrue(effects.any { it is PermissionEffect.LaunchSettings && it.intent === intent })
    }

    @Test
    fun `1_2-UNIT-005 OpenSettings when intent unavailable emits ShowIntentUnavailable effect`() {
        every { repo.isExternalStorageManager() } returns false
        every { repo.buildManageStorageIntent() } returns null
        val vm = viewModel()
        vm.onIntent(PermissionIntent.CheckPermission)

        vm.onIntent(PermissionIntent.OpenSettings)

        val effects = drainEffects(vm)
        assertTrue(effects.contains(PermissionEffect.ShowIntentUnavailable))
        assertFalse(effects.any { it is PermissionEffect.LaunchSettings })
    }

    @Test
    fun `1_2-UNIT-006 Recheck after settings return granted transitions to Granted`() {
        every { repo.isExternalStorageManager() } returns false
        every { repo.buildManageStorageIntent() } returns intent
        val vm = viewModel()
        vm.onIntent(PermissionIntent.CheckPermission)
        vm.onIntent(PermissionIntent.OpenSettings)

        every { repo.isExternalStorageManager() } returns true
        vm.onIntent(PermissionIntent.Recheck)

        assertEquals(PermissionUiState.Granted, vm.uiState.value)
        val effects = drainEffects(vm)
        assertTrue(effects.contains(PermissionEffect.NavigateToConfigList))
    }

    @Test
    fun `1_2-UNIT-007 Recheck after settings return still denied keeps NotGranted with hasAttempted true`() {
        every { repo.isExternalStorageManager() } returns false
        every { repo.buildManageStorageIntent() } returns intent
        val vm = viewModel()
        vm.onIntent(PermissionIntent.CheckPermission)
        vm.onIntent(PermissionIntent.OpenSettings)

        vm.onIntent(PermissionIntent.Recheck)

        assertEquals(
            PermissionUiState.NotGranted(intentAvailable = true, hasAttempted = true),
            vm.uiState.value,
        )
    }

    @Test
    fun `1_2-UNIT-008 CheckPermissionUseCase delegates to repository isExternalStorageManager`() {
        val useCase = CheckPermissionUseCase(repo)

        every { repo.isExternalStorageManager() } returns true
        assertTrue(useCase.invoke())

        every { repo.isExternalStorageManager() } returns false
        assertFalse(useCase.invoke())

        verify(exactly = 2) { repo.isExternalStorageManager() }
    }

    // ============================================================
    // AC1: 未授权时引导用户跳转系统设置 — Blind-spot scenarios
    // ============================================================

    @Test
    fun `1_2-BLIND-ERROR-001 OpenSettings when repo returns null intent emits ShowIntentUnavailable not crash`() {
        every { repo.isExternalStorageManager() } returns false
        every { repo.buildManageStorageIntent() } returns intent
        val vm = viewModel()
        vm.onIntent(PermissionIntent.CheckPermission)
        drainEffects(vm)

        every { repo.buildManageStorageIntent() } returns null
        vm.onIntent(PermissionIntent.OpenSettings)

        val effects = drainEffects(vm)
        assertEquals(listOf(PermissionEffect.ShowIntentUnavailable), effects)
        val state = vm.uiState.value as PermissionUiState.NotGranted
        assertFalse(state.intentAvailable)
    }

    @Test
    fun `1_2-BLIND-FLOW-001 Multi-step retry flow flips hasAttempted and survives state transitions`() {
        every { repo.isExternalStorageManager() } returns false
        every { repo.buildManageStorageIntent() } returns intent
        val vm = viewModel()

        vm.onIntent(PermissionIntent.CheckPermission)
        var state = vm.uiState.value as PermissionUiState.NotGranted
        assertFalse(state.hasAttempted)

        vm.onIntent(PermissionIntent.OpenSettings)
        state = vm.uiState.value as PermissionUiState.NotGranted
        assertTrue(state.hasAttempted)

        vm.onIntent(PermissionIntent.Recheck)
        state = vm.uiState.value as PermissionUiState.NotGranted
        assertTrue(state.hasAttempted)
        assertTrue(state.intentAvailable)
    }

    @Test
    fun `1_2-BLIND-FLOW-002 Duplicate OpenSettings within short window does not corrupt state`() {
        every { repo.isExternalStorageManager() } returns false
        every { repo.buildManageStorageIntent() } returns intent
        val vm = viewModel()
        vm.onIntent(PermissionIntent.CheckPermission)

        vm.onIntent(PermissionIntent.OpenSettings)
        vm.onIntent(PermissionIntent.OpenSettings)

        val state = vm.uiState.value as PermissionUiState.NotGranted
        assertTrue(state.hasAttempted)
        val effects = drainEffects(vm)
        assertFalse(effects.any { it == PermissionEffect.NavigateToConfigList })
    }

    @Test
    fun `1_2-BLIND-CONCURRENCY-001 Two rapid ON_RESUME-triggered CheckPermission calls converge consistently`() {
        every { repo.isExternalStorageManager() } returns false
        every { repo.buildManageStorageIntent() } returns intent
        val vm = viewModel()

        vm.onIntent(PermissionIntent.CheckPermission)
        every { repo.isExternalStorageManager() } returns true
        vm.onIntent(PermissionIntent.CheckPermission)

        assertEquals(PermissionUiState.Granted, vm.uiState.value)
        val effects = drainEffects(vm)
        val navCount = effects.count { it == PermissionEffect.NavigateToConfigList }
        assertTrue("nav count should be ≤ 2 but was $navCount", navCount in 1..2)
    }

    // ============================================================
    // Helpers
    // ============================================================

    @Suppress("UNCHECKED_CAST")
    private fun drainEffects(vm: PermissionViewModel): List<PermissionEffect> {
        val out = mutableListOf<PermissionEffect>()
        val field =
            PermissionViewModel::class.java.getDeclaredField("_effect").apply { isAccessible = true }
        val ch = field.get(vm) as Channel<PermissionEffect>
        while (true) {
            val result = ch.tryReceive()
            if (result.isSuccess) {
                out += result.getOrNull()!!
            } else {
                break
            }
        }
        return out
    }
}
