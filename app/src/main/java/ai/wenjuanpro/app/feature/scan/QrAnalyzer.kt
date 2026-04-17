package ai.wenjuanpro.app.feature.scan

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

internal interface QrDecoder {
    fun decode(
        yuv: ByteArray,
        width: Int,
        height: Int,
    ): String?

    companion object {
        fun default(): QrDecoder = ZxingQrDecoder()
    }
}

internal object QrHints {
    val ZXING_HINTS: Map<DecodeHintType, Any> =
        mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE))
}

private class ZxingQrDecoder : QrDecoder {
    private val reader =
        MultiFormatReader().apply {
            setHints(QrHints.ZXING_HINTS)
        }

    override fun decode(
        yuv: ByteArray,
        width: Int,
        height: Int,
    ): String? {
        val source =
            PlanarYUVLuminanceSource(
                yuv,
                width,
                height,
                0,
                0,
                width,
                height,
                false,
            )
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        return try {
            reader.decodeWithState(bitmap).text
        } catch (_: NotFoundException) {
            null
        } finally {
            reader.reset()
        }
    }
}

class QrAnalyzer internal constructor(
    private val decoder: QrDecoder,
    private val onDecoded: (String) -> Unit,
) : ImageAnalysis.Analyzer {
    constructor(onDecoded: (String) -> Unit) : this(QrDecoder.default(), onDecoded)

    private val fired = AtomicBoolean(false)

    override fun analyze(image: ImageProxy) {
        try {
            if (fired.get()) return
            val yPlane = image.planes.getOrNull(0) ?: return
            val buffer = yPlane.buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            val decoded =
                try {
                    decoder.decode(bytes, image.width, image.height)
                } catch (e: Exception) {
                    Timber.d("qr decode error cls=${e::class.simpleName}")
                    null
                }
            if (decoded != null && fired.compareAndSet(false, true)) {
                onDecoded(decoded)
            }
        } finally {
            image.close()
        }
    }
}
