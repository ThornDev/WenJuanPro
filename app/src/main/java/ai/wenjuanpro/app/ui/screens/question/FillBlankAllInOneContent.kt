package ai.wenjuanpro.app.ui.screens.question

import ai.wenjuanpro.app.R
import ai.wenjuanpro.app.feature.question.QuestionIntent
import ai.wenjuanpro.app.feature.question.QuestionUiState
import ai.wenjuanpro.app.ui.components.CountdownBar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

object FillBlankAllInOneTags {
    const val ROOT = "fill_blank_all_in_one_root"
    const val PROGRESS_LABEL = "fill_blank_progress_label"
    const val INPUT_FIELD = "fill_blank_input_field"
    const val SUBMIT_BUTTON = "fill_blank_submit_button"
}

@Composable
fun FillBlankAllInOneContent(
    state: QuestionUiState.FillBlankAllInOne,
    onIntent: (QuestionIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember(state.qid) { FocusRequester() }
    LaunchedEffect(state.qid) {
        runCatching { focusRequester.requestFocus() }
    }
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .testTag(FillBlankAllInOneTags.ROOT),
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
                    .testTag(FillBlankAllInOneTags.PROGRESS_LABEL),
        )
        Spacer(Modifier.height(16.dp))
        StemBlock(
            stem = state.stem,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = state.answer,
            onValueChange = { onIntent(QuestionIntent.UpdateFillBlank(it)) },
            label = { Text(stringResource(R.string.question_fill_label)) },
            placeholder = { Text(stringResource(R.string.question_fill_placeholder)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions =
                KeyboardActions(onDone = {
                    if (state.submitEnabled) onIntent(QuestionIntent.Submit)
                }),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .focusRequester(focusRequester)
                    .testTag(FillBlankAllInOneTags.INPUT_FIELD),
        )
        Spacer(Modifier.weight(1f))
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
                            .testTag(FillBlankAllInOneTags.SUBMIT_BUTTON),
                ) {
                    Text(stringResource(R.string.question_submit_button))
                }
            }
        }
    }
}
