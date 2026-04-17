package ai.wenjuanpro.app.data.permission

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * Robolectric-backed tests for Story 2.1 — CameraPermissionRepositoryImpl.
 */
@RunWith(RobolectricTestRunner::class)
class CameraPermissionRepositoryImplTest {
    private lateinit var context: Context
    private lateinit var repo: CameraPermissionRepositoryImpl

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repo = CameraPermissionRepositoryImpl()
    }

    @Test
    fun `2_1-INT-001 isCameraGranted reflects ContextCompat checkSelfPermission`() {
        val app = context.applicationContext as Application
        // Revoked by default.
        assertFalse(repo.isCameraGranted(context))
        // After grant.
        shadowOf(app).grantPermissions(Manifest.permission.CAMERA)
        assertTrue(repo.isCameraGranted(context))
        // After revocation.
        shadowOf(app).denyPermissions(Manifest.permission.CAMERA)
        assertFalse(repo.isCameraGranted(context))
    }

    @Test
    fun `2_1-INT-002 hasBackCamera reflects PackageManager hasSystemFeature CAMERA`() {
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_CAMERA, true)
        assertTrue(repo.hasBackCamera(context))
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_CAMERA, false)
        assertFalse(repo.hasBackCamera(context))
    }
}
