package ai.wenjuanpro.app.ui.screens.scan

import ai.wenjuanpro.app.feature.scan.QrAnalyzer
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import timber.log.Timber
import java.util.concurrent.Executors

object CameraPreviewTags {
    const val PREVIEW_VIEW = "scan_preview_view"
}

@Composable
fun CameraPreviewView(
    onQrDecoded: (String) -> Unit,
    onBindFailed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView =
        remember {
            PreviewView(context).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        }

    DisposableEffect(lifecycleOwner) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)
        var boundProvider: ProcessCameraProvider? = null
        providerFuture.addListener({
            try {
                val cameraProvider = providerFuture.get()
                val preview =
                    Preview.Builder().build().apply {
                        setSurfaceProvider(previewView.surfaceProvider)
                    }
                val analysis =
                    ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                analysis.setAnalyzer(
                    cameraExecutor,
                    QrAnalyzer { content -> onQrDecoded(content) },
                )
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis,
                )
                boundProvider = cameraProvider
            } catch (e: Exception) {
                Timber.e("CameraX bind failed cls=${e::class.simpleName}")
                onBindFailed()
            }
        }, mainExecutor)

        onDispose {
            try {
                boundProvider?.unbindAll()
            } catch (e: Exception) {
                Timber.w("CameraX unbind threw cls=${e::class.simpleName}")
            }
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier.fillMaxSize().testTag(CameraPreviewTags.PREVIEW_VIEW),
    )
}
