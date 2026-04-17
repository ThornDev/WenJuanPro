package ai.wenjuanpro.app.ui.screens.welcome

import ai.wenjuanpro.app.R
import ai.wenjuanpro.app.feature.welcome.WelcomeConfirmEffect
import ai.wenjuanpro.app.feature.welcome.WelcomeConfirmIntent
import ai.wenjuanpro.app.feature.welcome.WelcomeConfirmUiState
import ai.wenjuanpro.app.feature.welcome.WelcomeConfirmViewModel
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

object WelcomeScreenTags {
    const val ROOT = "welcome_screen_root"
    const val LOADING = "welcome_screen_loading"
    const val READY_CONTENT = "welcome_screen_ready"
    const val GREETING = "welcome_screen_greeting"
    const val TITLE = "welcome_screen_title"
    const val QUESTION_COUNT = "welcome_screen_question_count"
    const val ETA = "welcome_screen_eta"
    const val START_BUTTON = "welcome_screen_start_button"
    const val SSAID_ERROR = "welcome_screen_ssaid_error"
    const val SSAID_RETRY_BUTTON = "welcome_screen_ssaid_retry"
    const val CONFIG_MISSING = "welcome_screen_config_missing"
}

@Composable
fun WelcomeConfirmScreen(
    onNavigateToFirstQuestion: (studentId: String, configId: String) -> Unit,
    onNavigateToResume: (studentId: String, configId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WelcomeConfirmViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is WelcomeConfirmEffect.NavigateToFirstQuestion ->
                    onNavigateToFirstQuestion(effect.studentId, effect.configId)
                is WelcomeConfirmEffect.NavigateToResume ->
                    onNavigateToResume(effect.studentId, effect.configId)
            }
        }
    }

    WelcomeContent(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

@Composable
fun WelcomeContent(
    state: WelcomeConfirmUiState,
    onIntent: (WelcomeConfirmIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.testTag(WelcomeScreenTags.ROOT)) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            when (state) {
                WelcomeConfirmUiState.Loading -> LoadingBody()
                is WelcomeConfirmUiState.Ready -> ReadyBody(state = state, onIntent = onIntent)
                is WelcomeConfirmUiState.SsaidUnavailable -> SsaidErrorBody(onIntent = onIntent)
                is WelcomeConfirmUiState.ConfigMissing -> ConfigMissingBody()
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
                .testTag(WelcomeScreenTags.LOADING),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ReadyBody(
    state: WelcomeConfirmUiState.Ready,
    onIntent: (WelcomeConfirmIntent) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp)
                .testTag(WelcomeScreenTags.READY_CONTENT),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.welcome_greeting, state.studentId),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.testTag(WelcomeScreenTags.GREETING),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.welcome_title_label, state.title),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag(WelcomeScreenTags.TITLE),
            )
            Text(
                text = stringResource(R.string.welcome_question_count, state.questionCount),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.testTag(WelcomeScreenTags.QUESTION_COUNT),
            )
            Text(
                text = stringResource(R.string.welcome_eta_minutes, state.etaMinutes),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.testTag(WelcomeScreenTags.ETA),
            )
        }
        Button(
            onClick = { onIntent(WelcomeConfirmIntent.OnStartClicked) },
            enabled = !state.starting,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .testTag(WelcomeScreenTags.START_BUTTON),
        ) {
            Text(stringResource(R.string.welcome_start_button))
        }
    }
}

@Composable
private fun SsaidErrorBody(onIntent: (WelcomeConfirmIntent) -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp)
                .testTag(WelcomeScreenTags.SSAID_ERROR),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.welcome_ssaid_error_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { onIntent(WelcomeConfirmIntent.OnRetrySsaid) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag(WelcomeScreenTags.SSAID_RETRY_BUTTON),
        ) {
            Text(stringResource(R.string.welcome_ssaid_retry))
        }
    }
}

@Composable
private fun ConfigMissingBody() {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp)
                .testTag(WelcomeScreenTags.CONFIG_MISSING),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.welcome_config_missing_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
    }
}
