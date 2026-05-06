package ai.wenjuanpro.app.ui.screens.question

import ai.wenjuanpro.app.R
import ai.wenjuanpro.app.feature.question.QuestionIntent
import ai.wenjuanpro.app.feature.question.QuestionUiState
import ai.wenjuanpro.app.ui.components.CountdownBar
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

object StagedFillBlankTags {
    const val ROOT = "staged_fill_blank_root"
    const val STEM_BLOCK = "staged_fill_blank_stem_block"
    const val OPTIONS_BLOCK = "staged_fill_blank_options_block"
    const val PHASE_LABEL = "staged_fill_blank_phase_label"
    const val INPUT_FIELD = "staged_fill_blank_input_field"
    const val SUBMIT_BUTTON = "staged_fill_blank_submit_button"
}

@Composable
fun StagedFillBlankScaffold(
    state: QuestionUiState.FillBlankStaged,
    onIntent: (QuestionIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .testTag(StagedFillBlankTags.ROOT),
    ) {
        Spacer(Modifier.height(24.dp))
        CountdownBar(
            progress = state.countdownProgress,
            isWarning = state.isWarning,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text =
                when (state.stage) {
                    QuestionUiState.Stage.STEM ->
                        stringResource(R.string.question_progress_counter_format, state.questionIndex, state.totalQuestions) +
                            " · " + stringResource(R.string.question_stem_phase_hint)
                    QuestionUiState.Stage.OPTIONS ->
                        stringResource(R.string.question_progress_counter_format, state.questionIndex, state.totalQuestions) +
                            " · " + stringResource(R.string.question_options_phase_hint)
                },
            style = MaterialTheme.typography.bodyMedium,
            modifier =
                Modifier
                    .padding(horizontal = 16.dp)
                    .testTag(StagedFillBlankTags.PHASE_LABEL),
        )
        Spacer(Modifier.height(16.dp))
        AnimatedContent(
            targetState = state.stage,
            transitionSpec = {
                fadeIn(animationSpec = tween(durationMillis = 200)) togetherWith
                    fadeOut(animationSpec = tween(durationMillis = 200))
            },
            label = "staged_fill_blank_content",
            modifier = Modifier.weight(1f),
        ) { stage ->
            when (stage) {
                QuestionUiState.Stage.STEM ->
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .testTag(StagedFillBlankTags.STEM_BLOCK)
                                .pointerInput(Unit) {
                                    detectTapGestures { /* swallow taps in stem stage */ }
                                },
                    ) {
                        StemBlock(
                            stem = state.stem,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                QuestionUiState.Stage.OPTIONS ->
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .testTag(StagedFillBlankTags.OPTIONS_BLOCK),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                        ) {
                            StemBlock(stem = state.stem)
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
                                        .testTag(StagedFillBlankTags.INPUT_FIELD),
                            )
                        }
                        if (state.showSubmitButton) {
                            Button(
                                onClick = { onIntent(QuestionIntent.Submit) },
                                enabled = state.submitEnabled,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 56.dp)
                                        .padding(16.dp)
                                        .testTag(StagedFillBlankTags.SUBMIT_BUTTON),
                            ) {
                                Text(stringResource(R.string.question_submit_button))
                            }
                        }
                    }
            }
        }
    }
}
