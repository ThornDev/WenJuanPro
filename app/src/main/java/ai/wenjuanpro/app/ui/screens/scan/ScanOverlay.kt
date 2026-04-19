package ai.wenjuanpro.app.ui.screens.scan

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

private val BracketBlue = Color(0xFF42A5F5)
private val LaserTop = Color(0x0042A5F5)
private val LaserMid = Color(0xCC42A5F5)

@Composable
fun ScanOverlay(modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val bracketStrokePx = with(density) { 4.dp.toPx() }
    val bracketLenPx = with(density) { 28.dp.toPx() }
    val cornerRadiusPx = with(density) { 16.dp.toPx() }

    val transition = rememberInfiniteTransition(label = "scan_laser")
    val laserPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 2200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "laser_phase",
    )

    Canvas(
        modifier =
            modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
    ) {
        drawRect(color = Color.Black.copy(alpha = 0.62f), size = size)

        val boxSide = size.width * 0.72f
        val boxLeft = (size.width - boxSide) / 2f
        val boxTop = (size.height - boxSide) / 2f
        val boxRight = boxLeft + boxSide
        val boxBottom = boxTop + boxSide

        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(boxLeft, boxTop),
            size = Size(boxSide, boxSide),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx, cornerRadiusPx),
            blendMode = BlendMode.Clear,
        )

        val laserTravel = boxSide * 0.82f
        val laserY = boxTop + (boxSide - laserTravel) / 2f + laserTravel * laserPhase
        val laserHeight = with(density) { 48.dp.toPx() }
        drawRect(
            brush =
                Brush.verticalGradient(
                    colors = listOf(LaserTop, LaserMid, LaserTop),
                    startY = laserY - laserHeight / 2f,
                    endY = laserY + laserHeight / 2f,
                ),
            topLeft = Offset(boxLeft + bracketStrokePx, laserY - laserHeight / 2f),
            size = Size(boxSide - bracketStrokePx * 2f, laserHeight),
        )

        val stroke = Stroke(width = bracketStrokePx)
        val half = bracketStrokePx / 2f
        fun corner(path: Path.() -> Unit) =
            drawPath(path = Path().apply(path), color = BracketBlue, style = stroke)

        corner {
            moveTo(boxLeft + half, boxTop + bracketLenPx)
            lineTo(boxLeft + half, boxTop + half)
            lineTo(boxLeft + bracketLenPx, boxTop + half)
        }
        corner {
            moveTo(boxRight - bracketLenPx, boxTop + half)
            lineTo(boxRight - half, boxTop + half)
            lineTo(boxRight - half, boxTop + bracketLenPx)
        }
        corner {
            moveTo(boxLeft + half, boxBottom - bracketLenPx)
            lineTo(boxLeft + half, boxBottom - half)
            lineTo(boxLeft + bracketLenPx, boxBottom - half)
        }
        corner {
            moveTo(boxRight - bracketLenPx, boxBottom - half)
            lineTo(boxRight - half, boxBottom - half)
            lineTo(boxRight - half, boxBottom - bracketLenPx)
        }
    }
}
