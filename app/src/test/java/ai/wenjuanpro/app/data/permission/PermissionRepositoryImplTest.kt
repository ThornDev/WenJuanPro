package ai.wenjuanpro.app.data.permission

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
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
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowEnvironment

/**
 * Tests for Story 1.2: PermissionRepositoryImpl.
 *
 * Covers SDK branching (BR-1.1), Intent construction, resolvability fallback, and defensive
 * SecurityException handling. Scenarios map to docs/qa/assessments/1.2-test-design-20260417.md.
 */
@RunWith(RobolectricTestRunner::class)
class PermissionRepositoryImplTest {
    private lateinit var context: Context
    private lateinit var repo: PermissionRepositoryImpl

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repo = PermissionRepositoryImpl(context)
    }

    @After
    fun tearDown() {
        ShadowEnvironment.setExternalStorageManager(false)
    }

    @Test
    @Config(sdk = [30])
    fun `1_2-INT-001 isExternalStorageManager on API 30+ returns true when Environment reports true`() {
        ShadowEnvironment.setExternalStorageManager(true)
        assertTrue(repo.isExternalStorageManager())
    }

    @Test
    @Config(sdk = [33])
    fun `1_2-INT-002 isExternalStorageManager on API 30+ returns false when Environment reports false`() {
        ShadowEnvironment.setExternalStorageManager(false)
        assertFalse(repo.isExternalStorageManager())
    }

    @Test
    @Config(sdk = [29])
    fun `1_2-INT-003 isExternalStorageManager on API 29 returns true when READ_EXTERNAL_STORAGE granted`() {
        val app = context.applicationContext as Application
        shadowOf(app).grantPermissions(Manifest.permission.READ_EXTERNAL_STORAGE)
        assertTrue(repo.isExternalStorageManager())
    }

    @Test
    @Config(sdk = [29])
    fun `1_2-INT-004 isExternalStorageManager on API 29 returns false when READ_EXTERNAL_STORAGE denied`() {
        assertFalse(repo.isExternalStorageManager())
    }

    @Test
    @Config(sdk = [30])
    fun `1_2-INT-005 buildManageStorageIntent on API 30+ returns resolvable MANAGE_APP_ALL_FILES_ACCESS intent`() {
        registerSettingsResolver()

        val intent = repo.buildManageStorageIntent()

        assertNotNull(intent)
        assertEquals(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, intent!!.action)
        assertEquals("package:${context.packageName}", intent.dataString)
        assertNotNull(intent.resolveActivity(context.packageManager))
    }

    @Test
    @Config(sdk = [29])
    fun `1_2-INT-006 buildManageStorageIntent on API 29 returns null (legacy path)`() {
        assertNull(repo.buildManageStorageIntent())
    }

    @Test
    @Config(sdk = [30])
    fun `1_2-BLIND-BOUNDARY-001 buildManageStorageIntent returns null when no activity resolves`() {
        // Intentionally do NOT register a resolver → packageManager.resolveActivity returns null
        assertNull(repo.buildManageStorageIntent())
    }

    @Test
    @Config(sdk = [29])
    fun `1_2-BLIND-BOUNDARY-002a API boundary SDK 29 uses legacy ContextCompat path`() {
        val app = context.applicationContext as Application
        shadowOf(app).grantPermissions(Manifest.permission.READ_EXTERNAL_STORAGE)
        assertTrue(repo.isExternalStorageManager())
    }

    @Test
    @Config(sdk = [30])
    fun `1_2-BLIND-BOUNDARY-002b API boundary SDK 30 uses Environment path not legacy permission`() {
        // Do not grant READ_EXTERNAL_STORAGE — on SDK 30 the legacy permission must NOT be consulted.
        ShadowEnvironment.setExternalStorageManager(true)
        assertTrue(repo.isExternalStorageManager())
    }

    @Test
    @Config(sdk = [30])
    fun `1_2-BLIND-ERROR-002 isExternalStorageManager returns false defensively on SecurityException`() {
        mockkStatic(Environment::class)
        try {
            every { Environment.isExternalStorageManager() } throws SecurityException("denied")
            assertFalse(repo.isExternalStorageManager())
        } finally {
            unmockkStatic(Environment::class)
        }
    }

    private fun registerSettingsResolver() {
        val intent =
            Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:${context.packageName}"),
            )
        val resolveInfo =
            ResolveInfo().apply {
                activityInfo =
                    ActivityInfo().apply {
                        packageName = "com.android.settings"
                        name = "com.android.settings.ManageApplications"
                    }
            }
        shadowOf(context.packageManager).addResolveInfoForIntent(intent, resolveInfo)
    }
}
