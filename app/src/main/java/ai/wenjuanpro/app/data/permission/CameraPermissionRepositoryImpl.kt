package ai.wenjuanpro.app.data.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CameraPermissionRepositoryImpl
    @Inject
    constructor() : CameraPermissionRepository {
        override fun isCameraGranted(context: Context): Boolean =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA,
            ) == PackageManager.PERMISSION_GRANTED

        override fun hasBackCamera(context: Context): Boolean =
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
    }
