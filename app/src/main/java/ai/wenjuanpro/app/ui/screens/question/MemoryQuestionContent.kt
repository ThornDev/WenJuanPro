package ai.wenjuanpro.app.ui.screens.question

import ai.wenjuanpro.app.R
import ai.wenjuanpro.app.feature.question.QuestionUiState
import ai.wenjuanpro.app.ui.components.CountdownBar
import ai.wenjuanpro.app.ui.components.DotGrid
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

object MemoryQuestionContentTags {
    const val ROOT = "memory_question_content"
}

@Composable
fun MemoryQuestionContent(
    state: QuestionUiState.Memory,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .testTag(MemoryQuestionContentTags.ROOT),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CountdownBar(
            progress = state.countdownProgress,
            isWarning = state.isWarning,
            isPaused = true,
            overlayText = stringResource(R.string.question_memory_phase_hint),
        )

        Spacer(modifier = Modifier.height(24.dp))

        DotGrid(
            dotStates = state.dotStates,
            modifier = Modifier.fillMaxWidth(0.9f),
        )
    }
}
