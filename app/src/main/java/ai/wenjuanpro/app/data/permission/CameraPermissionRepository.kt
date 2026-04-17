package ai.wenjuanpro.app.data.permission

import android.content.Context

interface CameraPermissionRepository {
    fun isCameraGranted(context: Context): Boolean

    fun hasBackCamera(context: Context): Boolean
}
