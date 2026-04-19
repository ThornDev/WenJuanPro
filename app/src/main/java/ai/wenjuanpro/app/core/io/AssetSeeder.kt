package ai.wenjuanpro.app.core.io

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssetSeeder
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun seedIfNeeded() {
            runCatching {
                seedDir(apkAssetDir = "assets", target = File("/sdcard/WenJuanPro/assets"))
                seedDir(apkAssetDir = "config", target = File("/sdcard/WenJuanPro/config"))
            }.onFailure { Timber.w(it, "asset seed failed") }
        }

        private fun seedDir(
            apkAssetDir: String,
            target: File,
        ) {
            val bundled = context.assets.list(apkAssetDir)?.takeIf { it.isNotEmpty() } ?: return
            val alreadyPopulated = target.isDirectory && (target.listFiles()?.isNotEmpty() == true)
            if (alreadyPopulated) return
            if (!target.exists() && !target.mkdirs()) {
                Timber.w("cannot create seed target $target")
                return
            }
            bundled.forEach { name ->
                val out = File(target, name)
                if (out.exists()) return@forEach
                runCatching {
                    context.assets.open("$apkAssetDir/$name").use { input ->
                        out.outputStream().use { output -> input.copyTo(output) }
                    }
                }.onFailure { Timber.w(it, "seed copy failed for $name") }
            }
            Timber.i("seeded ${bundled.size} files into $target")
        }
    }
