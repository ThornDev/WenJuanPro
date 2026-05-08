package ai.wenjuanpro.app.ui.screens.complete

import ai.wenjuanpro.app.R
import ai.wenjuanpro.app.feature.complete.CompleteUploadState
import ai.wenjuanpro.app.feature.complete.CompleteViewModel
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay

object CompleteScreenTags {
    const val ROOT = "complete_screen_root"
    const val RETURN_BUTTON = "complete_return_button"
    const val COUNTDOWN_TEXT = "complete_countdown_text"
    const val UPLOAD_STATUS = "complete_upload_status"
}

private const val AUTO_RETURN_SECONDS = 60
private val HeroStart = Color(0xFF1976D2)
private val HeroEnd = Color(0xFF42A5F5)

@Composable
fun CompleteScreen(
    onReturn: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CompleteViewModel = hiltViewModel(),
) {
    val uploadState by viewModel.uploadState.collectAsStateWithLifecycle()
    var remaining by remember { mutableIntStateOf(AUTO_RETURN_SECONDS) }

    BackHandler(enabled = true) { onReturn() }

    LaunchedEffect(Unit) {
        while (remaining > 0) {
            delay(1000L)
            remaining -= 1
        }
        onReturn()
    }

    val progress by animateFloatAsState(
        targetValue = remaining.toFloat() / AUTO_RETURN_SECONDS.toFloat(),
        animationSpec = tween(durationMillis = 900, easing = LinearEasing),
        label = "complete_progress",
    )

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(HeroStart, HeroEnd)))
                .testTag(CompleteScreenTags.ROOT),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier =
                Modifier
                    .fillMaxHeight()
                    .widthIn(max = 520.dp)
                    .padding(horizontal = 32.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(128.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = HeroStart,
                        modifier = Modifier.size(56.dp),
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
            Text(
                text = stringResource(R.string.complete_title),
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.complete_thanks),
                color = Color.White.copy(alpha = 0.88f),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(40.dp))
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.22f)),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                            .background(Color.White),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.complete_countdown, remaining),
                color = Color.White.copy(alpha = 0.82f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag(CompleteScreenTags.COUNTDOWN_TEXT),
            )
            Spacer(Modifier.height(20.dp))
            UploadStatusPill(state = uploadState)
        }
    }
}

@Composable
private fun UploadStatusPill(state: CompleteUploadState) {
    val (text, dim, showSpinner) =
        when (state) {
            CompleteUploadState.Idle -> Triple(null, false, false)
            is CompleteUploadState.Uploading ->
                Triple(
                    stringResource(
                        R.string.complete_uploading,
                        state.attempt,
                        state.maxAttempts,
                    ),
                    false,
                    true,
                )
            CompleteUploadState.Success ->
                Triple(stringResource(R.string.complete_upload_success), false, false)
            is CompleteUploadState.Failed ->
                Triple(
                    if (state.reason.isNullOrBlank()) {
                        stringResource(R.string.complete_upload_failed, state.attempts)
                    } else {
                        stringResource(
                            R.string.complete_upload_failed_with_reason,
                            state.attempts,
                            state.reason,
                        )
                    },
                    true,
                    false,
                )
            CompleteUploadState.NoFile ->
                Triple(stringResource(R.string.complete_upload_no_file), true, false)
        }
    if (text == null) return
    Surface(
        color = Color.White.copy(alpha = if (dim) 0.10f else 0.18f),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.testTag(CompleteScreenTags.UPLOAD_STATUS),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showSpinner) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = Color.White.copy(alpha = if (dim) 0.7f else 0.95f),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
