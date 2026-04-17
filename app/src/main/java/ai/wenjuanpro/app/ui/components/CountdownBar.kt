package ai.wenjuanpro.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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

private val PrimaryBlue = Color(0xFF1976D2)
private val WarningOrange = Color(0xFFFB8C00)
private val PausedGray = Color(0xFFBDBDBD)
private val TrackGray = Color(0xFFEEEEEE)

@Composable
fun CountdownBar(
    progress: Float,
    isWarning: Boolean,
    isPaused: Boolean = false,
    overlayText: String? = null,
    modifier: Modifier = Modifier,
) {
    val target =
        when {
            isPaused -> PausedGray
            isWarning -> WarningOrange
            else -> PrimaryBlue
        }
    val animated by animateColorAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 200, easing = LinearEasing),
        label = "countdown_color",
    )
    val semanticTag =
        when {
            isPaused -> CountdownBarTags.SEMANTIC_PAUSED
            isWarning -> CountdownBarTags.SEMANTIC_WARNING
            else -> CountdownBarTags.SEMANTIC_NORMAL
        }
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(TrackGray)
                .testTag(CountdownBarTags.ROOT)
                .semantics { contentDescription = semanticTag },
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = clamped)
                    .background(animated)
                    .alpha(if (isPaused) 0.4f else 1f)
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
