package ai.wenjuanpro.app.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

object DotGridColors {
    val Blue: Color = Color(0xFF1976D2)
    val Gray: Color = Color(0xFFE0E0E0)
    val Flash: Color = Color(0xFFFFB300)
    val Selected: Color = Color(0xFF4CAF50)
}

object DotGridTags {
    const val ROOT = "dot_grid"
}

@Composable
fun DotGrid(
    dotStates: List<DotState>,
    modifier: Modifier = Modifier,
    selectedSequence: List<Int> = emptyList(),
    onTap: ((Float, Float) -> Unit)? = null,
) {
    BoxWithConstraints(modifier = modifier) {
        val gridWidth = maxWidth
        val cellSizeDp = gridWidth / 8
        val cellSizePx = with(LocalDensity.current) { cellSizeDp.toPx() }
        val radius = cellSizePx * 0.35f
        val strokeWidthPx = with(LocalDensity.current) { 1.dp.toPx() }
        val textSize = cellSizePx * 0.3f

        val tapModifier = if (onTap != null) {
            Modifier.pointerInput(Unit) {
                detectTapGestures { offset ->
                    onTap(offset.x, offset.y)
                }
            }
        } else {
            Modifier
        }

        Canvas(
            modifier =
                Modifier
                    .size(gridWidth)
                    .then(tapModifier)
                    .testTag(DotGridTags.ROOT),
        ) {
            val textPaint = Paint().apply {
                color = android.graphics.Color.WHITE
                this.textSize = textSize
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }

            for (index in 0 until 64) {
                val row = index / 8
                val col = index % 8
                val cx = (col + 0.5f) * cellSizePx
                val cy = (row + 0.5f) * cellSizePx
                val center = Offset(cx, cy)
                val state = dotStates.getOrElse(index) { DotState.EMPTY }

                when (state) {
                    DotState.BLUE -> drawCircle(
                        color = DotGridColors.Blue,
                        radius = radius,
                        center = center,
                    )
                    DotState.EMPTY -> drawCircle(
                        color = DotGridColors.Gray,
                        radius = radius,
                        center = center,
                        style = Stroke(width = strokeWidthPx),
                    )
                    DotState.FLASHING -> drawCircle(
                        color = DotGridColors.Flash,
                        radius = radius,
                        center = center,
                    )
                    DotState.SELECTED -> {
                        drawCircle(
                            color = DotGridColors.Selected,
                            radius = radius,
                            center = center,
                        )
                        val seqIndex = selectedSequence.indexOf(index)
                        if (seqIndex >= 0) {
                            val label = (seqIndex + 1).toString()
                            drawIntoCanvas { canvas ->
                                val fontMetrics = textPaint.fontMetrics
                                val textY = cy - (fontMetrics.ascent + fontMetrics.descent) / 2f
                                canvas.nativeCanvas.drawText(label, cx, textY, textPaint)
                            }
                        }
                    }
                }
            }
        }
    }
}
