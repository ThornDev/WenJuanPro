package ai.wenjuanpro.app.feature.scan

import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.DecodeHintType
import com.google.zxing.NotFoundException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer

/**
 * Tests for Story 2.1 — QrAnalyzer (ImageAnalysis.Analyzer + ZXing decode + AtomicBoolean debounce).
 *
 * Uses an injected [QrDecoder] stub to keep the test pure JVM (no Robolectric / CameraX hardware).
 */
class QrAnalyzerTest {
    private fun fakeImage(
        width: Int = 2,
        height: Int = 2,
        bytes: ByteArray = ByteArray(width * height),
    ): ImageProxy {
        val image = mockk<ImageProxy>(relaxed = true)
        val plane = mockk<ImageProxy.PlaneProxy>(relaxed = true)
        every { image.width } returns width
        every { image.height } returns height
        every { image.planes } returns arrayOf(plane, plane, plane)
        every { plane.buffer } returns ByteBuffer.wrap(bytes)
        return image
    }

    @Test
    fun `2_1-UNIT-024 analyze with valid qr frame invokes onDecoded once`() {
        val decoder =
            object : QrDecoder {
                override fun decode(
                    yuv: ByteArray,
                    width: Int,
                    height: Int,
                ): String = "S001"
            }
        val captured = mutableListOf<String>()
        val analyzer = QrAnalyzer(decoder) { captured.add(it) }
        val image = fakeImage()

        analyzer.analyze(image)

        assertEquals(listOf("S001"), captured)
        verify(exactly = 1) { image.close() }
    }

    @Test
    fun `2_1-UNIT-025 analyze called 3x same frame invokes onDecoded once (BR-1_10 BLIND-CONCURRENCY-001)`() {
        val decoder =
            object : QrDecoder {
                override fun decode(
                    yuv: ByteArray,
                    width: Int,
                    height: Int,
                ): String = "S001"
            }
        val captured = mutableListOf<String>()
        val analyzer = QrAnalyzer(decoder) { captured.add(it) }

        repeat(3) { analyzer.analyze(fakeImage()) }

        assertEquals(listOf("S001"), captured)
    }

    @Test
    fun `2_1-UNIT-026 multiformatreader hints contain only QR_CODE format (BR-1_8)`() {
        val hints = QrHints.ZXING_HINTS
        assertTrue(hints.containsKey(DecodeHintType.POSSIBLE_FORMATS))
        val formats = hints[DecodeHintType.POSSIBLE_FORMATS] as List<*>
        assertEquals(listOf(BarcodeFormat.QR_CODE), formats)
    }

    @Test
    fun `2_1-UNIT-027 image proxy is closed in finally even when decode throws (BLIND-RESOURCE-002)`() {
        val decoder =
            object : QrDecoder {
                override fun decode(
                    yuv: ByteArray,
                    width: Int,
                    height: Int,
                ): String? = throw NotFoundException.getNotFoundInstance()
            }
        val captured = mutableListOf<String>()
        val analyzer = QrAnalyzer(decoder) { captured.add(it) }
        val image = fakeImage()

        analyzer.analyze(image)

        assertTrue("onDecoded must not fire when decoder throws", captured.isEmpty())
        verify(exactly = 1) { image.close() }
    }
}
