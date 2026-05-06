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
            val children = context.assets.list(apkAssetDir).orEmpty()
            if (children.isEmpty()) return
            if (!target.exists() && !target.mkdirs()) {
                Timber.w("cannot create seed target $target")
                return
            }
            var copied = 0
            children.forEach { name ->
                val apkPath = "$apkAssetDir/$name"
                val out = File(target, name)
                val sub = context.assets.list(apkPath).orEmpty()
                if (sub.isNotEmpty()) {
                    seedDir(apkPath, out)
                } else {
                    if (out.exists()) return@forEach
                    runCatching {
                        context.assets.open(apkPath).use { input ->
                            out.outputStream().use { output -> input.copyTo(output) }
                        }
                        copied += 1
                    }.onFailure { Timber.w(it, "seed copy failed for $apkPath") }
                }
            }
            if (copied > 0) Timber.i("seeded $copied new files into $target")
        }
    }
