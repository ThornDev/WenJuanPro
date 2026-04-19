package ai.wenjuanpro.app.ui.screens.question

import ai.wenjuanpro.app.R
import ai.wenjuanpro.app.feature.question.MemoryPhase
import ai.wenjuanpro.app.feature.question.QuestionIntent
import ai.wenjuanpro.app.feature.question.QuestionUiState
import ai.wenjuanpro.app.ui.components.CountdownBar
import ai.wenjuanpro.app.ui.components.DotGrid
import ai.wenjuanpro.app.domain.usecase.HitTestUtil
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

object MemoryQuestionContentTags {
    const val ROOT = "memory_question_content"
}

@Composable
fun MemoryQuestionContent(
    state: QuestionUiState.Memory,
    onIntent: (QuestionIntent) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val isRecalling = state.phase is MemoryPhase.Recalling

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .testTag(MemoryQuestionContentTags.ROOT),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        CountdownBar(
            progress = state.countdownProgress,
            isWarning = state.isWarning,
            isPaused = !isRecalling,
            overlayText = if (isRecalling) {
                stringResource(R.string.question_options_phase_hint)
            } else {
                stringResource(R.string.question_memory_phase_hint)
            },
        )

        Spacer(modifier = Modifier.height(24.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 480.dp),
        ) {
            val gridWidthDp = maxWidth
            val cellSizePx = with(LocalDensity.current) { (gridWidthDp / 8).toPx() }

            DotGrid(
                dotStates = state.dotStates,
                selectedSequence = state.selectedSequence,
                onTap = if (isRecalling) { x, y ->
                    val hitIndex = HitTestUtil.hitTest(
                        touchX = x,
                        touchY = y,
                        dotsPositions = state.dotsPositions,
                        cellSizePx = cellSizePx,
                        selectedIndices = state.selectedSequence.toSet(),
                    )
                    if (hitIndex != null) {
                        onIntent(QuestionIntent.RecallTap(hitIndex))
                    }
                } else {
                    null
                },
            )
        }
    }
}
