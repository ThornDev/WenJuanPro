package ai.wenjuanpro.app.core.device

import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidDeviceIdProvider
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : DeviceIdProvider {
        override fun ssaid(): String? =
            try {
                val raw = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                raw?.takeIf { it.isNotBlank() }
            } catch (e: SecurityException) {
                Timber.w("ssaid read denied; code=SSAID_UNAVAILABLE")
                null
            }
    }
