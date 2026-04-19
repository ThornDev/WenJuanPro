package ai.wenjuanpro.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

object CountdownBarTags {
    const val ROOT = "countdown_bar_root"
    const val FILL = "countdown_bar_fill"
    const val OVERLAY_TEXT = "countdown_bar_overlay"
    const val SEMANTIC_NORMAL = "countdown_bar_color_normal"
    const val SEMANTIC_WARNING = "countdown_bar_color_warning"
    const val SEMANTIC_PAUSED = "countdown_bar_color_paused"
}

private val PrimaryBlueStart = Color(0xFF42A5F5)
private val PrimaryBlueEnd = Color(0xFF1565C0)
private val WarningOrangeStart = Color(0xFFFFB74D)
private val WarningOrangeEnd = Color(0xFFE64A19)
private val PausedGrayStart = Color(0xFFCFCFCF)
private val PausedGrayEnd = Color(0xFF9E9E9E)
private val TrackGray = Color(0xFFECEFF1)

@Composable
fun CountdownBar(
    progress: Float,
    isWarning: Boolean,
    isPaused: Boolean = false,
    overlayText: String? = null,
    modifier: Modifier = Modifier,
) {
    val targetStart =
        when {
            isPaused -> PausedGrayStart
            isWarning -> WarningOrangeStart
            else -> PrimaryBlueStart
        }
    val targetEnd =
        when {
            isPaused -> PausedGrayEnd
            isWarning -> WarningOrangeEnd
            else -> PrimaryBlueEnd
        }
    val animatedStart by animateColorAsState(
        targetValue = targetStart,
        animationSpec = tween(durationMillis = 250, easing = LinearEasing),
        label = "countdown_color_start",
    )
    val animatedEnd by animateColorAsState(
        targetValue = targetEnd,
        animationSpec = tween(durationMillis = 250, easing = LinearEasing),
        label = "countdown_color_end",
    )
    val clamped = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clamped,
        animationSpec = tween(durationMillis = 120, easing = LinearEasing),
        label = "countdown_progress",
    )
    val semanticTag =
        when {
            isPaused -> CountdownBarTags.SEMANTIC_PAUSED
            isWarning -> CountdownBarTags.SEMANTIC_WARNING
            else -> CountdownBarTags.SEMANTIC_NORMAL
        }
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(shape)
                .background(TrackGray)
                .testTag(CountdownBarTags.ROOT)
                .semantics { contentDescription = semanticTag },
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = animatedProgress)
                    .clip(shape)
                    .background(Brush.horizontalGradient(listOf(animatedStart, animatedEnd)))
                    .alpha(if (isPaused) 0.5f else 1f)
                    .testTag(CountdownBarTags.FILL),
        )
        overlayText?.takeIf { it.isNotBlank() }?.let { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Black,
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .testTag(CountdownBarTags.OVERLAY_TEXT),
            )
        }
    }
}
