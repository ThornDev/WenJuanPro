package ai.wenjuanpro.app.ui.screens.question

import ai.wenjuanpro.app.R
import ai.wenjuanpro.app.feature.question.QuestionIntent
import ai.wenjuanpro.app.feature.question.QuestionUiState
import ai.wenjuanpro.app.ui.components.CountdownBar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

object IntroContentTags {
    const val ROOT = "intro_content_root"
    const val STEM_AREA = "intro_content_stem_area"
    const val NEXT_BUTTON = "intro_content_next_button"
}

@Composable
fun IntroContent(
    state: QuestionUiState.IntroDisplay,
    onIntent: (QuestionIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .testTag(IntroContentTags.ROOT),
    ) {
        Spacer(Modifier.height(24.dp))
        CountdownBar(
            progress = state.countdownProgress,
            isWarning = state.isWarning,
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag(IntroContentTags.STEM_AREA),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 24.dp),
            ) {
                StemBlock(stem = state.stem)
            }
        }
        if (state.showNextButton) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = { onIntent(QuestionIntent.SkipIntro) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .testTag(IntroContentTags.NEXT_BUTTON),
                ) {
                    Text(stringResource(R.string.question_intro_next))
                }
            }
        }
    }
}
