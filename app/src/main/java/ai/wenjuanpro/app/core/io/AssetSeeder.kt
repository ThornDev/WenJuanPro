package ai.wenjuanpro.app.core.io

import ai.wenjuanpro.app.BuildConfig
import android.content.Context
import android.media.MediaScannerConnection
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
                val root = File(ROOT_DIR)
                if (!root.exists() && !root.mkdirs()) {
                    Timber.w("cannot create seed root $root")
                    return@runCatching
                }
                ensureNoMediaMarker(root)
                val forceOverwrite = shouldForceOverwrite(root)
                seedDir(apkAssetDir = "assets", target = File(root, "assets"), forceOverwrite = forceOverwrite)
                seedDir(apkAssetDir = "config", target = File(root, "config"), forceOverwrite = forceOverwrite)
                writeSeedVersion(root)
                requestMediaRescan(root)
            }.onFailure { Timber.w(it, "asset seed failed") }
        }

        /**
         * Drop a single `.nomedia` marker at the data root. Android's
         * MediaScanner skips any directory (and its subtree) that contains
         * such a file, so the question images and audios will no longer
         * appear in the system gallery / files browsers.
         */
        private fun ensureNoMediaMarker(root: File) {
            val marker = File(root, NOMEDIA_FILE)
            if (marker.exists()) return
            runCatching { marker.createNewFile() }
                .onSuccess { Timber.i(".nomedia created at $marker") }
                .onFailure { Timber.w(it, ".nomedia create failed at $marker") }
        }

        /**
         * Ask MediaScanner to revisit every file beneath [root]. Files that
         * were indexed before .nomedia existed get removed from the
         * MediaStore on this pass, so the gallery clears itself without a
         * reboot.
         */
        private fun requestMediaRescan(root: File) {
            runCatching {
                val paths =
                    root.walkTopDown()
                        .filter { it.isFile }
                        .map { it.absolutePath }
                        .toList()
                        .toTypedArray()
                if (paths.isEmpty()) return
                MediaScannerConnection.scanFile(context, paths, null, null)
                Timber.d("media rescan requested files=${paths.size}")
            }.onFailure { Timber.w(it, "media rescan request failed") }
        }

        private fun seedDir(
            apkAssetDir: String,
            target: File,
            forceOverwrite: Boolean,
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
                    seedDir(apkPath, out, forceOverwrite)
                } else {
                    if (!forceOverwrite && out.exists()) return@forEach
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

        private fun shouldForceOverwrite(root: File): Boolean {
            val marker = File(root, SEED_VERSION_FILE)
            val oldVersion = marker.takeIf { it.exists() }?.readText()?.trim()
            val currentVersion = BuildConfig.VERSION_CODE.toString()
            val force = oldVersion != currentVersion
            if (force) {
                Timber.i("seed version changed old=$oldVersion new=$currentVersion; overwrite enabled")
            }
            return force
        }

        private fun writeSeedVersion(root: File) {
            val marker = File(root, SEED_VERSION_FILE)
            marker.writeText(BuildConfig.VERSION_CODE.toString())
        }

        companion object {
            private const val ROOT_DIR = "/sdcard/WenJuanPro"
            private const val SEED_VERSION_FILE = ".seed_version"
            private const val NOMEDIA_FILE = ".nomedia"
        }
    }
