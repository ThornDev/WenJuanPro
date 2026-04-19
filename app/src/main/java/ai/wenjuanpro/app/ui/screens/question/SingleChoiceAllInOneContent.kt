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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

object SingleChoiceAllInOneTags {
    const val ROOT = "single_choice_all_in_one_root"
    const val PROGRESS_LABEL = "single_choice_progress_label"
    const val SUBMIT_BUTTON = "single_choice_submit_button"
}

@Composable
fun SingleChoiceAllInOneContent(
    state: QuestionUiState.SingleChoiceAllInOne,
    onIntent: (QuestionIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .testTag(SingleChoiceAllInOneTags.ROOT),
    ) {
        Spacer(Modifier.height(24.dp))
        CountdownBar(
            progress = state.countdownProgress,
            isWarning = state.isWarning,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text =
                stringResource(
                    R.string.question_progress_counter_format,
                    state.questionIndex,
                    state.totalQuestions,
                ),
            style = MaterialTheme.typography.bodyMedium,
            modifier =
                Modifier
                    .padding(horizontal = 16.dp)
                    .testTag(SingleChoiceAllInOneTags.PROGRESS_LABEL),
        )
        Spacer(Modifier.height(16.dp))
        StemBlock(
            stem = state.stem,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(16.dp))
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
        ) {
            OptionsGrid(
                options = state.options,
                selectedIndex = state.selectedIndex,
                onOptionClick = { onIntent(QuestionIntent.SelectOption(it)) },
                optionsPerRow = state.optionsPerRow,
            )
        }
        if (state.showSubmitButton) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = { onIntent(QuestionIntent.Submit) },
                    enabled = state.submitEnabled,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .testTag(SingleChoiceAllInOneTags.SUBMIT_BUTTON),
                ) {
                    Text(stringResource(R.string.question_submit_button))
                }
            }
        }
    }
}
