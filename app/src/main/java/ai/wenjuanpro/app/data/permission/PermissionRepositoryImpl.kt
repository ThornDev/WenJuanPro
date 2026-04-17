package ai.wenjuanpro.app.data.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : PermissionRepository {
        override fun isExternalStorageManager(): Boolean =
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Environment.isExternalStorageManager()
                } else {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                    ) == PackageManager.PERMISSION_GRANTED
                }
            } catch (e: SecurityException) {
                Timber.w("permission check threw SecurityException; defaulting to denied")
                false
            }

        override fun buildManageStorageIntent(): Intent? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
            val intent =
                Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:${context.packageName}"),
                )
            return if (intent.resolveActivity(context.packageManager) != null) intent else null
        }
    }
