package ai.wenjuanpro.app.feature.scan

import ai.wenjuanpro.app.data.permission.CameraPermissionRepository
import ai.wenjuanpro.app.domain.session.SessionStateHolder
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ScanViewModelTest {
    private lateinit var context: Context
    private lateinit var repo: CameraPermissionRepository
    private lateinit var session: SessionStateHolder

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repo = mockk(relaxed = true)
        session = SessionStateHolder()
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
        Timber.uprootAll()
    }

    private fun buildVm(
        configId: String? = DEFAULT_CONFIG_ID,
        granted: Boolean = true,
        hasBackCamera: Boolean = true,
        overrideSession: SessionStateHolder = session,
    ): ScanViewModel {
        every { repo.hasBackCamera(any()) } returns hasBackCamera
        every { repo.isCameraGranted(any()) } returns granted
        val handle =
            SavedStateHandle(
                if (configId == null) emptyMap() else mapOf("configId" to configId),
            )
        return ScanViewModel(repo, overrideSession, context, handle)
    }

    private suspend fun TestScope.drainEffects(vm: ScanViewModel): MutableList<ScanEffect> {
        val effects = mutableListOf<ScanEffect>()
        val job = launch { vm.effects.toList(effects) }
        advanceUntilIdle()
        job.cancel()
        return effects
    }

    @Test
    fun `2_1-UNIT-009 init with granted permission and back camera enters Preview`() =
        runTest {
            val vm = buildVm(granted = true, hasBackCamera = true)
            val state = vm.uiState.value
            assertTrue("expected Preview, got $state", state is ScanUiState.Preview)
            assertEquals(DEFAULT_CONFIG_ID, (state as ScanUiState.Preview).configId)
        }

    @Test
    fun `2_1-UNIT-010 init with no back camera enters NoCamera (BLIND-ERROR-003)`() =
        runTest {
            val vm = buildVm(hasBackCamera = false)
            assertTrue(vm.uiState.value is ScanUiState.NoCamera)
            val effects = drainEffects(vm)
            assertFalse(
                "NoCamera must not request permission",
                effects.any { it is ScanEffect.RequestCameraPermission },
            )
        }

    @Test
    fun `2_1-UNIT-011 init without permission emits RequestCameraPermission effect`() =
        runTest {
            val vm = buildVm(granted = false, hasBackCamera = true)
            assertTrue(vm.uiState.value is ScanUiState.CheckingPermission)
            val effects = drainEffects(vm)
            assertEquals(
                1,
                effects.count { it is ScanEffect.RequestCameraPermission },
            )
        }

    @Test
    fun `2_1-UNIT-012 OnPermissionResult false enters PermissionDenied CAMERA_DENIED (BLIND-ERROR-001)`() =
        runTest {
            val vm = buildVm(granted = false, hasBackCamera = true)
            vm.onIntent(ScanIntent.OnPermissionResult(granted = false))
            val state = vm.uiState.value
            assertTrue(state is ScanUiState.PermissionDenied)
            assertEquals(
                ScanViewModel.REASON_CAMERA_DENIED,
                (state as ScanUiState.PermissionDenied).reason,
            )
        }

    @Test
    fun `2_1-UNIT-013 valid qr writes session and navigates to welcome`() =
        runTest {
            val vm = buildVm()
            val effects = mutableListOf<ScanEffect>()
            val job = launch { vm.effects.toList(effects) }
            vm.onIntent(ScanIntent.OnQrDecoded("S001@beihai"))
            advanceUntilIdle()
            job.cancel()

            assertEquals("S001", session.studentId.value)
            val navigateCount =
                effects.count { eff ->
                    eff is ScanEffect.NavigateToWelcome &&
                        eff.studentId == "S001" &&
                        eff.configId == DEFAULT_CONFIG_ID
                }
            assertEquals(1, navigateCount)
            val finalState = vm.uiState.value
            assertTrue(finalState is ScanUiState.Recognized)
            assertEquals("S001", (finalState as ScanUiState.Recognized).studentId)
        }

    @Test
    fun `2_1-UNIT-014 setStudentId happens before NavigateToWelcome (BR-1_6 BLIND-CONCURRENCY-002)`() =
        runTest {
            val spySession = spyk(SessionStateHolder())
            val calls = mutableListOf<String>()
            every { spySession.setStudentId(any()) } answers {
                calls.add("setStudentId")
                callOriginal()
            }
            val vm = buildVm(overrideSession = spySession)
            val job =
                launch {
                    vm.effects.collect { eff ->
                        if (eff is ScanEffect.NavigateToWelcome) {
                            calls.add("navigate")
                        }
                    }
                }
            vm.onIntent(ScanIntent.OnQrDecoded("S001@beihai"))
            advanceUntilIdle()
            job.cancel()
            assertEquals(listOf("setStudentId", "navigate"), calls)
        }

    @Test
    fun `2_1-UNIT-015 invalid qr emits snackbar and keeps scanning (STUDENT_ID_INVALID)`() =
        runTest {
            val vm = buildVm()
            val effects = mutableListOf<ScanEffect>()
            val job = launch { vm.effects.toList(effects) }
            vm.onIntent(ScanIntent.OnQrDecoded(" S001 "))
            advanceUntilIdle()
            job.cancel()

            assertNull(session.studentId.value)
            assertTrue(effects.none { it is ScanEffect.NavigateToWelcome })
            val state = vm.uiState.value
            assertTrue(state is ScanUiState.Preview)
            assertNotNull((state as ScanUiState.Preview).transientSnackbar)
        }

    @Test
    fun `2_1-UNIT-016 duplicate qr decoded emits navigate only once (BR-1_10 BLIND-CONCURRENCY-001)`() =
        runTest {
            val vm = buildVm()
            val effects = mutableListOf<ScanEffect>()
            val job = launch { vm.effects.toList(effects) }
            vm.onIntent(ScanIntent.OnQrDecoded("S001@beihai"))
            vm.onIntent(ScanIntent.OnQrDecoded("S001@beihai"))
            vm.onIntent(ScanIntent.OnQrDecoded("S001@beihai"))
            advanceUntilIdle()
            job.cancel()

            assertEquals(
                1,
                effects.count { it is ScanEffect.NavigateToWelcome },
            )
        }

    @Test
    fun `2_1-UNIT-017 missing configId empty string navigates back immediately (CONFIG_MISSING)`() =
        runTest {
            val vm = buildVm(configId = "")
            val effects = drainEffects(vm)
            assertEquals(1, effects.count { it is ScanEffect.NavigateBack })
            assertFalse(effects.any { it is ScanEffect.RequestCameraPermission })
            assertFalse("Preview must not be entered", vm.uiState.value is ScanUiState.Preview)
        }

    @Test
    fun `2_1-UNIT-018 missing configId null navigates back immediately`() =
        runTest {
            val vm = buildVm(configId = null)
            val effects = drainEffects(vm)
            assertEquals(1, effects.count { it is ScanEffect.NavigateBack })
        }

    @Test
    fun `2_1-UNIT-019 OnRetryPermission emits RequestCameraPermission effect`() =
        runTest {
            val vm = buildVm(granted = true)
            val effects = mutableListOf<ScanEffect>()
            val job = launch { vm.effects.toList(effects) }
            vm.onIntent(ScanIntent.OnRetryPermission)
            advanceUntilIdle()
            job.cancel()
            assertEquals(
                1,
                effects.count { it is ScanEffect.RequestCameraPermission },
            )
        }

    @Test
    fun `2_1-UNIT-020 OnOpenSettings emits OpenAppSettings effect`() =
        runTest {
            val vm = buildVm()
            val effects = mutableListOf<ScanEffect>()
            val job = launch { vm.effects.toList(effects) }
            vm.onIntent(ScanIntent.OnOpenSettings)
            advanceUntilIdle()
            job.cancel()
            assertEquals(1, effects.count { it is ScanEffect.OpenAppSettings })
        }

    @Test
    fun `2_1-UNIT-021 OnCameraBindFailed transitions to PermissionDenied CAMERA_BIND_FAILED (BLIND-ERROR-002)`() =
        runTest {
            val vm = buildVm()
            vm.onIntent(ScanIntent.OnCameraBindFailed)
            val state = vm.uiState.value
            assertTrue(state is ScanUiState.PermissionDenied)
            assertEquals(
                ScanViewModel.REASON_CAMERA_BIND_FAILED,
                (state as ScanUiState.PermissionDenied).reason,
            )
        }

    @Test
    fun `2_1-UNIT-022 SnackbarShown clears transientSnackbar to null`() =
        runTest {
            val vm = buildVm()
            vm.onIntent(ScanIntent.OnQrDecoded(" bad "))
            val withSnackbar = vm.uiState.value
            assertTrue(withSnackbar is ScanUiState.Preview)
            assertNotNull((withSnackbar as ScanUiState.Preview).transientSnackbar)
            vm.onIntent(ScanIntent.SnackbarShown)
            val cleared = vm.uiState.value
            assertTrue(cleared is ScanUiState.Preview)
            assertNull((cleared as ScanUiState.Preview).transientSnackbar)
        }

    @Test
    fun `2_1-UNIT-023 OnNavigateBack emits NavigateBack effect`() =
        runTest {
            val vm = buildVm()
            val effects = mutableListOf<ScanEffect>()
            val job = launch { vm.effects.toList(effects) }
            vm.onIntent(ScanIntent.OnNavigateBack)
            advanceUntilIdle()
            job.cancel()
            assertEquals(1, effects.count { it is ScanEffect.NavigateBack })
        }

    @Test
    fun `2_1-UNIT-030 timber logs do not contain raw scanned studentId (NFR9)`() =
        runTest {
            val captured = mutableListOf<String>()
            val tree =
                object : Timber.Tree() {
                    override fun log(
                        priority: Int,
                        tag: String?,
                        message: String,
                        t: Throwable?,
                    ) {
                        captured.add(message)
                    }
                }
            Timber.plant(tree)
            val vm = buildVm()
            val sensitiveId = "S001-SENSITIVE"
            val badId = " bad "
            vm.onIntent(ScanIntent.OnQrDecoded(sensitiveId))
            advanceUntilIdle()
            // Reset for second decode by emulating fresh Preview (new VM since the first one is Recognized).
            val vm2 = buildVm(overrideSession = SessionStateHolder())
            vm2.onIntent(ScanIntent.OnQrDecoded(badId))
            advanceUntilIdle()

            val dump = captured.joinToString("\n")
            assertFalse("raw sensitive id leaked: $dump", dump.contains(sensitiveId))
            assertFalse("raw bad id leaked: $dump", dump.contains(badId))
            // Metadata presence (sanity): at least one entry mentions len or valid/invalid.
            assertTrue(
                "expected metadata entry in $dump",
                dump.contains("len=") || dump.contains("valid") || dump.contains("invalid"),
            )
        }

    private companion object {
        const val DEFAULT_CONFIG_ID = "cog-mem-2026q3"
    }
}
