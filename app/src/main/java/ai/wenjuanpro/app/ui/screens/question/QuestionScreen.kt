package ai.wenjuanpro.app.ui.screens.question

import ai.wenjuanpro.app.R
import ai.wenjuanpro.app.feature.question.QuestionEffect
import ai.wenjuanpro.app.feature.question.QuestionIntent
import ai.wenjuanpro.app.feature.question.QuestionUiState
import ai.wenjuanpro.app.feature.question.QuestionViewModel
import ai.wenjuanpro.app.ui.components.ContentConstraint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

object QuestionScreenTags {
    const val ROOT = "question_screen_root"
    const val LOADING = "question_screen_loading"
    const val ERROR = "question_screen_error"
    const val RETRY_BANNER = "question_screen_retry_banner"
    const val RETRY_BUTTON = "question_screen_retry_button"
    const val ERROR_MESSAGE = "question_screen_error_message"
}

@Composable
fun QuestionScreen(
    onNavigateNext: (studentId: String, configId: String) -> Unit,
    onNavigateComplete: () -> Unit,
    onNavigateTerminal: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuestionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = true) { /* swallow back press during answering */ }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is QuestionEffect.NavigateNext ->
                    onNavigateNext(effect.studentId, effect.configId)
                QuestionEffect.NavigateComplete -> onNavigateComplete()
                QuestionEffect.NavigateTerminal -> onNavigateTerminal()
                is QuestionEffect.ShowRetryBanner -> Unit
            }
        }
    }

    QuestionContent(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

@Composable
fun QuestionContent(
    state: QuestionUiState,
    onIntent: (QuestionIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.testTag(QuestionScreenTags.ROOT)) { innerPadding ->
        ContentConstraint(modifier = Modifier.padding(innerPadding)) {
            when (state) {
                QuestionUiState.Loading -> LoadingBody()
                is QuestionUiState.SingleChoiceAllInOne ->
                    SingleChoiceAllInOneContent(state = state, onIntent = onIntent)
                is QuestionUiState.SingleChoiceStaged ->
                    StagedSingleChoiceScaffold(state = state, onIntent = onIntent)
                is QuestionUiState.MultiChoiceAllInOne ->
                    MultiChoiceAllInOneContent(state = state, onIntent = onIntent)
                is QuestionUiState.MultiChoiceStaged ->
                    StagedMultiChoiceScaffold(state = state, onIntent = onIntent)
                is QuestionUiState.Memory ->
                    MemoryQuestionContent(state = state, onIntent = onIntent)
                is QuestionUiState.FillBlankAllInOne ->
                    FillBlankAllInOneContent(state = state, onIntent = onIntent)
                is QuestionUiState.FillBlankStaged ->
                    StagedFillBlankScaffold(state = state, onIntent = onIntent)
                is QuestionUiState.IntroDisplay ->
                    IntroContent(state = state, onIntent = onIntent)
                is QuestionUiState.RetryWriteBanner ->
                    RetryBannerBody(state = state, onIntent = onIntent)
                is QuestionUiState.Error -> ErrorBody(state.message)
            }
        }
    }
}

@Composable
private fun LoadingBody() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .testTag(QuestionScreenTags.LOADING),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun RetryBannerBody(
    state: QuestionUiState.RetryWriteBanner,
    onIntent: (QuestionIntent) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp)
                .testTag(QuestionScreenTags.RETRY_BANNER),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.question_error_result_write_failed),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.question_progress_counter_format, state.retriesLeft, 3),
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { onIntent(QuestionIntent.Retry) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag(QuestionScreenTags.RETRY_BUTTON),
        ) {
            Text(stringResource(R.string.question_retry_button))
        }
    }
}

@Composable
private fun ErrorBody(message: String) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp)
                .testTag(QuestionScreenTags.ERROR),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(QuestionScreenTags.ERROR_MESSAGE),
        )
    }
}
