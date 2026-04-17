package ai.wenjuanpro.app.ui.screens.scan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun ScanOverlay(modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { 2.dp.toPx() }
    val cornerRadiusPx = with(density) { 16.dp.toPx() }

    Canvas(
        modifier =
            modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
    ) {
        drawRect(
            color = Color.Black.copy(alpha = 0.6f),
            size = size,
        )
        val boxSide = size.width * 0.7f
        val boxLeft = (size.width - boxSide) / 2f
        val boxTop = (size.height - boxSide) / 2f
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(boxLeft, boxTop),
            size = Size(boxSide, boxSide),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx, cornerRadiusPx),
            blendMode = BlendMode.Clear,
        )
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(boxLeft, boxTop),
            size = Size(boxSide, boxSide),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx, cornerRadiusPx),
            style = Stroke(width = strokeWidthPx),
        )
    }
}
