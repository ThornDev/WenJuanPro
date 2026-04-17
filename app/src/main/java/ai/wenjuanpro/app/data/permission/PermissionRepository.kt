package ai.wenjuanpro.app.data.permission

import android.content.Intent

interface PermissionRepository {
    fun isExternalStorageManager(): Boolean

    fun buildManageStorageIntent(): Intent?
}
